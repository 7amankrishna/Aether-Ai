package com.example.domain.models

data class AccessPoint(
    val id: String,
    val name: String,
    val endpointUrl: String,
    val apiKey: String,
    val defaultModelId: String = "claude-3-5-sonnet-20241022",
    val isDefault: Boolean = false
)
