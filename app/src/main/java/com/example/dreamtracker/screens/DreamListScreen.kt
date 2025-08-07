package com.example.dreamtracker.screens

import android.app.TimePickerDialog
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import com.example.dreamtracker.data.DreamRepository
import com.example.dreamtracker.export.ExportImportManager
import com.example.dreamtracker.notifications.ReminderScheduler
import com.example.dreamtracker.settings.SettingsRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun DreamListScreen(onAddNew: () -> Unit, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dreamDao() }
    val repo = remember { DreamRepository(context) }
    val settings = remember { SettingsRepository(context) }
    val exportManager = remember { ExportImportManager(context) }
    val dreams = dao.observeAll().collectAsState(initial = emptyList())

    var query by remember { mutableStateOf("") }
    var favoritesOnly by remember { mutableStateOf(false) }

    val remindersOn = settings.reminderOnFlow.collectAsState(initial = false)
    val hour = settings.reminderHourFlow.collectAsState(initial = 9)
    val minute = settings.reminderMinFlow.collectAsState(initial = 0)

    val openDoc = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            GlobalScope.launch {
                val input = context.contentResolver.openInputStream(uri) ?: return@launch
                val temp = kotlin.io.path.createTempFile().toFile()
                temp.outputStream().use { out -> input.copyTo(out) }
                exportManager.importFromFile(temp)
            }
        }
    }

    val filtered = dreams.value.filter { d ->
        val matchQuery = query.isBlank() ||
            d.title.contains(query, true) ||
            d.transcriptText.contains(query, true) ||
            d.tags.contains(query, true)
        val matchFav = !favoritesOnly || d.isFavorite
        matchQuery && matchFav
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically()) {
            Button(onClick = onAddNew, modifier = Modifier.fillMaxWidth()) { Text("Записать сон") }
        }
        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сны", style = MaterialTheme.typography.titleLarge)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("Поиск (название, текст, теги)") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row {
                IconButton(onClick = { favoritesOnly = !favoritesOnly }) {
                    Icon(
                        painter = painterResource(id = if (favoritesOnly) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off),
                        contentDescription = "Фильтр избранного"
                    )
                }
                Text(if (favoritesOnly) "Избранное" else "Все")
            }
            Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                Text("Напоминание: ")
                Switch(checked = remindersOn.value, onCheckedChange = {
                    GlobalScope.launch {
                        settings.setReminderOn(it)
                        if (it) ReminderScheduler.scheduleDaily(context, hour.value, minute.value) else ReminderScheduler.cancel(context)
                    }
                })
                Button(onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        GlobalScope.launch { settings.setReminderTime(h, m) }
                        if (remindersOn.value) ReminderScheduler.scheduleDaily(context, h, m)
                    }, hour.value, minute.value, true).show()
                }) { Text(String.format("%02d:%02d", hour.value, minute.value)) }
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Button(onClick = {
                GlobalScope.launch {
                    val file = exportManager.exportToFile()
                    exportManager.shareFile(file)
                }
            }) { Text("Экспорт/Поделиться") }
            Button(onClick = { openDoc.launch(arrayOf("application/json")) }) { Text("Импорт JSON") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            itemsIndexed(filtered, key = { _, item -> item.id }) { index, dream ->
                AnimatedVisibility(visible = true, enter = fadeIn() + slideInVertically(initialOffsetY = { it / 8 * index.coerceAtMost(8) })) {
                    DreamItem(dream,
                        onClick = { onOpen(dream.id) },
                        onToggleFavorite = { GlobalScope.launch { repo.toggleFavorite(dream.id) } }
                    )
                }
            }
        }
    }
}

@Composable
private fun DreamItem(dream: Dream, onClick: () -> Unit, onToggleFavorite: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = "#${dream.id} · ${formatEpoch(dream.createdAtEpoch)}", style = MaterialTheme.typography.labelMedium)
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        painter = painterResource(id = if (dream.isFavorite) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off),
                        contentDescription = "Избранное"
                    )
                }
            }
            if (dream.title.isNotBlank()) {
                Text(text = dream.title, style = MaterialTheme.typography.titleMedium)
            }
            Text(text = dream.transcriptText.take(120), style = MaterialTheme.typography.bodyMedium)
            if (dream.tags.isNotBlank()) {
                Text(text = "Теги: ${dream.tags}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun formatEpoch(epoch: Long): String = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(epoch))