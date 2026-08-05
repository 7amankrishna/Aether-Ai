package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.domain.models.UserMemory
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaPrimary
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoryDialog(
    memories: List<UserMemory>,
    onAddMemory: (UserMemory) -> Unit,
    onToggleMemory: (String, Boolean) -> Unit,
    onDeleteMemory: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var newCategory by remember { mutableStateOf("User Preference") }
    var newContent by remember { mutableStateOf("") }
    var isAddingNew by remember { mutableStateOf(false) }

    val categories = listOf("User Preference", "Identity", "Project Context", "Custom Memory")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxHeight(0.85f)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(TerracottaContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Psychology,
                            contentDescription = "AI Memory",
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "AI Memory & Context",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Persistent memory shared across all model switches",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Row: Add Custom & Export Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { isAddingNew = !isAddingNew },
                        colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isAddingNew) "Cancel" else "Add Memory", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            if (memories.isEmpty()) {
                                Toast.makeText(context, "No memories stored to export.", Toast.LENGTH_SHORT).show()
                            } else {
                                val exportText = buildString {
                                    appendLine("# AI User Context Memory Export")
                                    appendLine("Exported at: ${java.util.Date()}\n")
                                    memories.forEach { mem ->
                                        val status = if (mem.isEnabled) "ACTIVE" else "INACTIVE"
                                        appendLine("• [$status] [${mem.category}]: ${mem.content}")
                                    }
                                }
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("AI Memory Context", exportText))
                                Toast.makeText(context, "Memory context exported & copied to clipboard!", Toast.LENGTH_LONG).show()
                            }
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Outlined.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export", fontSize = 12.sp)
                    }
                }

                // Add Memory Drawer Form
                AnimatedVisibility(visible = isAddingNew) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text("Category", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(vertical = 6.dp)
                        ) {
                            categories.take(2).forEach { cat ->
                                FilterChip(
                                    selected = newCategory == cat,
                                    onClick = { newCategory = cat },
                                    label = { Text(cat, fontSize = 11.sp) }
                                )
                            }
                        }

                        OutlinedTextField(
                            value = newContent,
                            onValueChange = { newContent = it },
                            placeholder = { Text("e.g. Always generate concise Kotlin code.", fontSize = 12.sp) },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                if (newContent.isNotBlank()) {
                                    onAddMemory(
                                        UserMemory(
                                            id = UUID.randomUUID().toString(),
                                            category = newCategory,
                                            content = newContent.trim(),
                                            isEnabled = true
                                        )
                                    )
                                    newContent = ""
                                    isAddingNew = false
                                    Toast.makeText(context, "Memory context saved!", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = newContent.isNotBlank(),
                            modifier = Modifier.align(Alignment.End),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("Save Context", fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Memories List
                if (memories.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Outlined.Psychology,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No persistent memory yet",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Add memory items above so the AI remembers your context across all models",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 24.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(memories, key = { it.id }) { mem ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = mem.category.uppercase(),
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TerracottaPrimary
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = mem.content,
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }

                                    Switch(
                                        checked = mem.isEnabled,
                                        onCheckedChange = { onToggleMemory(mem.id, it) },
                                        modifier = Modifier.padding(horizontal = 4.dp)
                                    )

                                    IconButton(
                                        onClick = { onDeleteMemory(mem.id) }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete Memory",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
