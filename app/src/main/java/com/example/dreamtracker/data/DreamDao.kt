package com.example.dreamtracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DreamDao {
    @Query("SELECT * FROM dreams ORDER BY createdAtEpoch DESC")
    fun observeAll(): Flow<List<Dream>>

    @Query("SELECT * FROM dreams ORDER BY createdAtEpoch DESC")
    suspend fun getAll(): List<Dream>

    @Query("SELECT * FROM dreams WHERE id = :id")
    suspend fun getById(id: Long): Dream?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(dream: Dream): Long
}