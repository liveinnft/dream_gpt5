package com.example.dreamtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val title: String = "",
    val moodScore: Int = 3, // 1..5
    val audioFilePath: String? = null,
    val transcriptText: String = "",

    // Structured analysis fields
    val summary: String = "",
    val symbolsMatched: String = "", // comma-separated
    val insights: String = "", // bullet list joined by \n
    val recommendations: String = "", // bullet list joined by \n
    val tone: String = "", // e.g., calm/anxious
    val confidence: Float = 0f,

    // UX fields
    val isFavorite: Boolean = false,
    val tags: String = "",

    // Raw JSON for future use
    val analysisJson: String = ""
)