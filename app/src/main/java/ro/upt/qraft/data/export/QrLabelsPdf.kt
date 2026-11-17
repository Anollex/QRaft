package ro.upt.qraft.data.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.TextPaint
import android.text.TextUtils
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.OutputStream

/** O eticheta de printat: codul QR + textul de sub el. */
data class QrLabel(
    val qrContent: String,
    val code: String,
    val title: String,
    val subtitle: String
)

/**
 * Pagini A4 cu etichete QR intr-o grila de 3 x 7, cu linii punctate de taiere.
 * Codurile QR sunt desenate vectorial (dreptunghiuri), deci raman clare la orice rezolutie de printare.
 */
object QrLabelsPdf {

    // Unitatea PdfDocument este punctul tipografic (1/72 inch). A4 = 595 x 842.
    private const val PAGE_WIDTH = 595
    private const val PAGE_HEIGHT = 842
    private const val MARGIN = 28f
    private const val COLUMNS = 3
    private const val ROWS = 7
    private const val QR_SIZE = 66f
    private const val PADDING = 6f

    const val LABELS_PER_PAGE = COLUMNS * ROWS

    fun write(out: OutputStream, header: String, labels: List<QrLabel>) {
        val document = PdfDocument()
        try {
            val pages = labels.chunked(LABELS_PER_PAGE).ifEmpty { listOf(emptyList()) }
            pages.forEachIndexed { index, pageLabels ->
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, index + 1).create()
                val page = document.startPage(pageInfo)
                drawPage(page.canvas, "$header · pagina ${index + 1}/${pages.size}", pageLabels)
                document.finishPage(page)
            }
            document.writeTo(out)
        } finally {
            document.close()
        }
    }

    private val headerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8f
        color = Color.GRAY
    }
    private val codePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 8f
        typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
    }
    private val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val subtitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 7f
        color = Color.DKGRAY
        textAlign = Paint.Align.CENTER
    }
    private val cutPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 0.5f
        color = Color.LTGRAY
        pathEffect = DashPathEffect(floatArrayOf(3f, 3f), 0f)
    }
    private val qrPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.BLACK
    }

    private fun drawPage(canvas: Canvas, header: String, labels: List<QrLabel>) {
        canvas.drawText(header, MARGIN, MARGIN - 10f, headerPaint)

        val labelWidth = (PAGE_WIDTH - 2 * MARGIN) / COLUMNS
        val labelHeight = (PAGE_HEIGHT - 2 * MARGIN) / ROWS

        labels.forEachIndexed { i, label ->
            val left = MARGIN + (i % COLUMNS) * labelWidth
            val top = MARGIN + (i / COLUMNS) * labelHeight
            canvas.drawRect(left, top, left + labelWidth, top + labelHeight, cutPaint)

            val centerX = left + labelWidth / 2
            drawQr(canvas, label.qrContent, centerX - QR_SIZE / 2, top + PADDING, QR_SIZE)

            val textWidth = labelWidth - 2 * PADDING
            var baseline = top + PADDING + QR_SIZE + 11f
            canvas.drawText(label.code, centerX, baseline, codePaint)
            baseline += 12f
            canvas.drawText(ellipsize(label.title, titlePaint, textWidth), centerX, baseline, titlePaint)
            baseline += 10f
            canvas.drawText(ellipsize(label.subtitle, subtitlePaint, textWidth), centerX, baseline, subtitlePaint)
        }
    }

    private fun ellipsize(text: String, paint: TextPaint, width: Float): String =
        TextUtils.ellipsize(text, paint, width, TextUtils.TruncateAt.END).toString()

    /** Deseneaza codul QR modul cu modul, unind modulele negre consecutive de pe fiecare rand. */
    private fun drawQr(canvas: Canvas, content: String, left: Float, top: Float, size: Float) {
        val hints = mapOf(
            EncodeHintType.MARGIN to 0, // spatiul alb e asigurat de padding-ul etichetei
            EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M
        )
        // Dimensiunea 0 intoarce matricea bruta: un pixel per modul.
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0, hints)
        val modules = matrix.width
        val cell = size / modules

        for (y in 0 until modules) {
            var x = 0
            while (x < modules) {
                if (!matrix[x, y]) { x++; continue }
                val start = x
                while (x < modules && matrix[x, y]) x++
                canvas.drawRect(
                    left + start * cell, top + y * cell,
                    left + x * cell, top + (y + 1) * cell,
                    qrPaint
                )
            }
        }
    }
}
