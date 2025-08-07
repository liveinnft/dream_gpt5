package com.example.dreamtracker.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.network.ChatCompletionRequest
import com.example.dreamtracker.network.ChatMessage
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
    val keyValid = settings.keyValidFlow.collectAsState(initial = false)
    val validationMsg = settings.validationMsgFlow.collectAsState(initial = "")

    var keyState by remember { mutableStateOf("") }
    var modelState by remember { mutableStateOf(currentModel.value) }

    var models by remember { mutableStateOf(listOf<String>()) }
    var status by remember { mutableStateOf("") }

    fun openUrl(url: String) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }

    fun validateAsync(key: String?, model: String) {
        status = "Проверка..."
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val service = OpenRouterService.create(key)
                val req = ChatCompletionRequest(
                    model = model,
                    messages = listOf(
                        ChatMessage("system", "Скажи ‘ok’. Ответ строго: ok"),
                        ChatMessage("user", "ping")
                    )
                )
                val resp = service.createCompletion(req)
                val ok = resp.choices.firstOrNull()?.message?.content?.trim()?.lowercase() == "ok"
                settings.setValidation(ok, if (ok) "Ключ/модель валидны" else "Ответ некорректен (проверьте модель/ключ)")
                status = validationMsg.value
            } catch (e: Exception) {
                settings.setValidation(false, e.message ?: "Ошибка валидации")
                status = "Ошибка: ${'$'}{e.message}"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Настройки", style = MaterialTheme.typography.titleLarge)
            TextButton(onClick = onBack) { Text("Назад") }
        }

        Text("OpenRouter API ключ")
        OutlinedTextField(
            value = keyState,
            onValueChange = { keyState = it },
            label = { Text("Вставьте ключ (из openrouter.ai)") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                CoroutineScope(Dispatchers.IO).launch { settings.saveApiKeyToFile(keyState) }
                validateAsync(keyState, currentModel.value)
            }) { Text("Сохранить ключ и проверить") }
        }
        if (status.isNotBlank()) Text(status)
        if (validationMsg.value.isNotBlank()) Text(validationMsg.value)
        Text(if (keyValid.value) "Ключ валиден" else "Ключ не проверен/недействителен")

        Text("\nВыбор модели")
        OutlinedTextField(
            value = modelState,
            onValueChange = { modelState = it },
            label = { Text("Идентификатор модели") },
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = {
                CoroutineScope(Dispatchers.IO).launch { settings.setModel(modelState) }
                validateAsync(keyState.ifBlank { null }, modelState)
            }) { Text("Сохранить модель и проверить") }
            Button(onClick = {
                status = "Загрузка моделей..."
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val service = OpenRouterService.create(keyState.ifBlank { null })
                        val res = service.listModels()
                        val free = res.data.orEmpty()
                            .filter { it.pricing?.prompt == 0.0 || it.pricing?.completion == 0.0 }
                            .map { it.id }
                        models = free
                        status = "Загружено: ${'$'}{free.size} моделей"
                    } catch (e: Exception) {
                        status = "Ошибка загрузки: ${'$'}{e.message}"
                    }
                }
            }) { Text("Загрузить бесплатные модели") }
            TextButton(onClick = { openUrl("https://openrouter.ai/models") }) {
                Text("Открыть список моделей", textDecoration = TextDecoration.Underline)
            }
        }

        if (models.isNotEmpty()) {
            Text("\nБесплатные модели:")
            LazyColumn(Modifier.weight(1f, fill = true)) {
                items(models) { m ->
                    Text(
                        text = m,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { modelState = m }
                            .padding(vertical = 6.dp)
                    )
                }
            }
        }

        val remain = (5 - demoRemain.value).coerceAtLeast(0)
        Text("\nДемо-использований осталось: ${'$'}remain")
        if (remain == 0) {
            Text("Для продолжения добавьте свой ключ на сайте openrouter.ai и вставьте сюда.")
        }

        Text("\nКак получить ключ:", style = MaterialTheme.typography.titleMedium)
        Text("1) Зарегистрируйтесь на OpenRouter")
        Text("2) Профиль → API Keys → Создать ключ")
        Text("3) Вставьте ключ и нажмите ‘Сохранить ключ’")
        TextButton(onClick = { openUrl("https://openrouter.ai/keys") }) {
            Text("Открыть страницу ключей", textDecoration = TextDecoration.Underline)
        }
    }
}