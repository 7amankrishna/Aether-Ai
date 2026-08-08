package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.models.ChatFolder

@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey val id: String,
    val name: String,
    val colorHex: String,
    val emoji: String,
    val createdAt: Long
) {
    fun toDomain(): ChatFolder = ChatFolder(
        id = id,
        name = name,
        colorHex = colorHex,
        emoji = emoji,
        createdAt = createdAt
    )

    companion object {
        fun fromDomain(domain: ChatFolder): FolderEntity = FolderEntity(
            id = domain.id,
            name = domain.name,
            colorHex = domain.colorHex,
            emoji = domain.emoji,
            createdAt = domain.createdAt
        )
    }
}
