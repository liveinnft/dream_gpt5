package com.example.dreamtracker.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import com.example.dreamtracker.ui.charts.BarChart
import com.example.dreamtracker.ui.charts.PieChart
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun StatsScreen() {
    val dao = remember { AppDatabase.get(androidx.compose.ui.platform.LocalContext.current).dreamDao() }
    val moodByDay = remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    val tones = remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    val topSymbols = remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }

    LaunchedEffect(Unit) {
        val dreams = dao.getAll()
        moodByDay.value = calcMoodByDay(dreams)
        tones.value = calcTones(dreams)
        topSymbols.value = calcTopSymbols(dreams)
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Статистика", style = MaterialTheme.typography.titleLarge)

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
    val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
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