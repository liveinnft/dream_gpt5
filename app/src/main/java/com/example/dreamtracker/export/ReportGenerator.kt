package com.example.dreamtracker.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class ReportGenerator(private val context: Context) {
    private val dao = AppDatabase.get(context).dreamDao()

    suspend fun generate(periodDays: Int = 7): Pair<File, File> {
        val now = System.currentTimeMillis()
        val start = now - periodDays * 24L * 60L * 60L * 1000L
        val dreams = dao.getBetween(start, now)
        val summary = buildSummary(dreams)
        val (bmp, width, height) = drawReportBitmap(dreams, summary)
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

    private fun drawReportBitmap(dreams: List<Dream>, summary: Summary): Triple<Bitmap, Int, Int> {
        val width = 1240 // ~A4 width at ~150dpi
        val height = 1754
        val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        canvas.drawColor(Color.WHITE)
        val pTitle = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.BLACK; textSize = 48f }
        val pText = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.DKGRAY; textSize = 34f }
        val pSmall = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.GRAY; textSize = 28f }
        val pBar = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(120, 160, 200) }

        var y = 90f
        val title = "Отчёт по снам (${dateRange(dreams)})"
        canvas.drawText(title, 60f, y, pTitle); y += 60f
        canvas.drawText("Всего: ${summary.count}", 60f, y, pText); y += 46f
        canvas.drawText("Среднее настроение: ${String.format(Locale.getDefault(), "%.2f", summary.avgMood)}", 60f, y, pText); y += 54f

        // Tones (Pie-like bars)
        canvas.drawText("Тональность:", 60f, y, pText); y += 40f
        val totalTones = summary.toneTop.sumOf { it.second }
        var x = 60f
        summary.toneTop.forEach { (tone, count) ->
            val w = (count.toFloat() / totalTones.coerceAtLeast(1)) * (width - 120)
            pBar.color = nextColor(tone)
            canvas.drawRect(x, y, x + w, y + 30f, pBar)
            canvas.drawText("$tone ($count)", x, y + 50f, pSmall)
            x += w
        }
        y += 90f

        // Top symbols (bars)
        canvas.drawText("Частые символы:", 60f, y, pText); y += 16f
        val maxSym = summary.symbolsTop.maxOfOrNull { it.second } ?: 1
        summary.symbolsTop.forEach { (s, c) ->
            val bw = ((width - 280) * (c.toFloat() / maxSym)).coerceAtLeast(4f)
            canvas.drawText(s, 60f, y + 54f, pSmall)
            pBar.color = Color.rgb(110, 140, 180)
            canvas.drawRect(220f, y + 24f, 220f + bw, y + 54f, pBar)
            canvas.drawText(c.toString(), 230f + bw, y + 54f, pSmall)
            y += 60f
        }
        y += 20f

        // List of dreams (trimmed)
        canvas.drawText("Список снов:", 60f, y, pText); y += 40f
        val fmt = SimpleDateFormat("dd.MM HH:mm", Locale.getDefault())
        dreams.take(12).forEach { d ->
            canvas.drawText("${fmt.format(Date(d.createdAtEpoch))} • ${d.title.ifBlank { d.transcriptText.take(28) }}", 60f, y, pSmall)
            y += 34f
        }

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