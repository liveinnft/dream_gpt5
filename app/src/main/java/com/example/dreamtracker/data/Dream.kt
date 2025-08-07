package com.example.dreamtracker.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "dreams")
data class Dream(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val createdAtEpoch: Long = System.currentTimeMillis(),
    val audioFilePath: String? = null,
    val transcriptText: String = "",
    val symbolsMatched: String = "", // comma-separated
    val analysisText: String = ""
)