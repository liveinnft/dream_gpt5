package com.example.dreamtracker.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import com.example.dreamtracker.export.ReportGenerator
import com.example.dreamtracker.ui.charts.BarChart
import com.example.dreamtracker.ui.charts.PieChart
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen() {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dreamDao() }
    val moodByDay = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val tones = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val topSymbols = remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    val periodDays = remember { mutableStateOf(7) }
    val reportTheme = remember { mutableStateOf(ReportGenerator.ReportTheme.LIGHT) }
    val sharePng = remember { mutableStateOf(false) }

    LaunchedEffect(periodDays.value) {
        val now = System.currentTimeMillis()
        val start = now - periodDays.value * 24L * 60L * 60L * 1000L
        val dreams = dao.getBetween(start, now)
        moodByDay.value = calcMoodByDay(dreams)
        tones.value = calcTones(dreams)
        topSymbols.value = calcTopSymbols(dreams)
    }

    fun share(file: File, mime: String) {
        val uri = FileProvider.getUriForFile(context, context.packageName + ".fileprovider", file)
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = mime
            putExtra(android.content.Intent.EXTRA_STREAM, uri)
            addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Поделиться отчётом"))
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Статистика", style = MaterialTheme.typography.titleLarge)

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { periodDays.value = 7 }) { Text("Неделя") }
            Button(onClick = { periodDays.value = 30 }, modifier = Modifier.padding(start = 8.dp)) { Text("Месяц") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { reportTheme.value = ReportGenerator.ReportTheme.LIGHT }) { Text("Светлая тема отчёта") }
            Button(onClick = { reportTheme.value = ReportGenerator.ReportTheme.DARK }, modifier = Modifier.padding(start = 8.dp)) { Text("Тёмная тема отчёта") }
        }

        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            Button(onClick = { sharePng.value = false }) { Text("PDF") }
            Button(onClick = { sharePng.value = true }, modifier = Modifier.padding(start = 8.dp)) { Text("PNG") }
            Button(onClick = {
                GlobalScope.launch {
                    val (pdf, png) = ReportGenerator(context).generate(periodDays.value, reportTheme.value)
                    if (sharePng.value) share(png, "image/png") else share(pdf, "application/pdf")
                }
            }, modifier = Modifier.padding(start = 8.dp)) { Text("Экспорт и поделиться") }
        }

        AnimatedVisibility(visible = moodByDay.value.isNotEmpty(), enter = fadeIn() + slideInVertically()) {
            Column {
                Text("\nСреднее настроение по дням:")
                BarChart(data = moodByDay.value.map { it.key to it.value.toFloat() })
            }
        }

        AnimatedVisibility(visible = tones.value.isNotEmpty(), enter = fadeIn() + slideInVertically()) {
            Column {
                Text("\nТональность (распределение):")
                PieChart(data = tones.value.map { it.key to it.value.toFloat() })
            }
        }

        AnimatedVisibility(visible = topSymbols.value.isNotEmpty(), enter = fadeIn() + slideInVertically()) {
            Column {
                Text("\nТоп символов:")
                BarChart(data = topSymbols.value.take(8).map { it.first to it.second.toFloat() })
            }
        }
    }
}

private fun calcMoodByDay(dreams: List<Dream>): Map<String, Double> {
    val fmt = SimpleDateFormat("dd.MM", Locale.getDefault())
    val groups = dreams.groupBy { fmt.format(Date(it.createdAtEpoch)) }
    return groups.mapValues { (_, ds) -> ds.map { it.moodScore }.average() }.toSortedMap()
}

private fun calcTones(dreams: List<Dream>): Map<String, Int> =
    dreams.map { it.tone.ifBlank { "не указан" } }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }.toMap()

private fun calcTopSymbols(dreams: List<Dream>): List<Pair<String, Int>> {
    val counts = mutableMapOf<String, Int>()
    for (d in dreams) {
        d.symbolsMatched.split(',').map { it.trim() }.filter { it.isNotBlank() }.forEach { s -> counts[s] = (counts[s] ?: 0) + 1 }
    }
    return counts.entries.sortedByDescending { it.value }.map { it.key to it.value }
}