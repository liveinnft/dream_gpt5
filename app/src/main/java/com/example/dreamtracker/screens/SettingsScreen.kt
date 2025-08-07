package com.example.dreamtracker.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.network.OpenRouterService
import com.example.dreamtracker.settings.SettingsRepository

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }

    val keyState = remember { mutableStateOf("") }
    val modelState = remember { mutableStateOf(settings.getModelOrDefault(OpenRouterService.DEFAULT_MODEL)) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Настройки", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("Назад") }
        }

        Text("OpenRouter API ключ")
        OutlinedTextField(
            value = keyState.value,
            onValueChange = { keyState.value = it },
            label = { Text("Вставьте ключ (из openrouter.ai)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            settings.saveApiKeyToFile(keyState.value)
        }) { Text("Сохранить ключ") }

        Text("\nМодель")
        OutlinedTextField(
            value = modelState.value,
            onValueChange = { modelState.value = it },
            label = { Text("Идентификатор модели (скопируйте с сайта)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { settings.setModel(modelState.value) }) { Text("Сохранить модель") }

        val remain = settings.remainingDemoUses()
        Text("\nДемо-использований осталось: ${'$'}remain")
        if (remain == 0) {
            Text("Для продолжения добавьте свой ключ на сайте openrouter.ai и вставьте сюда.")
        }
    }
}