package com.example.data.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.remote.GeminiApiClient
import com.example.domain.models.ChatMessage
import com.example.domain.models.Conversation
import com.example.domain.models.UserMemory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao? = null,
    private val apiClient: GeminiApiClient = GeminiApiClient()
) {
    val allMemories: Flow<List<UserMemory>> =
        memoryDao?.getAllMemories()?.map { list -> list.map { it.toDomain() } }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertMemory(memory: UserMemory) {
        memoryDao?.insertMemory(MemoryEntity.fromDomain(memory))
    }

    suspend fun updateMemoryState(id: String, isEnabled: Boolean) {
        memoryDao?.updateMemoryState(id, isEnabled)
    }

    suspend fun deleteMemory(id: String) {
        memoryDao?.deleteMemory(id)
    }

    suspend fun getActiveMemoryContext(): String {
        val active = memoryDao?.getActiveMemories() ?: emptyList()
        if (active.isEmpty()) return ""
        return buildString {
            append("\n\n[USER MEMORY & PERSISTENT CONTEXT]\n")
            active.forEach { mem ->
                append("- [${mem.category}]: ${mem.content}\n")
            }
        }
    }
    val activeConversations: Flow<List<Conversation>> =
        conversationDao.getActiveConversations().map { entities ->
            entities.map { it.toDomain() }
        }

    val archivedConversations: Flow<List<Conversation>> =
        conversationDao.getArchivedConversations().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getMessagesForConversation(conversationId: String): Flow<List<ChatMessage>> =
        messageDao.getMessagesForConversation(conversationId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun createNewConversation(
        title: String = "New Chat",
        modelId: String = "gemini-3.5-flash",
        providerId: String = "gemini"
    ): Conversation {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            modelId = modelId,
            providerId = providerId
        )
        conversationDao.insertConversation(entity)
        return entity.toDomain()
    }

    suspend fun insertMessage(message: ChatMessage) {
        messageDao.insertMessage(message.toEntity())
        // Touch conversation update timestamp
        conversationDao.getConversationById(message.conversationId)?.let { conv ->
            val updatedTitle = if (conv.title == "New Chat" && message.role == "user") {
                message.content.take(30).ifBlank { "New Chat" }
            } else conv.title

            conversationDao.updateConversation(
                conv.copy(
                    updatedAt = System.currentTimeMillis(),
                    title = updatedTitle
                )
            )
        }
    }

    suspend fun setPinned(conversationId: String, isPinned: Boolean) {
        conversationDao.setPinned(conversationId, isPinned)
    }

    suspend fun setArchived(conversationId: String, isArchived: Boolean) {
        conversationDao.setArchived(conversationId, isArchived)
    }

    suspend fun updateTitle(conversationId: String, newTitle: String) {
        conversationDao.updateTitle(conversationId, newTitle)
    }

    suspend fun deleteConversation(conversationId: String) {
        messageDao.deleteMessagesForConversation(conversationId)
        conversationDao.deleteConversationById(conversationId)
    }

    suspend fun clearAll() {
        messageDao.clearAllMessages()
        conversationDao.clearAll()
    }

    suspend fun updateConversationModel(conversationId: String, modelId: String, providerId: String) {
        conversationDao.updateModelAndProvider(conversationId, modelId, providerId)
    }

    suspend fun fetchRemoteModels(
        customEndpoint: String = "",
        customApiKey: String = "",
        providerId: String = "aerolink"
    ) = apiClient.fetchRemoteModels(customEndpoint, customApiKey, providerId)

    fun streamAiResponse(
        prompt: String,
        history: List<ChatMessage>,
        systemPrompt: String,
        modelId: String,
        customApiKey: String,
        customEndpoint: String,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Flow<String> {
        val formattedHistory = history.map { Pair(it.role, it.content) }
        return apiClient.streamGenerateContent(
            prompt = prompt,
            history = formattedHistory,
            systemPrompt = systemPrompt,
            modelId = modelId,
            customApiKey = customApiKey,
            customEndpoint = customEndpoint,
            imageBase64 = imageBase64,
            imageMimeType = imageMimeType
        )
    }

    // Mapping extensions
    private fun ConversationEntity.toDomain() = Conversation(
        id = id,
        title = title,
        createdAt = createdAt,
        updatedAt = updatedAt,
        isPinned = isPinned,
        isArchived = isArchived,
        modelId = modelId,
        providerId = providerId,
        systemPrompt = systemPrompt
    )

    private fun MessageEntity.toDomain() = ChatMessage(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        imageUri = imageUri,
        attachmentName = attachmentName,
        attachmentType = attachmentType,
        tokenCount = tokenCount,
        isError = isError
    )

    private fun ChatMessage.toEntity() = MessageEntity(
        id = id,
        conversationId = conversationId,
        role = role,
        content = content,
        timestamp = timestamp,
        imageUri = imageUri,
        attachmentName = attachmentName,
        attachmentType = attachmentType,
        tokenCount = tokenCount,
        isError = isError
    )
}
