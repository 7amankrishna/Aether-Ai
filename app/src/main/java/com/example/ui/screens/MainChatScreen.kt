package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.models.ProviderRegistry
import com.example.ui.components.*
import com.example.ui.theme.TerracottaPrimary
import kotlinx.coroutines.launch

import androidx.compose.material.icons.outlined.Psychology

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainChatScreen(
    viewModel: ChatViewModel,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val activeConvs by viewModel.activeConversations.collectAsStateWithLifecycle()
    val archivedConvs by viewModel.archivedConversations.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val speakingMessageId by viewModel.ttsHelper.speakingMessageId.collectAsStateWithLifecycle()

    val selectedConv by viewModel.selectedConversation.collectAsStateWithLifecycle()
    val messages by viewModel.messages.collectAsStateWithLifecycle()
    val promptText by viewModel.promptText.collectAsStateWithLifecycle()
    val attachments by viewModel.attachments.collectAsStateWithLifecycle()
    val isGenerating by viewModel.isGenerating.collectAsStateWithLifecycle()
    val userSettings by viewModel.settings.collectAsStateWithLifecycle()

    var isSettingsOpen by remember { mutableStateOf(false) }
    var isAdminOpen by remember { mutableStateOf(false) }
    var isMemoryOpen by remember { mutableStateOf(false) }
    var previewImageUri by remember { mutableStateOf<String?>(null) }
    var isModelDropdownExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    val isUserAtBottom by remember {
        derivedStateOf {
            val visibleItems = listState.layoutInfo.visibleItemsInfo
            if (visibleItems.isEmpty() || messages.isEmpty()) true
            else {
                val lastVisible = visibleItems.last()
                val isLastItem = lastVisible.index == messages.size - 1
                if (!isLastItem) {
                    false
                } else {
                    val itemBottom = lastVisible.offset + lastVisible.size
                    val viewportBottom = listState.layoutInfo.viewportEndOffset
                    itemBottom <= viewportBottom + 120
                }
            }
        }
    }

    val isScrolledUp by remember {
        derivedStateOf {
            !isUserAtBottom
        }
    }

    var previousMessageCount by remember { mutableStateOf(0) }

    // Auto-scroll to bottom on new message or stream chunk
    LaunchedEffect(messages.size, messages.lastOrNull()?.content?.length) {
        if (messages.isNotEmpty()) {
            val targetIndex = messages.size - 1
            val isNewMessageAdded = messages.size != previousMessageCount
            previousMessageCount = messages.size

            if (isNewMessageAdded || isUserAtBottom) {
                val isLastStreaming = messages.lastOrNull()?.isStreaming == true
                if (isLastStreaming) {
                    listState.scrollToItem(targetIndex, scrollOffset = 100000)
                } else {
                    listState.animateScrollToItem(targetIndex, scrollOffset = 100000)
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ConversationDrawerContent(
                activeConversations = activeConvs,
                archivedConversations = archivedConvs,
                selectedConversationId = selectedConv?.id,
                onSelectConversation = { conv ->
                    viewModel.selectConversation(conv)
                    scope.launch { drawerState.close() }
                },
                onNewChat = {
                    viewModel.createNewChat()
                    scope.launch { drawerState.close() }
                },
                onPinConversation = { id, pinned -> viewModel.pinConversation(id, pinned) },
                onArchiveConversation = { id, archived -> viewModel.archiveConversation(id, archived) },
                onDeleteConversation = { id -> viewModel.deleteConversation(id) },
                onRenameConversation = { id, title -> viewModel.renameConversation(id, title) },
                onOpenSettings = {
                    scope.launch { drawerState.close() }
                    isSettingsOpen = true
                },
                onOpenAdmin = {
                    scope.launch { drawerState.close() }
                    isAdminOpen = true
                }
            )
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        // Header Model Selector Pill
                        Box {
                            val currentModel = remember(userSettings.selectedModelId) {
                                ProviderRegistry.getModel(userSettings.selectedModelId)
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                                    .clickable { isModelDropdownExpanded = true }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                                    .testTag("model_selector_pill"),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = TerracottaPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = currentModel.name,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Expand",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }

                            val providers by ProviderRegistry.providersState.collectAsState()

                            DropdownMenu(
                                expanded = isModelDropdownExpanded,
                                onDismissRequest = { isModelDropdownExpanded = false }
                            ) {
                                providers.forEach { provider ->
                                    DropdownMenuItem(
                                        text = { Text(provider.name, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = TerracottaPrimary) },
                                        onClick = {},
                                        enabled = false
                                    )
                                    provider.models.forEach { model ->
                                        DropdownMenuItem(
                                            text = { Text(model.name, fontSize = 13.sp) },
                                            onClick = {
                                                isModelDropdownExpanded = false
                                                viewModel.userPreferences.updateProviderAndModel(provider.id, model.id)
                                                viewModel.updateCurrentConversationModel(model.id, provider.id)
                                            }
                                        )
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(
                            onClick = { scope.launch { drawerState.open() } },
                            modifier = Modifier.testTag("open_drawer_button")
                        ) {
                            Icon(Icons.Default.Menu, contentDescription = "Open Drawer")
                        }
                    },
                    actions = {
                        IconButton(onClick = { isMemoryOpen = true }) {
                            BadgedBox(
                                badge = {
                                    if (memories.any { it.isEnabled }) {
                                        Badge(containerColor = TerracottaPrimary)
                                    }
                                }
                            ) {
                                Icon(
                                    Icons.Outlined.Psychology,
                                    contentDescription = "AI Memory & Context",
                                    tint = if (memories.isNotEmpty()) TerracottaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { isAdminOpen = true }) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel",
                                tint = if (userSettings.isAdminMode) TerracottaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { isSettingsOpen = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )
            },
            bottomBar = {
                PromptComposer(
                    promptText = promptText,
                    onPromptChange = { viewModel.onPromptChange(it) },
                    onSend = { viewModel.sendMessage() },
                    onStop = { viewModel.stopGeneration() },
                    isGenerating = isGenerating,
                    attachments = attachments,
                    onAddAttachment = { viewModel.addAttachment(it) },
                    onRemoveAttachment = { viewModel.removeAttachment(it) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                )
            },
            containerColor = MaterialTheme.colorScheme.background,
            modifier = modifier.fillMaxSize()
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (messages.isEmpty()) {
                    // Empty State Card
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(TerracottaPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TerracottaPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "What can I help with today?",
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Ask Aether AI anything, upload documents or code, or brainstorm ideas.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        // Interactive Starter Prompt Chips
                        val samplePrompts = listOf(
                            "💡 Brainstorm ideas for an Android app",
                            "💻 Code a Jetpack Compose screen",
                            "📷 Analyze an uploaded image or document",
                            "📝 Summarize long text into key takeaways"
                        )

                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth(0.92f)
                        ) {
                            samplePrompts.forEach { sample ->
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.onPromptChange(sample) }
                                ) {
                                    Text(
                                        text = sample,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)
                    ) {
                        items(messages, key = { it.id }) { msg ->
                            MessageCard(
                                message = msg,
                                fontSizeSp = userSettings.fontSizeSp,
                                speakingMessageId = speakingMessageId,
                                onRegenerate = { viewModel.regenerateLastResponse() },
                                onImageClick = { previewImageUri = it },
                                onSpeakClick = { id, text -> viewModel.speakMessage(id, text) }
                            )
                        }
                    }
                }

                // Jump to Bottom Floating Action Button
                AnimatedVisibility(
                    visible = isScrolledUp,
                    enter = fadeIn(),
                    exit = fadeOut(),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 12.dp)
                ) {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                if (messages.isNotEmpty()) {
                                    listState.animateScrollToItem(messages.size - 1, scrollOffset = 100000)
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = TerracottaPrimary,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(Icons.Default.ArrowDownward, contentDescription = "Jump to Bottom", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }

    // Dialogs
    if (isMemoryOpen) {
        MemoryDialog(
            memories = memories,
            onAddMemory = { viewModel.addMemory(it) },
            onToggleMemory = { id, enabled -> viewModel.toggleMemory(id, enabled) },
            onDeleteMemory = { id -> viewModel.deleteMemory(id) },
            onDismiss = { isMemoryOpen = false }
        )
    }

    if (!previewImageUri.isNullOrEmpty()) {
        FullScreenImageViewer(
            imageUri = previewImageUri,
            onDismiss = { previewImageUri = null }
        )
    }

    if (isSettingsOpen) {
        SettingsDialog(
            settings = userSettings,
            onUpdateTheme = { viewModel.userPreferences.updateThemeMode(it) },
            onUpdateProviderAndModel = { provider, model -> viewModel.userPreferences.updateProviderAndModel(provider, model) },
            onUpdateCustomApiKey = { viewModel.userPreferences.updateCustomApiKey(it) },
            onUpdateCustomEndpoint = { viewModel.userPreferences.updateCustomEndpoint(it) },
            onSelectAccessPoint = { viewModel.selectAccessPoint(it) },
            onFetchRemoteModels = { endpoint, key -> viewModel.fetchRemoteModels(endpoint, key) },
            onUpdateFontSize = { viewModel.userPreferences.updateFontSize(it) },
            onUpdateStreaming = { viewModel.userPreferences.updateStreamingEnabled(it) },
            onUpdateHaptics = { viewModel.userPreferences.updateHapticsEnabled(it) },
            onClearAllHistory = { viewModel.clearAllHistory() },
            onDismiss = { isSettingsOpen = false }
        )
    }

    if (isAdminOpen) {
        AdminPanelDialog(
            settings = userSettings,
            onUpdateAdminMode = { viewModel.userPreferences.updateAdminMode(it) },
            onUpdateTemperature = { viewModel.userPreferences.updateTemperature(it) },
            onUpdateSystemPrompt = { viewModel.userPreferences.updateSystemPrompt(it) },
            onDismiss = { isAdminOpen = false }
        )
    }
}
