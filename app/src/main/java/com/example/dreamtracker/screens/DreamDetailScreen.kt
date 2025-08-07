package com.example.dreamtracker.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import com.example.dreamtracker.data.AppDatabase
import com.example.dreamtracker.data.Dream

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
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Текст:")
            Text(dream.transcriptText)
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Анализ:")
            Text(dream.analysisText)
        } else {
            Spacer(modifier = Modifier.padding(8.dp))
            Text("Сон не найден")
        }
    }
}