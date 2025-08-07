package com.example.dreamtracker.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {
    enum class ReportTheme { LIGHT, DARK }

    private val dao = AppDatabase.get(context).dreamDao()

    suspend fun generate(periodDays: Int = 7, theme: ReportTheme = ReportTheme.LIGHT): Pair<File, File> {
        val now = System.currentTimeMillis()
        val start = now - periodDays * 24L * 60L * 60L * 1000L
        val dreams = dao.getBetween(start, now)
        val summary = buildSummary(dreams)
        val (bmp, width, height) = drawReportBitmap(dreams, summary, periodDays, theme)
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

    private data class Palette(
        val bgTop: Int,
        val bgBottom: Int,
        val card: Int,
        val cardShadow: Int,
        val title: Int,
        val sub: Int,
        val metric: Int,
        val metricVal: Int,
        val section: Int,
        val label: Int,
        val grid: Int,
        val barPrimary: Int,
        val barSecondary: Int,
        val footer: Int
    )

    private fun palette(theme: ReportTheme): Palette = when (theme) {
        ReportTheme.LIGHT -> Palette(
            bgTop = Color.rgb(238, 245, 252),
            bgBottom = Color.rgb(224, 235, 247),
            card = Color.WHITE,
            cardShadow = Color.argb(60, 0, 0, 0),
            title = Color.rgb(20, 35, 55),
            sub = Color.rgb(80, 100, 130),
            metric = Color.rgb(30, 50, 75),
            metricVal = Color.rgb(60, 90, 130),
            section = Color.rgb(30, 50, 75),
            label = Color.rgb(80, 100, 130),
            grid = Color.argb(60, 160, 180, 200),
            barPrimary = Color.rgb(130, 160, 200),
            barSecondary = Color.rgb(110, 140, 180),
            footer = Color.rgb(120, 140, 160)
        )
        ReportTheme.DARK -> Palette(
            bgTop = Color.rgb(18, 24, 32),
            bgBottom = Color.rgb(12, 18, 26),
            card = Color.rgb(24, 32, 44),
            cardShadow = Color.argb(80, 0, 0, 0),
            title = Color.rgb(220, 230, 240),
            sub = Color.rgb(170, 185, 200),
            metric = Color.rgb(210, 225, 240),
            metricVal = Color.rgb(120, 170, 220),
            section = Color.rgb(210, 225, 240),
            label = Color.rgb(180, 195, 210),
            grid = Color.argb(70, 120, 140, 160),
            barPrimary = Color.rgb(100, 140, 200),
            barSecondary = Color.rgb(80, 120, 180),
            footer = Color.rgb(150, 170, 190)
        )
    }

    private fun drawReportBitmap(
        dreams: List<Dream>,
        summary: Summary,
        periodDays: Int,
        theme: ReportTheme
    ): Triple<Bitmap, Int, Int> {
        val width = 1240
        val height = 1754
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)

        val pal = palette(theme)
        val sans = Typeface.SANS_SERIF
        val sansMedium = Typeface.create("sans-serif-medium", Typeface.NORMAL)

        // Background gradient
        val bg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(pal.bgTop, pal.bgBottom),
                floatArrayOf(0f, 1f), Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bg)

        fun drawCard(rect: RectF, radius: Float = 28f) {
            val shadow = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.cardShadow }
            canvas.drawRoundRect(RectF(rect.left, rect.top + 6, rect.right, rect.bottom + 6), radius, radius, shadow)
            val card = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.card }
            canvas.drawRoundRect(rect, radius, radius, card)
        }

        // Header
        val headerRect = RectF(60f, 60f, width - 60f, 240f)
        drawCard(headerRect)
        drawLogo(canvas, 90f, headerRect.centerY(), theme)
        val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.title; textSize = 58f; typeface = sansMedium }
        val pSub = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.sub; textSize = 34f; typeface = sans }
        canvas.drawText("DreamTracker", 170f, headerRect.top + 86f, pTitle)
        val periodLabel = if (periodDays <= 7) "Отчёт за неделю" else "Отчёт за месяц"
        canvas.drawText(periodLabel, 170f, headerRect.top + 135f, pSub)
        canvas.drawText(dateRange(dreams), 170f, headerRect.top + 175f, pSub)

        // Metrics
        val metricsRect = RectF(60f, headerRect.bottom + 20f, width - 60f, headerRect.bottom + 220f)
        drawCard(metricsRect)
        val pMetric = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.metric; textSize = 42f; typeface = sansMedium }
        val pMetricVal = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.metricVal; textSize = 42f; typeface = sansMedium }
        canvas.drawText("Всего снов:", metricsRect.left + 40f, metricsRect.top + 80f, pMetric)
        canvas.drawText("${summary.count}", metricsRect.left + 320f, metricsRect.top + 80f, pMetricVal)
        canvas.drawText("Среднее настроение:", metricsRect.left + 40f, metricsRect.top + 150f, pMetric)
        canvas.drawText(String.format(Locale.getDefault(), "%.2f", summary.avgMood), metricsRect.left + 460f, metricsRect.top + 150f, pMetricVal)

        // Tones
        val tonesRect = RectF(60f, metricsRect.bottom + 20f, width - 60f, metricsRect.bottom + 360f)
        drawCard(tonesRect)
        val pSec = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.section; textSize = 38f; typeface = sansMedium }
        canvas.drawText("Тональность", tonesRect.left + 40f, tonesRect.top + 64f, pSec)
        val legendX = tonesRect.right - 300f
        var legendY = tonesRect.top + 44f
        val totalTones = summary.toneTop.sumOf { it.second }.coerceAtLeast(1)
        val grid = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.grid; strokeWidth = 1f }
        val chartLeft = tonesRect.left + 40f
        val chartTop = tonesRect.top + 86f
        val chartRight = tonesRect.right - 320f
        val chartBottom = tonesRect.bottom - 44f
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop
        repeat(4) { i ->
            val y = chartTop + chartHeight * (i / 3f)
            canvas.drawLine(chartLeft, y, chartRight, y, grid)
        }
        var x = chartLeft
        summary.toneTop.forEach { (tone, count) ->
            val w = (count.toFloat() / totalTones) * chartWidth
            val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = nextColor(tone, pal) }
            val r = RectF(x, chartTop + 10f, x + w, chartBottom - 10f)
            canvas.drawRoundRect(r, 16f, 16f, barPaint)
            val dot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = barPaint.color }
            canvas.drawCircle(legendX, legendY, 10f, dot)
            val pLegend = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.label; textSize = 30f; typeface = sans }
            canvas.drawText("$tone ($count)", legendX + 20f, legendY + 10f, pLegend)
            legendY += 36f
            x += w + 14f
        }

        // Symbols
        val symbolsRect = RectF(60f, tonesRect.bottom + 20f, width - 60f, tonesRect.bottom + 560f)
        drawCard(symbolsRect)
        canvas.drawText("Частые символы", symbolsRect.left + 40f, symbolsRect.top + 64f, pSec)
        val sLeft = symbolsRect.left + 40f
        val sTop = symbolsRect.top + 96f
        val sRight = symbolsRect.right - 40f
        val sBottom = symbolsRect.bottom - 44f
        repeat(5) { i ->
            val y = sTop + (sBottom - sTop) * (i / 4f)
            canvas.drawLine(sLeft, y, sRight, y, grid)
        }
        val maxSym = summary.symbolsTop.maxOfOrNull { it.second } ?: 1
        var sy = sTop
        summary.symbolsTop.forEach { (s, c) ->
            val bw = ((sRight - sLeft - 240f) * (c.toFloat() / maxSym)).coerceAtLeast(6f)
            val label = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.label; textSize = 32f; typeface = sans }
            canvas.drawText(s, sLeft, sy + 36f, label)
            val bar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.barSecondary }
            val rr = RectF(sLeft + 200f, sy + 8f, sLeft + 200f + bw, sy + 40f)
            canvas.drawRoundRect(rr, 12f, 12f, bar)
            canvas.drawText(c.toString(), sLeft + 220f + bw, sy + 36f, label)
            sy += 58f
        }

        // List
        val listRect = RectF(60f, symbolsRect.bottom + 20f, width - 60f, height - 120f)
        drawCard(listRect)
        canvas.drawText("Список снов", listRect.left + 40f, listRect.top + 64f, pSec)
        val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        val pItem = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.label; textSize = 30f; typeface = sans }
        var ly = listRect.top + 96f
        dreams.take(16).forEach { d ->
            val line = "${fmt.format(Date(d.createdAtEpoch))} • ${d.title.ifBlank { d.transcriptText.take(32) }}"
            canvas.drawText(line, listRect.left + 40f, ly, pItem)
            ly += 36f
        }

        // Footer
        val foot = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.footer; textSize = 26f; typeface = sans }
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

    private fun nextColor(seed: String, pal: Palette): Int {
        val base = seed.hashCode()
        val r = 80 + (base shr 16 and 0x7F)
        val g = 90 + (base shr 8 and 0x7F)
        val b = 120 + (base and 0x7F)
        val c = Color.rgb(r, g, b)
        return if (isDark(c) && pal == palette(ReportTheme.DARK)) brighten(c, 0.3f) else c
    }

    private fun isDark(color: Int): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)
        val l = (0.299 * r + 0.587 * g + 0.114 * b)
        return l < 128
    }

    private fun brighten(color: Int, factor: Float): Int {
        val r = (Color.red(color) + 255 * factor).toInt().coerceAtMost(255)
        val g = (Color.green(color) + 255 * factor).toInt().coerceAtMost(255)
        val b = (Color.blue(color) + 255 * factor).toInt().coerceAtMost(255)
        return Color.rgb(r, g, b)
    }

    private fun drawLogo(canvas: Canvas, cx: Float, cy: Float, theme: ReportTheme) {
        val moon = if (theme == ReportTheme.DARK) Color.rgb(150, 190, 240) else Color.rgb(140, 170, 210)
        val moonPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = moon }
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.TRANSPARENT }
        canvas.drawCircle(cx, cy, 40f, moonPaint)
        // Crescent effect
        val overlay = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = if (theme == ReportTheme.DARK) Color.rgb(24, 32, 44) else Color.WHITE }
        canvas.drawCircle(cx + 18f, cy - 6f, 36f, overlay)
        val star = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = moon }
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