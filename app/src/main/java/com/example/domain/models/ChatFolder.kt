package com.example.domain.models

data class ChatFolder(
    val id: String,
    val name: String,
    val colorHex: String,
    val emoji: String,
    val createdAt: Long = System.currentTimeMillis()
)
