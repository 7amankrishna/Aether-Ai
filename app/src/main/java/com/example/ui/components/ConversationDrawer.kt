package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
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
import com.example.domain.models.ChatFolder
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
    folders: List<ChatFolder> = emptyList(),
    selectedFolderId: String? = null,
    onSelectFolder: (String?) -> Unit = {},
    onCreateFolder: (String, String, String) -> Unit = { _, _, _ -> },
    onDeleteFolder: (String) -> Unit = {},
    onAssignConversationToFolder: (String, String?) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var activeTab by remember { mutableIntStateOf(0) } // 0: All, 1: Pinned, 2: Archived

    var conversationToRename by remember { mutableStateOf<Conversation?>(null) }
    var renameInputText by remember { mutableStateOf("") }

    var showCreateFolderDialog by remember { mutableStateOf(false) }
    var folderToDelete by remember { mutableStateOf<ChatFolder?>(null) }
    var conversationToMove by remember { mutableStateOf<Conversation?>(null) }

    val filteredList = remember(searchQuery, activeTab, selectedFolderId, activeConversations, archivedConversations) {
        val baseList = if (activeTab == 2) archivedConversations else activeConversations
        baseList.filter { conv ->
            val matchesFolder = if (selectedFolderId == null) true else conv.folderId == selectedFolderId
            val matchesTab = when (activeTab) {
                1 -> conv.isPinned
                else -> true
            }
            val matchesQuery = searchQuery.isBlank() || conv.title.contains(searchQuery, ignoreCase = true)
            matchesFolder && matchesTab && matchesQuery
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
                .padding(12.dp)
        ) {
            // Header Logo & Brand
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(TerracottaPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "App Logo",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Aether AI",
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Intelligent Assistant",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // New Chat Button
            Button(
                onClick = onNewChat,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("new_chat_button"),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Chat", tint = Color.White, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("New Chat", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Folders / Projects Section Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.FolderOpen,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "FOLDERS & PROJECTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { showCreateFolderDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "New Folder",
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Folder Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    val isSelected = selectedFolderId == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSelectFolder(null) },
                        label = { Text("🌐 All Chats", fontSize = 12.sp) },
                        leadingIcon = if (isSelected) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                        } else null
                    )
                }

                items(folders, key = { it.id }) { folder ->
                    val isSelected = selectedFolderId == folder.id
                    val folderColor = try {
                        Color(android.graphics.Color.parseColor(folder.colorHex))
                    } catch (e: Exception) {
                        TerracottaPrimary
                    }

                    Box {
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectFolder(folder.id) },
                            label = { Text("${folder.emoji} ${folder.name}", fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Delete Folder",
                                    modifier = Modifier
                                        .size(14.dp)
                                        .clickable { folderToDelete = folder },
                                    tint = if (isSelected) folderColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = folderColor.copy(alpha = 0.25f),
                                selectedLabelColor = MaterialTheme.colorScheme.onSurface
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = folderColor,
                                selectedBorderColor = folderColor,
                                borderWidth = if (isSelected) 2.dp else 1.dp
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search conversations...", fontSize = 12.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", modifier = Modifier.size(14.dp))
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp)
                    .testTag("search_conversations_input"),
                shape = RoundedCornerShape(10.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Filter Tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("All", "Pinned", "Archived").forEachIndexed { index, label ->
                    FilterChip(
                        selected = activeTab == index,
                        onClick = { activeTab = index },
                        label = { Text(label, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Conversation List
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(filteredList, key = { it.id }) { conv ->
                    val isSelected = conv.id == selectedConversationId
                    var isMenuExpanded by remember { mutableStateOf(false) }

                    val convFolder = folders.find { it.id == conv.folderId }

                    val formattedDate = remember(conv.updatedAt) {
                        val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
                        sdf.format(Date(conv.updatedAt))
                    }

                    Surface(
                        onClick = { onSelectConversation(conv) },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) TerracottaPrimary.copy(alpha = 0.15f) else Color.Transparent,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("conversation_item_${conv.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (conv.isPinned) {
                                    Icon(
                                        imageVector = Icons.Default.PushPin,
                                        contentDescription = "Pinned",
                                        tint = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                }

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = conv.title,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            maxLines = 1,
                                            color = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.weight(1f, fill = false)
                                        )

                                        if (convFolder != null) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            val badgeColor = try {
                                                Color(android.graphics.Color.parseColor(convFolder.colorHex))
                                            } catch (e: Exception) {
                                                TerracottaPrimary
                                            }
                                            Surface(
                                                color = badgeColor.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(4.dp),
                                                border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.5f))
                                            ) {
                                                Text(
                                                    text = "${convFolder.emoji} ${convFolder.name}",
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }

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
                                        text = { Text("Move to Folder...") },
                                        leadingIcon = { Icon(Icons.Outlined.Folder, contentDescription = null) },
                                        onClick = {
                                            isMenuExpanded = false
                                            conversationToMove = conv
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

    // Move to Folder Dialog
    if (conversationToMove != null) {
        val targetConv = conversationToMove!!
        AlertDialog(
            onDismissRequest = { conversationToMove = null },
            title = { Text("Move '${targetConv.title}' to Folder") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        onClick = {
                            onAssignConversationToFolder(targetConv.id, null)
                            conversationToMove = null
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (targetConv.folderId == null) TerracottaPrimary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("🌐 General (No Folder)", fontSize = 14.sp)
                        }
                    }

                    folders.forEach { f ->
                        val fColor = try {
                            Color(android.graphics.Color.parseColor(f.colorHex))
                        } catch (e: Exception) {
                            TerracottaPrimary
                        }
                        Surface(
                            onClick = {
                                onAssignConversationToFolder(targetConv.id, f.id)
                                conversationToMove = null
                            },
                            shape = RoundedCornerShape(8.dp),
                            color = if (targetConv.folderId == f.id) fColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, fColor.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text("${f.emoji} ${f.name}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { conversationToMove = null }) { Text("Cancel") }
            }
        )
    }

    // Delete Folder Confirmation Dialog
    folderToDelete?.let { targetFolder ->
        AlertDialog(
            onDismissRequest = { folderToDelete = null },
            title = { Text("Delete Project / Folder?") },
            text = { Text("Are you sure you want to delete '${targetFolder.emoji} ${targetFolder.name}'? Conversations in this project will not be deleted.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteFolder(targetFolder.id)
                        folderToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { folderToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Create Folder Dialog
    if (showCreateFolderDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateFolderDialog = false },
            onCreate = { name, colorHex, emoji ->
                onCreateFolder(name, colorHex, emoji)
                showCreateFolderDialog = false
            }
        )
    }
}

@Composable
fun CreateFolderDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String, String) -> Unit
) {
    var folderName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#FF7043") } // default Terracotta
    var selectedEmoji by remember { mutableStateOf("📁") }

    val colors = listOf(
        "#FF7043", // Terracotta / Coral
        "#42A5F5", // Sky Blue
        "#66BB6A", // Emerald Green
        "#AB47BC", // Vibrant Purple
        "#FFA726", // Warm Amber
        "#EC407A", // Pink
        "#26A69A", // Teal
        "#78909C"  // Slate Gray
    )

    val emojis = listOf("📁", "🚀", "💡", "📚", "🎨", "💼", "🔬", "🤖", "📝", "🧠", "⚡", "🎯", "💻", "📈", "🛠️")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.FolderOpen, contentDescription = null, tint = TerracottaPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Project Folder", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = folderName,
                    onValueChange = { folderName = it },
                    label = { Text("Folder / Project Name") },
                    placeholder = { Text("e.g. Science Research, Marketing Campaign") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Choose Icon Emoji:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(emojis) { em ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selectedEmoji == em) TerracottaPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .border(
                                    width = if (selectedEmoji == em) 2.dp else 0.dp,
                                    color = if (selectedEmoji == em) TerracottaPrimary else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .clickable { selectedEmoji = em },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(em, fontSize = 18.sp)
                        }
                    }
                }

                Text("Choose Theme Color:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colors.forEach { hex ->
                        val parsedColor = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (e: Exception) {
                            TerracottaPrimary
                        }
                        val isSelected = selectedColor == hex

                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(parsedColor)
                                .border(
                                    width = if (isSelected) 3.dp else 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (folderName.isNotBlank()) {
                        onCreate(folderName.trim(), selectedColor, selectedEmoji)
                    }
                },
                enabled = folderName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary)
            ) {
                Text("Create Folder")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
