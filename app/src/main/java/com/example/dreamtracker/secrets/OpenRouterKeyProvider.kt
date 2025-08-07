package com.example.dreamtracker.secrets

import android.content.Context
import java.io.File

object OpenRouterKeyProvider {
    fun readKey(context: Context): String? {
        // 1) Try app-internal file: /data/data/<pkg>/files/openrouter_key.txt
        val internalFile = File(context.filesDir, "openrouter_key.txt")
        if (internalFile.exists()) {
            val key = internalFile.readText().trim()
            if (key.isNotEmpty()) return key
        }
        // 2) Try bundled asset (gitignored). Place file at app/src/main/assets/openrouter_key.txt
        return try {
            context.assets.open("openrouter_key.txt").bufferedReader().use { it.readText().trim() }.ifEmpty { null }
        } catch (_: Exception) {
            null
        }
    }
}