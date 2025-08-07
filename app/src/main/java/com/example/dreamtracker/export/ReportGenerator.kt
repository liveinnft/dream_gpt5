package com.example.dreamtracker.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.pdf.PdfDocument
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {
    private val dao = AppDatabase.get(context).dreamDao()

    suspend fun generate(periodDays: Int = 7): Pair<File, File> {
        val now = System.currentTimeMillis()
        val start = now - periodDays * 24L * 60L * 60L * 1000L
        val dreams = dao.getBetween(start, now)
        val summary = buildSummary(dreams)
        val (bmp, width, height) = drawReportBitmap(dreams, summary, periodDays)
        val pdf = savePdf(bmp, width, height)
        val png = savePng(bmp)
        return pdf to png
    }

    data class Summary(
        val count: Int,
        val avgMood: Double,
        val toneTop: List<Pair<String, Int>>,
        val symbolsTop: List<Pair<String, Int>>
    )

    private fun buildSummary(dreams: List<Dream>): Summary {
        val count = dreams.size
        val avgMood = if (count == 0) 0.0 else dreams.map { it.moodScore }.average()
        val tones = dreams.map { it.tone.ifBlank { "не указан" } }.groupingBy { it }.eachCount()
            .entries.sortedByDescending { it.value }.take(5).map { it.key to it.value }
        val symCounts = mutableMapOf<String, Int>()
        dreams.forEach { d -> d.symbolsMatched.split(',').map { it.trim() }.filter { it.isNotBlank() }.forEach { s -> symCounts[s] = (symCounts[s] ?: 0) + 1 } }
        val topSymbols = symCounts.entries.sortedByDescending { it.value }.take(8).map { it.key to it.value }
        return Summary(count, avgMood, tones, topSymbols)
    }

    private fun drawReportBitmap(dreams: List<Dream>, summary: Summary, periodDays: Int): Triple<Bitmap, Int, Int> {
        val width = 1240
        val height = 1754
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        // Pastel gradient background
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.rgb(238, 245, 252), Color.rgb(224, 235, 247)),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        // Card helper
        fun drawCard(rect: RectF, radius: Float = 28f) {
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 0, 0, 0) }
            canvas.drawRoundRect(RectF(rect.left, rect.top + 6, rect.right, rect.bottom + 6), radius, radius, shadow)
            val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
            canvas.drawRoundRect(rect, radius, radius, card)
        }

        // Header with logo and title
        val headerRect = RectF(60f, 60f, width - 60f, 240f)
        drawCard(headerRect)
        drawLogo(canvas, 90f, headerRect.centerY())
        val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 35, 55); textSize = 54f }
        val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 100, 130); textSize = 32f }
        val title = "DreamTracker"
        canvas.drawText(title, 170f, headerRect.top + 80f, pTitle)
        val periodLabel = if (periodDays <= 7) "Отчёт за неделю" else "Отчёт за месяц"
        canvas.drawText(periodLabel, 170f, headerRect.top + 130f, pSub)
        val dateLabel = dateRange(dreams)
        canvas.drawText(dateLabel, 170f, headerRect.top + 170f, pSub)

        // Metrics card
        val metricsRect = RectF(60f, headerRect.bottom + 20f, width - 60f, headerRect.bottom + 220f)
        drawCard(metricsRect)
        val pMetric = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 50, 75); textSize = 40f }
        val pMetricVal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(60, 90, 130); textSize = 40f }
        canvas.drawText("Всего снов:", metricsRect.left + 40f, metricsRect.top + 70f, pMetric)
        canvas.drawText("${summary.count}", metricsRect.left + 300f, metricsRect.top + 70f, pMetricVal)
        canvas.drawText("Среднее настроение:", metricsRect.left + 40f, metricsRect.top + 140f, pMetric)
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", summary.avgMood), metricsRect.left + 420f, metricsRect.top + 140f, pMetricVal)

        // Tones card with legend
        val tonesRect = RectF(60f, metricsRect.bottom + 20f, width - 60f, metricsRect.bottom + 360f)
        drawCard(tonesRect)
        val pSec = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(30, 50, 75); textSize = 36f }
        canvas.drawText("Тональность", tonesRect.left + 40f, tonesRect.top + 60f, pSec)
        val legendX = tonesRect.right - 300f
        var legendY = tonesRect.top + 40f
        val totalTones = summary.toneTop.sumOf { it.second }.coerceAtLeast(1)
        // Bars with subtle grid
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.argb(60, 160, 180, 200); strokeWidth = 1f }
        val chartLeft = tonesRect.left + 40f
        val chartTop = tonesRect.top + 80f
        val chartRight = tonesRect.right - 320f
        val chartBottom = tonesRect.bottom - 40f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        repeat(4) { i ->
            val y = chartTop + chartHeight * (i / 3f)
            canvas.drawLine(chartLeft, y, chartRight, y, grid)
        }
        var x = chartLeft
        summary.toneTop.forEach { (tone, count) ->
            val w = (count.toFloat() / totalTones) * chartWidth
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = nextColor(tone) }
            val r = RectF(x, chartTop + 10f, x + w, chartBottom - 10f)
            canvas.drawRoundRect(r, 16f, 16f, barPaint)
            // legend
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barPaint.color }
            canvas.drawCircle(legendX, legendY, 10f, dot)
            val pLegend = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(70, 90, 120); textSize = 28f }
            canvas.drawText("$tone ($count)", legendX + 20f, legendY + 10f, pLegend)
            legendY += 36f
            x += w + 14f
        }

        // Top symbols card with bars and grid
        val symbolsRect = RectF(60f, tonesRect.bottom + 20f, width - 60f, tonesRect.bottom + 560f)
        drawCard(symbolsRect)
        canvas.drawText("Частые символы", symbolsRect.left + 40f, symbolsRect.top + 60f, pSec)
        val sLeft = symbolsRect.left + 40f
        val sTop = symbolsRect.top + 90f
        val sRight = symbolsRect.right - 40f
        val sBottom = symbolsRect.bottom - 40f
        repeat(5) { i ->
            val y = sTop + (sBottom - sTop) * (i / 4f)
            canvas.drawLine(sLeft, y, sRight, y, grid)
        }
        val maxSym = summary.symbolsTop.maxOfOrNull { it.second } ?: 1
        var sy = sTop
        summary.symbolsTop.forEach { (s, c) ->
            val bw = ((sRight - sLeft - 220f) * (c.toFloat() / maxSym)).coerceAtLeast(4f)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 100, 130); textSize = 30f }
            canvas.drawText(s, sLeft, sy + 34f, label)
            val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(130, 160, 200) }
            val rr = RectF(sLeft + 180f, sy + 8f, sLeft + 180f + bw, sy + 38f)
            canvas.drawRoundRect(rr, 12f, 12f, bar)
            canvas.drawText(c.toString(), sLeft + 200f + bw, sy + 34f, label)
            sy += 54f
        }

        // Dreams list card
        val listRect = RectF(60f, symbolsRect.bottom + 20f, width - 60f, height - 120f)
        drawCard(listRect)
        canvas.drawText("Список снов", listRect.left + 40f, listRect.top + 60f, pSec)
        val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        val pItem = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(80, 100, 130); textSize = 28f }
        var ly = listRect.top + 90f
        dreams.take(16).forEach { d ->
            val line = "${fmt.format(Date(d.createdAtEpoch))} • ${d.title.ifBlank { d.transcriptText.take(32) }}"
            canvas.drawText(line, listRect.left + 40f, ly, pItem)
            ly += 34f
        }

        // Footer
        val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(120, 140, 160); textSize = 24f }
        canvas.drawText(
            "Сгенерировано DreamTracker • ${SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault()).format(Date())}",
            60f, height - 40f, foot
        )

        return Triple(bmp, width, height)
    }

    private fun dateRange(dreams: List<Dream>): String {
        if (dreams.isEmpty()) return "нет данных"
        val fmt = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        val start = dreams.first().createdAtEpoch
        val end = dreams.last().createdAtEpoch
        return "${fmt.format(Date(start))} — ${fmt.format(Date(end))}"
    }

    private fun nextColor(seed: String): Int {
        val base = seed.hashCode()
        val r = 100 + (base shr 16 and 0x7F)
        val g = 100 + (base shr 8 and 0x7F)
        val b = 120 + (base and 0x7F)
        return Color.rgb(r, g, b)
    }

    private fun drawLogo(canvas: Canvas, cx: Float, cy: Float) {
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(140, 170, 210) }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
        // Moon (crescent)
        canvas.drawCircle(cx, cy, 40f, moonPaint)
        canvas.drawCircle(cx + 18f, cy - 6f, 36f, bgPaint)
        // Stars
        val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(140, 170, 210) }
        canvas.drawCircle(cx + 60f, cy - 30f, 4f, star)
        canvas.drawCircle(cx + 44f, cy + 8f, 3f, star)
        canvas.drawCircle(cx + 76f, cy - 4f, 2.5f, star)
    }

    private fun savePdf(bmp: Bitmap, width: Int, height: Int): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
        val page = doc.startPage(pageInfo)
        page.canvas.drawBitmap(bmp, 0f, 0f, null)
        doc.finishPage(page)
        val outDir = File(context.filesDir, "exports").apply { mkdirs() }
        val name = "report_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".pdf"
        val file = File(outDir, name)
        FileOutputStream(file).use { doc.writeTo(it) }
        doc.close()
        return file
    }

    private fun savePng(bmp: Bitmap): File {
        val outDir = File(context.filesDir, "exports").apply { mkdirs() }
        val name = "report_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date()) + ".png"
        val file = File(outDir, name)
        FileOutputStream(file).use { fos -> bmp.compress(Bitmap.CompressFormat.PNG, 100, fos) }
        return file
    }
}