package com.example.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.UserPreferences
import com.example.data.repository.ChatRepository
import com.example.domain.models.Attachment
import com.example.domain.models.ChatMessage
import com.example.domain.models.Conversation
import com.example.domain.models.ProviderRegistry
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

import com.example.domain.models.ChatFolder
import com.example.domain.models.UserMemory
import com.example.utils.TtsHelper

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getInstance(application)
    private val repository = ChatRepository(
        conversationDao = database.conversationDao(),
        messageDao = database.messageDao(),
        memoryDao = database.memoryDao(),
        folderDao = database.folderDao()
    )
    val userPreferences = UserPreferences(application)
    val ttsHelper = TtsHelper(application)

    val folders: StateFlow<List<ChatFolder>> = repository.allFolders
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedFolderId = MutableStateFlow<String?>(null)
    val selectedFolderId: StateFlow<String?> = _selectedFolderId.asStateFlow()

    val activeConversations: StateFlow<List<Conversation>> = repository.activeConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedConversations: StateFlow<List<Conversation>> = repository.archivedConversations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memories: StateFlow<List<UserMemory>> = repository.allMemories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings = userPreferences.settings

    private val _selectedConversation = MutableStateFlow<Conversation?>(null)
    val selectedConversation: StateFlow<Conversation?> = _selectedConversation.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _promptText = MutableStateFlow("")
    val promptText: StateFlow<String> = _promptText.asStateFlow()

    private val _attachments = MutableStateFlow<List<Attachment>>(emptyList())
    val attachments: StateFlow<List<Attachment>> = _attachments.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var currentStreamJob: Job? = null
    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            // Auto-fetch remote models for configured endpoint on launch
            val s = settings.value
            repository.fetchRemoteModels(
                customEndpoint = s.customEndpointUrl,
                customApiKey = s.customApiKey,
                providerId = s.selectedProviderId
            )

            // Load or initialize default conversation
            val convs = activeConversations.first()
            if (convs.isNotEmpty()) {
                selectConversation(convs.first())
            } else {
                createNewChat()
            }
        }
    }

    fun fetchRemoteModels(customEndpoint: String = "", customApiKey: String = "", providerId: String = "aerolink") {
        viewModelScope.launch {
            val s = settings.value
            val endpoint = customEndpoint.ifBlank { s.customEndpointUrl }
            val key = customApiKey.ifBlank { s.customApiKey }
            val pid = providerId.ifBlank { s.selectedProviderId }
            val loaded = repository.fetchRemoteModels(endpoint, key, pid)
            if (loaded.isNotEmpty()) {
                if (!loaded.any { it.id == s.selectedModelId }) {
                    userPreferences.updateProviderAndModel(pid, loaded.first().id)
                }
            }
        }
    }

    fun onPromptChange(text: String) {
        _promptText.value = text
    }

    fun addAttachment(attachment: Attachment) {
        _attachments.value = _attachments.value + attachment
    }

    fun removeAttachment(attachment: Attachment) {
        _attachments.value = _attachments.value - attachment
    }

    fun selectFolder(folderId: String?) {
        _selectedFolderId.value = folderId
    }

    fun createFolder(name: String, colorHex: String, emoji: String) {
        viewModelScope.launch {
            val newFolder = ChatFolder(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                colorHex = colorHex,
                emoji = emoji
            )
            repository.insertFolder(newFolder)
            _selectedFolderId.value = newFolder.id
        }
    }

    fun updateFolder(folder: ChatFolder) {
        viewModelScope.launch {
            repository.updateFolder(folder)
        }
    }

    fun deleteFolder(folderId: String) {
        viewModelScope.launch {
            repository.deleteFolder(folderId)
            if (_selectedFolderId.value == folderId) {
                _selectedFolderId.value = null
            }
        }
    }

    fun assignConversationToFolder(conversationId: String, folderId: String?) {
        viewModelScope.launch {
            repository.updateConversationFolder(conversationId, folderId)
            // Update selected conversation in state if it's the current one
            if (_selectedConversation.value?.id == conversationId) {
                _selectedConversation.value = _selectedConversation.value?.copy(folderId = folderId)
            }
        }
    }

    fun selectConversation(conversation: Conversation) {
        messagesJob?.cancel()
        _messages.value = emptyList() // Immediately clear previous messages to prevent glimpses of old chats
        _selectedConversation.value = conversation
        if (conversation.folderId != null) {
            _selectedFolderId.value = conversation.folderId
        }
        messagesJob = viewModelScope.launch {
            repository.getMessagesForConversation(conversation.id).collect { msgList ->
                if (_selectedConversation.value?.id == conversation.id) {
                    _messages.value = msgList
                }
            }
        }
    }

    fun addMemory(category: String, content: String) {
        viewModelScope.launch {
            val memory = UserMemory(
                id = UUID.randomUUID().toString(),
                category = category,
                content = content,
                isEnabled = true,
                folderId = _selectedFolderId.value
            )
            repository.insertMemory(memory)
        }
    }

    fun addMemory(memory: UserMemory) {
        viewModelScope.launch {
            val memWithFolder = if (memory.folderId == null) memory.copy(folderId = _selectedFolderId.value) else memory
            repository.insertMemory(memWithFolder)
        }
    }

    fun toggleMemory(id: String, isEnabled: Boolean) {
        viewModelScope.launch {
            repository.updateMemoryState(id, isEnabled)
        }
    }

    fun deleteMemory(id: String) {
        viewModelScope.launch {
            repository.deleteMemory(id)
        }
    }

    fun updateMessageContent(messageId: String, newContent: String) {
        viewModelScope.launch {
            val msg = _messages.value.find { it.id == messageId }
            if (msg != null) {
                val updated = msg.copy(content = newContent)
                repository.insertMessage(updated)
            }
        }
    }

    fun speakMessage(messageId: String, text: String) {
        ttsHelper.speak(messageId, text)
    }

    fun stopTts() {
        ttsHelper.stop()
    }

    fun selectAccessPoint(ap: com.example.domain.models.AccessPoint) {
        userPreferences.selectAccessPoint(ap)
    }

    fun updateCurrentConversationModel(modelId: String, providerId: String) {
        val currentConv = _selectedConversation.value ?: return
        val updatedConv = currentConv.copy(modelId = modelId, providerId = providerId)
        _selectedConversation.value = updatedConv
        viewModelScope.launch {
            repository.updateConversationModel(currentConv.id, modelId, providerId)
        }
    }

    fun createNewChat() {
        val currentConv = _selectedConversation.value
        if (currentConv != null && _messages.value.isEmpty()) {
            if (currentConv.folderId != _selectedFolderId.value) {
                assignConversationToFolder(currentConv.id, _selectedFolderId.value)
            }
            return
        }

        messagesJob?.cancel()
        _selectedConversation.value = null
        _messages.value = emptyList()

        viewModelScope.launch {
            val activeList = repository.activeConversations.first()
            val emptyConv = activeList.firstOrNull { conv ->
                val msgs = repository.getMessagesForConversation(conv.id).first()
                msgs.isEmpty()
            }
            if (emptyConv != null) {
                if (emptyConv.folderId != _selectedFolderId.value) {
                    repository.updateConversationFolder(emptyConv.id, _selectedFolderId.value)
                }
                selectConversation(emptyConv)
                return@launch
            }

            val currentSettings = settings.value
            val newConv = repository.createNewConversation(
                title = "New Chat",
                modelId = currentSettings.selectedModelId,
                providerId = currentSettings.selectedProviderId,
                folderId = _selectedFolderId.value
            )
            selectConversation(newConv)
        }
    }

    fun exportBackup(onResult: (String) -> Unit) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            onResult(json)
        }
    }

    fun importBackup(jsonStr: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.importBackupJson(jsonStr)
            if (success) {
                val convs = activeConversations.first()
                if (convs.isNotEmpty()) {
                    selectConversation(convs.first())
                }
            }
            onResult(success)
        }
    }

    fun sendMessage() {
        val prompt = _promptText.value.trim()
        val currentConv = _selectedConversation.value ?: return
        if (prompt.isEmpty() && _attachments.value.isEmpty()) return

        val attachment = _attachments.value.firstOrNull()
        val userMsgId = UUID.randomUUID().toString()

        val imageUriStr = if (attachment?.type == com.example.domain.models.AttachmentType.IMAGE) attachment.uriString else null
        val (mimeType, base64Data) = if (imageUriStr != null) {
            com.example.utils.ImageUtils.readUriAsBase64(getApplication(), imageUriStr) ?: Pair("image/jpeg", "")
        } else {
            Pair("", "")
        }

        val userMessage = ChatMessage(
            id = userMsgId,
            conversationId = currentConv.id,
            role = "user",
            content = prompt,
            timestamp = System.currentTimeMillis(),
            imageUri = imageUriStr,
            attachmentName = attachment?.name,
            attachmentType = attachment?.type?.name
        )

        _promptText.value = ""
        _attachments.value = emptyList()

        viewModelScope.launch {
            repository.insertMessage(userMessage)
            extractUserMemoryFromPrompt(prompt)
            generateAiResponse(
                conversation = currentConv,
                prompt = prompt,
                imageBase64 = base64Data.ifEmpty { null },
                imageMimeType = mimeType.ifEmpty { null }
            )
        }
    }

    private fun extractUserMemoryFromPrompt(prompt: String) {
        val lower = prompt.lowercase()
        val triggers = listOf("remember that ", "remember: ", "remember ", "keep in mind that ", "save to memory: ", "note that ")
        for (tr in triggers) {
            if (lower.contains(tr)) {
                val idx = lower.indexOf(tr) + tr.length
                val fact = prompt.substring(idx).trim()
                if (fact.isNotBlank()) {
                    val key = if (fact.length > 30) fact.take(30) + "..." else fact
                    addMemory("User Fact: $key", fact)
                    break
                }
            }
        }
    }

    private fun generateAiResponse(
        conversation: Conversation,
        prompt: String,
        imageBase64: String? = null,
        imageMimeType: String? = null
    ) {
        val assistantMsgId = UUID.randomUUID().toString()
        val assistantPlaceholder = ChatMessage(
            id = assistantMsgId,
            conversationId = conversation.id,
            role = "assistant",
            content = "",
            timestamp = System.currentTimeMillis(),
            isStreaming = true
        )

        _isGenerating.value = true

        viewModelScope.launch {
            repository.insertMessage(assistantPlaceholder)

            val currentSettings = settings.value
            val memoryContext = repository.getActiveMemoryContext(conversation.folderId ?: _selectedFolderId.value)
            val fullSystemPrompt = currentSettings.systemPrompt + memoryContext
            val history = _messages.value.dropLast(1) // exclude placeholder
            val activeModelId = currentSettings.selectedModelId.ifBlank { conversation.modelId }

            val accumulatedContent = StringBuilder()

            var lastDbWriteTime = 0L

            currentStreamJob = launch {
                var isErrorOccurred = false
                var errorText = ""

                repository.streamAiResponse(
                    prompt = prompt,
                    history = history,
                    systemPrompt = fullSystemPrompt,
                    modelId = activeModelId,
                    customApiKey = currentSettings.customApiKey,
                    customEndpoint = currentSettings.customEndpointUrl,
                    imageBase64 = imageBase64,
                    imageMimeType = imageMimeType
                ).catch { e ->
                    isErrorOccurred = true
                    errorText = e.localizedMessage ?: "Unknown error occurred"
                }.collect { chunk ->
                    accumulatedContent.append(chunk)
                    val now = System.currentTimeMillis()
                    if (now - lastDbWriteTime > 120L) {
                        lastDbWriteTime = now
                        val updatedMsg = assistantPlaceholder.copy(
                            content = accumulatedContent.toString(),
                            isStreaming = true
                        )
                        repository.insertMessage(updatedMsg)
                    }
                }

                _isGenerating.value = false

                if (isErrorOccurred) {
                    val finalContent = if (accumulatedContent.isNotEmpty()) {
                        "${accumulatedContent}\n\n⚠️ **Error:** $errorText"
                    } else {
                        "⚠️ **Error:** $errorText"
                    }
                    val errorMsg = assistantPlaceholder.copy(
                        content = finalContent,
                        isStreaming = false,
                        isError = true
                    )
                    repository.insertMessage(errorMsg)
                } else {
                    val fullText = accumulatedContent.toString()
                    
                    // Parse any memory tags generated by AI: [MEMORY: key | value]
                    val memoryRegex = Regex("\\[MEMORY:\\s*([^|\\]=]+)[|=]([^\\]]+)\\]", RegexOption.IGNORE_CASE)
                    memoryRegex.findAll(fullText).forEach { match ->
                        val k = match.groupValues[1].trim()
                        val v = match.groupValues[2].trim()
                        if (k.isNotBlank() && v.isNotBlank()) {
                            addMemory(k, v)
                        }
                    }

                    val cleanedText = fullText.replace(memoryRegex, "").trim()
                    val finalMsg = assistantPlaceholder.copy(
                        content = if (cleanedText.isNotBlank()) cleanedText else fullText,
                        isStreaming = false,
                        tokenCount = fullText.length / 4
                    )
                    repository.insertMessage(finalMsg)
                }
            }
        }
    }

    fun stopGeneration() {
        currentStreamJob?.cancel()
        _isGenerating.value = false

        viewModelScope.launch {
            val lastMsg = _messages.value.lastOrNull()
            if (lastMsg != null && lastMsg.role == "assistant" && lastMsg.isStreaming) {
                val stoppedMsg = lastMsg.copy(
                    content = lastMsg.content.ifBlank { "Generation stopped." } + " *(stopped)*",
                    isStreaming = false
                )
                repository.insertMessage(stoppedMsg)
            }
        }
    }

    fun regenerateLastResponse() {
        val currentConv = _selectedConversation.value ?: return
        val lastUserMsg = _messages.value.lastOrNull { it.role == "user" } ?: return
        generateAiResponse(currentConv, lastUserMsg.content)
    }

    fun pinConversation(conversationId: String, isPinned: Boolean) {
        viewModelScope.launch {
            repository.setPinned(conversationId, isPinned)
        }
    }

    fun archiveConversation(conversationId: String, isArchived: Boolean) {
        viewModelScope.launch {
            repository.setArchived(conversationId, isArchived)
        }
    }

    fun deleteConversation(conversationId: String) {
        viewModelScope.launch {
            repository.deleteConversation(conversationId)
            if (_selectedConversation.value?.id == conversationId) {
                val remaining = activeConversations.value.filter { it.id != conversationId }
                if (remaining.isNotEmpty()) {
                    selectConversation(remaining.first())
                } else {
                    createNewChat()
                }
            }
        }
    }

    fun renameConversation(conversationId: String, newTitle: String) {
        viewModelScope.launch {
            repository.updateTitle(conversationId, newTitle)
            if (_selectedConversation.value?.id == conversationId) {
                _selectedConversation.value = _selectedConversation.value?.copy(title = newTitle)
            }
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearAll()
            createNewChat()
        }
    }

    override fun onCleared() {
        super.onCleared()
        ttsHelper.shutdown()
    }
}
