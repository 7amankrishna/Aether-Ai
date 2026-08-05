package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM user_memories ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryEntity>>

    @Query("SELECT * FROM user_memories WHERE isEnabled = 1")
    suspend fun getActiveMemories(): List<MemoryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryEntity)

    @Query("UPDATE user_memories SET isEnabled = :isEnabled WHERE id = :id")
    suspend fun updateMemoryState(id: String, isEnabled: Boolean)

    @Query("DELETE FROM user_memories WHERE id = :id")
    suspend fun deleteMemory(id: String)

    @Query("DELETE FROM user_memories")
    suspend fun clearAll()
}
