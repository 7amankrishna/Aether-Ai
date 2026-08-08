package com.example.domain.models

data class Conversation(
    val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val modelId: String = "gemini-3.5-flash",
    val providerId: String = "gemini",
    val systemPrompt: String = "",
    val folderId: String? = null
)
