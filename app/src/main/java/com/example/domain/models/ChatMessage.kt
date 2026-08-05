package com.example.domain.models

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val role: String, // "user", "assistant", "system"
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val imageUri: String? = null,
    val attachmentName: String? = null,
    val attachmentType: String? = null,
    val tokenCount: Int = 0,
    val isStreaming: Boolean = false,
    val isError: Boolean = false
)
