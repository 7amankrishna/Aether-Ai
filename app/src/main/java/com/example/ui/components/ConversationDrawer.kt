package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Settings
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
import com.example.domain.models.Conversation
import com.example.ui.theme.TerracottaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationDrawerContent(
    activeConversations: List<Conversation>,
    archivedConversations: List<Conversation>,
    selectedConversationId: String?,
    onSelectConversation: (Conversation) -> Unit,
    onNewChat: () -> Unit,
    onPinConversation: (String, Boolean) -> Unit,
    onArchiveConversation: (String, Boolean) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onRenameConversation: (String, String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0: All, 1: Pinned, 2: Archived

    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    val filteredList = remember(searchQuery, activeTab, activeConversations, archivedConversations) {
        val baseList = if (activeTab == 2) archivedConversations else activeConversations
        baseList.filter { conv ->
            val matchesTab = when (activeTab) {
                1 -> conv.isPinned
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() || conv.title.contains(searchQuery, ignoreCase = true)
            matchesTab && matchesQuery
        }
    }

    ModalDrawerSheet(
        modifier = modifier
            .width(320.dp)
            .fillMaxHeight(),
        drawerContainerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // Header Logo & Brand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(TerracottaPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Aether AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Intelligent Assistant",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("new_chat_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Chat", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...", fontSize = 13.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(16.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("search_conversations_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Pinned", "Archived").forEachIndexed { index, label ->
                    FilterChip(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        label = { Text(label, fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Conversation List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(filteredList, key = { it.id }) { conv ->
                    val isSelected = conv.id == selectedConversationId
                    var isMenuExpanded by remember { mutableStateOf(false) }

                    val formattedDate = remember(conv.updatedAt) {
                        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                        sdf.format(Date(conv.updatedAt))
                    }

                    Surface(
                        onClick = { onSelectConversation(conv) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) TerracottaPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation_item_${conv.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (conv.isPinned) Icons.Default.PushPin else Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = conv.title,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        color = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = formattedDate,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }

                            Box {
                                IconButton(
                                    onClick = { isMenuExpanded = true },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                DropdownMenu(
                                    expanded = isMenuExpanded,
                                    onDismissRequest = { isMenuExpanded = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text(if (conv.isPinned) "Unpin" else "Pin") },
                                        leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                                        onClick = {
                                            isMenuExpanded = false
                                            onPinConversation(conv.id, !conv.isPinned)
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Rename") },
                                        leadingIcon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                                        onClick = {
                                            isMenuExpanded = false
                                            conversationToRename = conv
                                            renameInputText = conv.title
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text(if (conv.isArchived) "Unarchive" else "Archive") },
                                        leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                                        onClick = {
                                            isMenuExpanded = false
                                            onArchiveConversation(conv.id, !conv.isArchived)
                                        }
                                    )
                                    HorizontalDivider()
                                    DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            isMenuExpanded = false
                                            onDeleteConversation(conv.id)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Footer Settings Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.testTag("open_settings_button")
                ) {
                    Icon(Icons.Outlined.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Settings", fontSize = 13.sp)
                }

                TextButton(
                    onClick = onOpenAdmin,
                    modifier = Modifier.testTag("open_admin_button")
                ) {
                    Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Admin", fontSize = 13.sp)
                }
            }
        }
    }

    // Rename Dialog
    if (conversationToRename != null) {
        AlertDialog(
            onDismissRequest = { conversationToRename = null },
            title = { Text("Rename Conversation") },
            text = {
                OutlinedTextField(
                    value = renameInputText,
                    onValueChange = { renameInputText = it },
                    singleLine = true,
                    label = { Text("Title") }
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        conversationToRename?.let { conv ->
                            if (renameInputText.isNotBlank()) {
                                onRenameConversation(conv.id, renameInputText.trim())
                            }
                        }
                        conversationToRename = null
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { conversationToRename = null }) { Text("Cancel") }
            }
        )
    }
}
