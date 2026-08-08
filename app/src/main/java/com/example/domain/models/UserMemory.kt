package com.example.domain.models

data class UserMemory(
    val id: String,
    val category: String,
    val content: String,
    val isEnabled: Boolean = true,
    val timestamp: Long = System.currentTimeMillis(),
    val folderId: String? = null
)
