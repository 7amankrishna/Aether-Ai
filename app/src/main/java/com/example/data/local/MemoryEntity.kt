package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.models.UserMemory

@Entity(tableName = "user_memories")
data class MemoryEntity(
    @PrimaryKey val id: String,
    val category: String,
    val content: String,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val folderId: String? = null
) {
    fun toDomain(): UserMemory = UserMemory(
        id = id,
        category = category,
        content = content,
        isEnabled = isEnabled,
        timestamp = timestamp,
        folderId = folderId
    )

    companion object {
        fun fromDomain(domain: UserMemory): MemoryEntity = MemoryEntity(
            id = domain.id,
            category = domain.category,
            content = domain.content,
            isEnabled = domain.isEnabled,
            timestamp = domain.timestamp,
            folderId = domain.folderId
        )
    }
}
