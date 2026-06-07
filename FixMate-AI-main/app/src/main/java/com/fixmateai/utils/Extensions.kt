package com.fixmateai.utils

import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.fixmateai.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Small reusable extension functions to keep UI code concise. */

fun androidx.appcompat.app.AppCompatActivity.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
}

fun Fragment.toast(message: String) {
    Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
}

fun View.visible() { visibility = View.VISIBLE }
fun View.gone() { visibility = View.GONE }
fun View.show(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}

/** Load a remote or local image into an ImageView with a placeholder. */
fun ImageView.loadImage(url: String?) {
    Glide.with(this.context)
        .load(url)
        .placeholder(R.drawable.ic_image_placeholder)
        .error(R.drawable.ic_image_placeholder)
        .centerCrop()
        .into(this)
}

/** Format an epoch-millis timestamp for display. */
fun Long.toReadableDate(): String {
    val formatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return formatter.format(Date(this))
}

/** Basic email validation. */
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()
