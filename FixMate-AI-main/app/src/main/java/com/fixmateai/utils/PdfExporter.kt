package com.fixmateai.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import java.io.File

/**
 * Renders a simple, shareable PDF (no third-party libraries) for a saved report.
 * Used by the History detail screen's "Export PDF" action.
 */
object PdfExporter {

    private const val PAGE_W = 595 // A4 @ 72dpi
    private const val PAGE_H = 842
    private const val MARGIN = 40f

    /** Builds the PDF from titled sections and launches a share sheet. */
    fun exportAndShare(context: Context, title: String, sections: List<Pair<String, String>>) {
        val doc = PdfDocument()
        val page = doc.startPage(PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, 1).create())
        val canvas = page.canvas
        var y = MARGIN + 20f

        val titlePaint = Paint().apply {
            color = Color.parseColor("#1565C0"); textSize = 22f; isFakeBoldText = true
        }
        val headPaint = Paint().apply {
            color = Color.parseColor("#1A1C1E"); textSize = 13f; isFakeBoldText = true
        }
        val bodyPaint = Paint().apply { color = Color.parseColor("#444444"); textSize = 12f }

        canvas.drawText("FixMate AI — $title", MARGIN, y, titlePaint)
        y += 30f

        sections.forEach { (head, body) ->
            if (y > PAGE_H - MARGIN) return@forEach
            canvas.drawText(head, MARGIN, y, headPaint)
            y += 18f
            body.split("\n").forEach { line ->
                wrap(line, bodyPaint).forEach { wrapped ->
                    if (y <= PAGE_H - MARGIN) {
                        canvas.drawText(wrapped, MARGIN, y, bodyPaint)
                        y += 16f
                    }
                }
            }
            y += 10f
        }

        doc.finishPage(page)

        val dir = File(context.cacheDir, "pdfs").apply { mkdirs() }
        val file = File(dir, "FixMate_Report_${System.currentTimeMillis()}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()

        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val share = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(share, "Share report PDF"))
    }

    /** Naive word-wrap to the page width. */
    private fun wrap(text: String, paint: Paint): List<String> {
        if (text.isBlank()) return listOf("")
        val maxW = PAGE_W - 2 * MARGIN
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var line = StringBuilder()
        words.forEach { w ->
            val test = if (line.isEmpty()) w else "$line $w"
            if (paint.measureText(test) > maxW) {
                if (line.isNotEmpty()) lines.add(line.toString())
                line = StringBuilder(w)
            } else line = StringBuilder(test)
        }
        if (line.isNotEmpty()) lines.add(line.toString())
        return lines
    }
}
