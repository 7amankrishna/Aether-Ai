package com.example.domain.models

enum class AttachmentType { IMAGE, DOCUMENT, TEXT }

data class Attachment(
    val uriString: String,
    val name: String,
    val type: AttachmentType,
    val textContent: String? = null
)
