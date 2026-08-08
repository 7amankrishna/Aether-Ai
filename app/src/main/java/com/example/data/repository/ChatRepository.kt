package com.example.data.repository

import com.example.data.local.ConversationDao
import com.example.data.local.ConversationEntity
import com.example.data.local.FolderDao
import com.example.data.local.FolderEntity
import com.example.data.local.MemoryDao
import com.example.data.local.MemoryEntity
import com.example.data.local.MessageDao
import com.example.data.local.MessageEntity
import com.example.data.remote.GeminiApiClient
import com.example.domain.models.ChatMessage
import com.example.domain.models.ChatFolder
import com.example.domain.models.Conversation
import com.example.domain.models.UserMemory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

class ChatRepository(
    private val conversationDao: ConversationDao,
    private val messageDao: MessageDao,
    private val memoryDao: MemoryDao? = null,
    private val folderDao: FolderDao? = null,
    private val apiClient: GeminiApiClient = GeminiApiClient()
) {
    val allFolders: Flow<List<ChatFolder>> =
        folderDao?.getAllFolders()?.map { list -> list.map { it.toDomain() } }
            ?: kotlinx.coroutines.flow.flowOf(emptyList())

    suspend fun insertFolder(folder: ChatFolder) {
        folderDao?.insertFolder(FolderEntity.fromDomain(folder))
    }

    suspend fun updateFolder(folder: ChatFolder) {
        folderDao?.updateFolder(FolderEntity.fromDomain(folder))
    }

    suspend fun deleteFolder(id: String) {
        folderDao?.deleteFolder(id)
    }

    suspend fun updateConversationFolder(conversationId: String, folderId: String?) {
        conversationDao.updateFolderId(conversationId, folderId)
    }

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

    suspend fun getActiveMemoryContext(folderId: String? = null): String {
        val active = if (folderId != null) {
            memoryDao?.getActiveMemoriesForFolder(folderId) ?: emptyList()
        } else {
            memoryDao?.getActiveMemories() ?: emptyList()
        }
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
        providerId: String = "gemini",
        folderId: String? = null
    ): Conversation {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        val entity = ConversationEntity(
            id = id,
            title = title,
            createdAt = now,
            updatedAt = now,
            modelId = modelId,
            providerId = providerId,
            folderId = folderId
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

    suspend fun exportBackupJson(): String {
        val foldersList = folderDao?.getAllFoldersList() ?: emptyList()
        val conversationsList = conversationDao.getAllConversationsList()
        val messagesList = messageDao.getAllMessagesList()

        val root = JSONObject()
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())

        val foldersArray = JSONArray()
        foldersList.forEach { f ->
            val obj = JSONObject()
            obj.put("id", f.id)
            obj.put("name", f.name)
            obj.put("colorHex", f.colorHex)
            obj.put("emoji", f.emoji)
            obj.put("createdAt", f.createdAt)
            foldersArray.put(obj)
        }
        root.put("folders", foldersArray)

        val convsArray = JSONArray()
        conversationsList.forEach { c ->
            val obj = JSONObject()
            obj.put("id", c.id)
            obj.put("title", c.title)
            obj.put("createdAt", c.createdAt)
            obj.put("updatedAt", c.updatedAt)
            obj.put("isPinned", c.isPinned)
            obj.put("isArchived", c.isArchived)
            obj.put("modelId", c.modelId)
            obj.put("providerId", c.providerId)
            obj.put("systemPrompt", c.systemPrompt)
            obj.put("folderId", c.folderId ?: JSONObject.NULL)
            convsArray.put(obj)
        }
        root.put("conversations", convsArray)

        val msgsArray = JSONArray()
        messagesList.forEach { m ->
            val obj = JSONObject()
            obj.put("id", m.id)
            obj.put("conversationId", m.conversationId)
            obj.put("role", m.role)
            obj.put("content", m.content)
            obj.put("timestamp", m.timestamp)
            obj.put("imageUri", m.imageUri ?: JSONObject.NULL)
            obj.put("attachmentName", m.attachmentName ?: JSONObject.NULL)
            obj.put("attachmentType", m.attachmentType ?: JSONObject.NULL)
            msgsArray.put(obj)
        }
        root.put("messages", msgsArray)

        return root.toString(2)
    }

    suspend fun importBackupJson(jsonStr: String): Boolean {
        return try {
            val root = JSONObject(jsonStr)

            if (root.has("folders")) {
                val foldersArray = root.getJSONArray("folders")
                for (i in 0 until foldersArray.length()) {
                    val f = foldersArray.getJSONObject(i)
                    val folder = FolderEntity(
                        id = f.getString("id"),
                        name = f.getString("name"),
                        colorHex = f.optString("colorHex", "#E06D53"),
                        emoji = f.optString("emoji", "📁"),
                        createdAt = f.optLong("createdAt", System.currentTimeMillis())
                    )
                    folderDao?.insertFolder(folder)
                }
            }

            if (root.has("conversations")) {
                val convsArray = root.getJSONArray("conversations")
                for (i in 0 until convsArray.length()) {
                    val c = convsArray.getJSONObject(i)
                    val folderId = if (c.isNull("folderId")) null else c.optString("folderId", null)
                    val conv = ConversationEntity(
                        id = c.getString("id"),
                        title = c.optString("title", "New Chat"),
                        createdAt = c.optLong("createdAt", System.currentTimeMillis()),
                        updatedAt = c.optLong("updatedAt", System.currentTimeMillis()),
                        isPinned = c.optBoolean("isPinned", false),
                        isArchived = c.optBoolean("isArchived", false),
                        modelId = c.optString("modelId", "gemini-3.5-flash"),
                        providerId = c.optString("providerId", "gemini"),
                        systemPrompt = c.optString("systemPrompt", ""),
                        folderId = folderId
                    )
                    conversationDao.insertConversation(conv)
                }
            }

            if (root.has("messages")) {
                val msgsArray = root.getJSONArray("messages")
                for (i in 0 until msgsArray.length()) {
                    val m = msgsArray.getJSONObject(i)
                    val msg = MessageEntity(
                        id = m.getString("id"),
                        conversationId = m.getString("conversationId"),
                        role = m.optString("role", "user"),
                        content = m.optString("content", ""),
                        timestamp = m.optLong("timestamp", System.currentTimeMillis()),
                        imageUri = if (m.isNull("imageUri")) null else m.optString("imageUri", null),
                        attachmentName = if (m.isNull("attachmentName")) null else m.optString("attachmentName", null),
                        attachmentType = if (m.isNull("attachmentType")) null else m.optString("attachmentType", null)
                    )
                    messageDao.insertMessage(msg)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
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
        systemPrompt = systemPrompt,
        folderId = folderId
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
