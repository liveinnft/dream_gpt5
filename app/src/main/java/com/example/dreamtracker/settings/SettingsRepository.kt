package com.example.dreamtracker.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.File

private val Context.dataStore by preferencesDataStore(name = "dream_prefs")

class SettingsRepository(private val context: Context) {
    companion object {
        private val KEY_MODEL = stringPreferencesKey("openrouter_model")
        private val KEY_DEMO_USES = intPreferencesKey("demo_uses")
        private const val DEMO_LIMIT = 5

        private val KEY_REMINDER_ON = booleanPreferencesKey("reminder_on")
        private val KEY_REMINDER_HOUR = intPreferencesKey("reminder_hour")
        private val KEY_REMINDER_MIN = intPreferencesKey("reminder_min")

        private val KEY_KEY_VALID = booleanPreferencesKey("key_valid")
        private val KEY_VALIDATION_MSG = stringPreferencesKey("validation_msg")
    }

    val modelFlow: Flow<String> = context.dataStore.data.map { it[KEY_MODEL] ?: com.example.dreamtracker.network.OpenRouterService.DEFAULT_MODEL }
    suspend fun setModel(model: String) { context.dataStore.edit { it[KEY_MODEL] = model } }
    suspend fun getModelOrDefault(defaultModel: String): String = context.dataStore.data.map { it[KEY_MODEL] ?: defaultModel }.first()

    val demoUsesFlow: Flow<Int> = context.dataStore.data.map { it[KEY_DEMO_USES] ?: 0 }
    suspend fun incrementDemoUse() { context.dataStore.edit { prefs -> prefs[KEY_DEMO_USES] = (prefs[KEY_DEMO_USES] ?: 0) + 1 } }
    suspend fun remainingDemoUses(): Int = (DEMO_LIMIT - demoUsesFlow.first()).coerceAtLeast(0)
    suspend fun isDemoLimitReached(): Boolean = demoUsesFlow.first() >= DEMO_LIMIT

    val reminderOnFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_REMINDER_ON] ?: false }
    val reminderHourFlow: Flow<Int> = context.dataStore.data.map { it[KEY_REMINDER_HOUR] ?: 9 }
    val reminderMinFlow: Flow<Int> = context.dataStore.data.map { it[KEY_REMINDER_MIN] ?: 0 }
    suspend fun setReminderOn(on: Boolean) { context.dataStore.edit { it[KEY_REMINDER_ON] = on } }
    suspend fun setReminderTime(hour: Int, minute: Int) { context.dataStore.edit { it[KEY_REMINDER_HOUR] = hour; it[KEY_REMINDER_MIN] = minute } }

    val keyValidFlow: Flow<Boolean> = context.dataStore.data.map { it[KEY_KEY_VALID] ?: false }
    val validationMsgFlow: Flow<String> = context.dataStore.data.map { it[KEY_VALIDATION_MSG] ?: "" }
    suspend fun setValidation(valid: Boolean, msg: String) { context.dataStore.edit { it[KEY_KEY_VALID] = valid; it[KEY_VALIDATION_MSG] = msg } }

    fun saveApiKeyToFile(key: String) { File(context.filesDir, "openrouter_key.txt").writeText(key.trim()) }
}