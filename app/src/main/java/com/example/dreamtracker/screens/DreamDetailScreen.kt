package com.example.dreamtracker.screens

import android.content.Intent
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import com.example.dreamtracker.data.DreamRepository
import java.io.File
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun DreamDetailScreen(dreamId: Long?, onBack: () -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dreamDao() }
    val repo = remember { DreamRepository(context) }
    val dreamState = remember { mutableStateOf<Dream?>(null) }

    val editMode = remember { mutableStateOf(false) }
    val titleState = remember { mutableStateOf("") }
    val moodState = remember { mutableStateOf(3) }
    val tagsState = remember { mutableStateOf("") }
    val textState = remember { mutableStateOf("") }

    LaunchedEffect(dreamId) {
        val d = if (dreamId != null) dao.getById(dreamId) else dao.getAll().firstOrNull()
        dreamState.value = d
        if (d != null) {
            titleState.value = d.title
            moodState.value = d.moodScore
            tagsState.value = d.tags
            textState.value = d.transcriptText
        }
    }

    val dream = dreamState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = onBack) { Text("Назад") }
            Row {
                IconButton(onClick = {
                    if (dream != null) {
                        GlobalScope.launch { repo.toggleFavorite(dream.id); dreamState.value = dao.getById(dream.id) }
                    }
                }) {
                    Icon(painter = painterResource(id = if (dream?.isFavorite == true) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off), contentDescription = "Избранное")
                }
                IconButton(onClick = {
                    if (dream != null) {
                        val shareText = buildString {
                            appendLine(dream.title.ifBlank { "Сон #${dream.id}" })
                            if (dream.summary.isNotBlank()) appendLine("\nКраткое резюме:\n${dream.summary}")
                            if (dream.insights.isNotBlank()) appendLine("\nНаблюдения:\n${dream.insights}")
                            if (dream.recommendations.isNotBlank()) appendLine("\nРекомендации:\n${dream.recommendations}")
                        }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Поделиться сном"))
                    }
                }) {
                    Icon(painter = painterResource(id = android.R.drawable.ic_menu_share), contentDescription = "Поделиться")
                }
            }
        }

        Text("Детали сна", style = MaterialTheme.typography.titleLarge)
        if (dream != null) {
            if (!editMode.value) {
                if (dream.title.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text("Название: ${dream.title}", style = MaterialTheme.typography.titleMedium)
                }
                Spacer(modifier = Modifier.padding(4.dp))
                Text("Дата: ${formatEpoch(dream.createdAtEpoch)}")
                Text("Настроение: ${dream.moodScore}")
                if (dream.tags.isNotBlank()) Text("Теги: ${dream.tags}")

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
                            dream.insights.split('\n').forEach { Text("• ${it}") }
                        }
                    }
                }
                if (dream.recommendations.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(8.dp))
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Рекомендации", style = MaterialTheme.typography.titleMedium)
                            dream.recommendations.split('\n').forEach { Text("• ${it}") }
                        }
                    }
                }
                if (dream.tone.isNotBlank()) {
                    Spacer(modifier = Modifier.padding(8.dp))
                    Text("Тон: ${dream.tone}; Уверенность: ${String.format("%.2f", dream.confidence)}")
                }

                Spacer(modifier = Modifier.padding(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { editMode.value = true }) { Text("Редактировать") }
                    Button(onClick = {
                        GlobalScope.launch { repo.deleteDream(dream.id); onBack() }
                    }) { Text("Удалить") }
                }
            } else {
                Spacer(modifier = Modifier.padding(8.dp))
                OutlinedTextField(value = titleState.value, onValueChange = { titleState.value = it }, label = { Text("Название") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.padding(6.dp))
                OutlinedTextField(value = tagsState.value, onValueChange = { tagsState.value = it }, label = { Text("Теги") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.padding(6.dp))
                OutlinedTextField(value = textState.value, onValueChange = { textState.value = it }, label = { Text("Текст сна") }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.padding(12.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Button(onClick = { editMode.value = false }) { Text("Отмена") }
                    Button(onClick = {
                        val updated = dream.copy(title = titleState.value, tags = tagsState.value, transcriptText = textState.value)
                        GlobalScope.launch { repo.updateDream(updated); dreamState.value = dao.getById(dream.id); editMode.value = false }
                    }) { Text("Сохранить") }
                }
            }
        } else {
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Сон не найден")
        }
    }
}

@Composable
private fun AudioPlayerControl(filePath: String) {
    val playerState = remember { mutableStateOf<MediaPlayer?>(null) }
    val isPlaying = remember { mutableStateOf(false) }

    fun start() {
        try {
            val file = File(filePath)
            if (!file.exists()) return
            val mp = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                prepare()
                setOnCompletionListener { isPlaying.value = false }
            }
            mp.start()
            playerState.value = mp
            isPlaying.value = true
        } catch (_: Exception) {}
    }

    fun stop() {
        playerState.value?.apply {
            stop(); release()
        }
        playerState.value = null
        isPlaying.value = false
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Button(onClick = { if (isPlaying.value) stop() else start() }) { Text(if (isPlaying.value) "Стоп" else "Воспроизвести") }
    }
}

@Composable
private fun formatEpoch(epoch: Long): String = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(epoch))