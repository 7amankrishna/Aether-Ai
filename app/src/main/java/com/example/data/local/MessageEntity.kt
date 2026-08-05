package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long,
    val imageUri: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val tokenCount: Int = 0,
    val isError: Boolean = false
)
