package com.example.dreamtracker.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Build
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.dreamtracker.data.DreamRepository
import com.example.dreamtracker.feature.recording.AudioRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun RecordDreamScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val audioRecorder = remember { AudioRecorder(context) }
    val repo = remember { DreamRepository(context) }

    val titleState = remember { mutableStateOf("") }
    val tagsState = remember { mutableStateOf("") }
    val transcriptState = remember { mutableStateOf("") }
    val moodState = remember { mutableStateOf(3f) }

    val isRecording = remember { mutableStateOf(false) }
    val recordedFile = remember { mutableStateOf<File?>(null) }
    val status = remember { mutableStateOf("") }
    val saving = remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!granted) {
            status.value = "Нет разрешения на микрофон"
        }
    }

    val notificationsPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    val speechLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) transcriptState.value = text
        }
    }

    fun requestNotificationsIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationsPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Text(text = "Новый сон", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = titleState.value,
            onValueChange = { titleState.value = it },
            label = { Text("Название (необязательно)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = tagsState.value,
            onValueChange = { tagsState.value = it },
            label = { Text("Теги (через запятую)") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Настроение: ${'$'}{moodState.value.toInt()}")
        }
        Slider(
            value = moodState.value,
            onValueChange = { moodState.value = it },
            valueRange = 1f..5f,
            steps = 3,
            colors = SliderDefaults.colors()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) }, modifier = Modifier.fillMaxWidth()) {
            Text("Запросить доступ к микрофону")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            if (!isRecording.value) {
                val file = audioRecorder.start()
                recordedFile.value = file
                isRecording.value = true
                status.value = "Идет запись..."
            } else {
                audioRecorder.stop()
                isRecording.value = false
                status.value = "Запись остановлена"
            }
        }, modifier = Modifier.fillMaxWidth()) {
            Text(if (isRecording.value) "Стоп" else "Записать голосом")
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(onClick = {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
                putExtra(RecognizerIntent.EXTRA_PROMPT, "Расскажите ваш сон")
            }
            speechLauncher.launch(intent)
        }, modifier = Modifier.fillMaxWidth()) {
            Text("Распознать речь (текст)")
        }

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = transcriptState.value,
            onValueChange = { transcriptState.value = it },
            label = { Text("Текст сна (можно отредактировать)") },
            modifier = Modifier.fillMaxWidth().height(180.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                requestNotificationsIfNeeded()
                saving.value = true
                status.value = "Сохраняю и анализирую..."
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val id = repo.saveDream(
                            recordedFile.value?.absolutePath,
                            transcriptState.value,
                            title = titleState.value,
                            moodScore = moodState.value.toInt(),
                            tags = tagsState.value
                        )
                        status.value = "Сохранено (#${'$'}id). Анализ готов."
                        onBack()
                    } finally {
                        saving.value = false
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !saving.value && transcriptState.value.isNotBlank()
        ) {
            Text(if (saving.value) "Анализ..." else "Сохранить и проанализировать")
        }

        Spacer(modifier = Modifier.height(8.dp))
        if (saving.value) {
            CircularProgressIndicator()
        } else if (status.value.isNotBlank()) {
            Text(status.value, style = MaterialTheme.typography.bodyMedium)
        }
    }
}