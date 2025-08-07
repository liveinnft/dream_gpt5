package com.example.dreamtracker.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.network.OpenRouterService
import com.example.dreamtracker.settings.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember { SettingsRepository(context) }

    val currentModel = settings.modelFlow.collectAsState(initial = OpenRouterService.DEFAULT_MODEL)
    val demoRemain = settings.demoUsesFlow.collectAsState(initial = 0)

    val keyState = remember { mutableStateOf("") }
    val modelState = remember { mutableStateOf(currentModel.value) }

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
            CoroutineScope(Dispatchers.IO).launch { settings.saveApiKeyToFile(keyState.value) }
        }) { Text("Сохранить ключ") }

        Text("\nМодель")
        OutlinedTextField(
            value = modelState.value,
            onValueChange = { modelState.value = it },
            label = { Text("Идентификатор модели (скопируйте с сайта)") },
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = { CoroutineScope(Dispatchers.IO).launch { settings.setModel(modelState.value) } }) { Text("Сохранить модель") }
        Text("Текущая: ${'$'}{currentModel.value}")

        val remain = (5 - demoRemain.value).coerceAtLeast(0)
        Text("\nДемо-использований осталось: ${'$'}remain")
        if (remain == 0) {
            Text("Для продолжения добавьте свой ключ на сайте openrouter.ai и вставьте сюда.")
        }

        Text("\nКак получить ключ:", style = MaterialTheme.typography.titleMedium)
        Text("1) Зарегистрируйтесь на OpenRouter")
        Text("2) Перейдите в профиль → API Keys → Создать ключ")
        Text("3) Вставьте ключ сюда и нажмите ‘Сохранить ключ’")

        Text("\nСписок моделей (есть бесплатные):", style = MaterialTheme.typography.titleMedium)
        Text(
            text = "Смотреть на OpenRouter",
            modifier = Modifier.clickable {
                // no-op; в приложении можно открыть CustomTabs, но опустим
            },
            style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.Underline)
        )
        Text("Подсказка: на странице моделей отметьте фильтр ‘Free’. Скопируйте идентификатор (например, ‘openai/gpt-4o-mini’) и вставьте выше.")
    }
}