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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream
import com.example.dreamtracker.data.DreamRepository
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@Composable
fun DreamListScreen(onAddNew: () -> Unit, onOpen: (Long) -> Unit) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.get(context).dreamDao() }
    val repo = remember { DreamRepository(context) }
    val dreams = dao.observeAll().collectAsState(initial = emptyList())

    val queryState = remember { mutableStateOf("") }
    val favoritesOnly = remember { mutableStateOf(false) }

    val filtered = dreams.value.filter { d ->
        val matchQuery = queryState.value.isBlank() ||
            d.title.contains(queryState.value, true) ||
            d.transcriptText.contains(queryState.value, true) ||
            d.tags.contains(queryState.value, true)
        val matchFav = !favoritesOnly.value || d.isFavorite
        matchQuery && matchFav
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Сны", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onAddNew) { Text("Добавить") }
        }

        OutlinedTextField(
            value = queryState.value,
            onValueChange = { queryState.value = it },
            label = { Text("Поиск (название, текст, теги)") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            IconButton(onClick = { favoritesOnly.value = !favoritesOnly.value }) {
                Icon(
                    painter = painterResource(id = if (favoritesOnly.value) android.R.drawable.btn_star_big_on else android.R.drawable.btn_star_big_off),
                    contentDescription = "Фильтр избранного"
                )
            }
        }

        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(filtered) { dream ->
                DreamItem(dream,
                    onClick = { onOpen(dream.id) },
                    onToggleFavorite = { GlobalScope.launch { repo.toggleFavorite(dream.id) } }
                )
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