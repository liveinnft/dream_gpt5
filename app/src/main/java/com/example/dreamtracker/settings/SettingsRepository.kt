package com.example.dreamtracker.settings

import android.content.Context
import java.io.File

class SettingsRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("dream_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_MODEL = "openrouter_model"
        private const val KEY_DEMO_USES = "demo_uses"
        private const val DEMO_LIMIT = 5
    }

    fun getModelOrDefault(defaultModel: String): String {
        return prefs.getString(KEY_MODEL, defaultModel) ?: defaultModel
    }

    fun setModel(model: String) {
        prefs.edit().putString(KEY_MODEL, model).apply()
    }

    fun getDemoUses(): Int = prefs.getInt(KEY_DEMO_USES, 0)

    fun incrementDemoUse() {
        val cur = getDemoUses()
        prefs.edit().putInt(KEY_DEMO_USES, cur + 1).apply()
    }

    fun remainingDemoUses(): Int = (DEMO_LIMIT - getDemoUses()).coerceAtLeast(0)

    fun isDemoLimitReached(): Boolean = getDemoUses() >= DEMO_LIMIT

    fun saveApiKeyToFile(key: String) {
        val file = File(context.filesDir, "openrouter_key.txt")
        file.writeText(key.trim())
    }
}