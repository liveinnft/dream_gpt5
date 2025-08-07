package com.example.dreamtracker.screens

import android.media.MediaPlayer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import java.io.File

@Composable
fun DreamDetailScreen(dreamId: Long?, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dreamDao() }
    val dreamState = remember { mutableStateOf<Dream?>(null) }

    LaunchedEffect(dreamId) {
        dreamState.value = if (dreamId != null) dao.getById(dreamId) else dao.getAll().firstOrNull()
    }

    val dream = dreamState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Button(onClick = onBack) { Text("Назад") }
        Spacer(modifier = Modifier.padding(8.dp))
        Text("Детали сна", style = MaterialTheme.typography.titleLarge)
        if (dream != null) {
            if (dream.title.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Text("Название: ${'$'}{dream.title}", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(modifier = Modifier.padding(4.dp))
            Text("Дата: ${'$'}{formatEpoch(dream.createdAtEpoch)}")
            Text("Настроение: ${'$'}{dream.moodScore}")

            Spacer(modifier = Modifier.padding(8.dp))
            Text("Текст:")
            Text(dream.transcriptText)

            if (dream.audioFilePath != null) {
                Spacer(modifier = Modifier.padding(8.dp))
                AudioPlayerControl(filePath = dream.audioFilePath)
            }

            if (dream.summary.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Краткое резюме", style = MaterialTheme.typography.titleMedium)
                        Text(dream.summary)
                    }
                }
            }
            if (dream.insights.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Наблюдения", style = MaterialTheme.typography.titleMedium)
                        dream.insights.split('\n').forEach { Text("• ${'$'}it") }
                    }
                }
            }
            if (dream.recommendations.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text("Рекомендации", style = MaterialTheme.typography.titleMedium)
                        dream.recommendations.split('\n').forEach { Text("• ${'$'}it") }
                    }
                }
            }
            if (dream.tone.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Text("Тон: ${'$'}{dream.tone}; Уверенность: ${'$'}{String.format("%.2f", dream.confidence)}")
            }
            if (dream.symbolsMatched.isNotBlank()) {
                Spacer(modifier = Modifier.padding(8.dp))
                Text("Символы: ${'$'}{dream.symbolsMatched}")
            }
        } else {
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Сон не найден")
        }
    }
}

@Composable
private fun AudioPlayerControl(filePath: String) {
    val context = LocalContext.current
    val playerState = remember { mutableStateOf<MediaPlayer?>(null) }
    val isPlaying = remember { mutableStateOf(false) }

    fun start() {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener {
                    isPlaying.value = false
                }
            }
            mp.start()
            playerState.value = mp
            isPlaying.value = true
        } catch (_: Exception) {}
    }

    fun stop() {
        playerState.value?.apply {
            stop()
            release()
        }
        playerState.value = null
        isPlaying.value = false
    }

    RowWithButtons(
        leftText = if (isPlaying.value) "Стоп" else "Воспроизвести",
        onLeft = { if (isPlaying.value) stop() else start() }
    )
}

@Composable
private fun RowWithButtons(leftText: String, onLeft: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
        Button(onClick = onLeft) { Text(leftText) }
    }
}

@Composable
private fun formatEpoch(epoch: Long): String = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(epoch))