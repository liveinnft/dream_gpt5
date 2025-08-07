package com.example.dreamtracker.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream

@Composable
fun DreamListScreen(onAddNew: () -> Unit, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dreamDao() }
    val dreams = dao.observeAll().collectAsState(initial = emptyList())

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сны", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onAddNew) { Text("Добавить") }
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(dreams.value) { dream ->
                DreamItem(dream) { onOpen(dream.id) }
            }
        }
    }
}

@Composable
private fun DreamItem(dream: Dream, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable { onClick() }) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = "#${dream.id} · ${formatEpoch(dream.createdAtEpoch)}", style = MaterialTheme.typography.labelMedium)
            Text(text = dream.transcriptText.take(140), style = MaterialTheme.typography.bodyMedium)
            if (dream.symbolsMatched.isNotBlank()) {
                Text(text = "Символы: ${dream.symbolsMatched}", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun formatEpoch(epoch: Long): String = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm").format(java.util.Date(epoch))