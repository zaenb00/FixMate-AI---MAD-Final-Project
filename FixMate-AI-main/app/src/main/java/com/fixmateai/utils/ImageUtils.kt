package com.fixmateai.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import android.graphics.Matrix
import java.io.ByteArrayOutputStream

/**
 * Image helpers: load + compress a picked/captured image and convert it to
 * Base64 so it can be embedded in the Gemini JSON request, and produce a
 * compressed [ByteArray] for uploading to Firebase Storage.
 *
 * Compressing before upload keeps storage usage and upload time low, and keeps
 * the Gemini request within size limits.
 */
object ImageUtils {

    private const val MAX_DIMENSION = 1024      // longest edge in px after scaling
    private const val JPEG_QUALITY = 80         // 0..100

    /**
     * Decodes the image at [uri], scales it down to at most [MAX_DIMENSION] on its
     * longest edge, fixes orientation using EXIF, and returns a compressed JPEG.
     * Returns null if the image cannot be decoded (e.g. invalid/corrupt file).
     */
    fun compressImage(context: Context, uri: Uri): ByteArray? {
        return try {
            val original = decodeSampledBitmap(context, uri) ?: return null
            val rotated = applyExifRotation(context, uri, original)

            val outputStream = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
            rotated.recycle()
            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    /** Convenience: compressed image as a Base64 string for the Gemini request. */
    fun toBase64(imageBytes: ByteArray): String =
        Base64.encodeToString(imageBytes, Base64.NO_WRAP)

    // --- internals ---

    private fun decodeSampledBitmap(context: Context, uri: Uri): Bitmap? {
        // First pass: read only bounds to compute a sensible sample size.
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, boundsOptions)
        }

        val sampleSize = calculateInSampleSize(boundsOptions, MAX_DIMENSION, MAX_DIMENSION)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }

        val decoded = context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        } ?: return null

        return scaleToMaxDimension(decoded)
    }

    private fun scaleToMaxDimension(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= MAX_DIMENSION && height <= MAX_DIMENSION) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val (newW, newH) = if (width >= height) {
            MAX_DIMENSION to (MAX_DIMENSION / ratio).toInt()
        } else {
            (MAX_DIMENSION * ratio).toInt() to MAX_DIMENSION
        }
        val scaled = Bitmap.createScaledBitmap(bitmap, newW, newH, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val (height, width) = options.outHeight to options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight &&
                halfWidth / inSampleSize >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun applyExifRotation(context: Context, uri: Uri, bitmap: Bitmap): Bitmap {
        return try {
            val orientation = context.contentResolver.openInputStream(uri)?.use { input ->
                ExifInterface(input).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            } ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap
            }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap) bitmap.recycle()
            rotated
        } catch (e: Exception) {
            bitmap
        }
    }
}
