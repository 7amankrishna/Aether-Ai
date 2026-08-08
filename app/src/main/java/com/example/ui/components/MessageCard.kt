package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.models.ChatMessage
import com.example.ui.theme.TerracottaContainer
import com.example.ui.theme.TerracottaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.outlined.VolumeUp

import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.utils.PdfExporter
import kotlinx.coroutines.launch

@Composable
fun MessageCard(
    message: ChatMessage,
    fontSizeSp: Float = 15f,
    speakingMessageId: String? = null,
    isDrawerOpen: Boolean = false,
    onRegenerate: () -> Unit = {},
    onEditPrompt: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onSpeakClick: (String, String) -> Unit = { _, _ -> },
    onSaveToMemory: (String, String) -> Unit = { _, _ -> },
    onUpdateContent: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isUser = message.role == "user"
    var showHighlightDialog by remember { mutableStateOf(false) }
    var isPdfExported by remember { mutableStateOf(false) }
    var exportedPdfUri by remember { mutableStateOf<Uri?>(null) }

    val formattedTime = remember(message.timestamp) {
        val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
        sdf.format(Date(message.timestamp))
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 2.dp)
            .testTag("message_card_${message.id}"),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start
    ) {
        // Main Message Container utilizing full width
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (isUser) {
                TerracottaPrimary
            } else if (message.isError) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else if (message.isError) MaterialTheme.colorScheme.error.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                    shape = RoundedCornerShape(16.dp)
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header tag for assistant message (raw full width)
                if (!isUser) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (message.isError) Icons.Default.ErrorOutline else Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = if (message.isError) MaterialTheme.colorScheme.error else TerracottaPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (message.isError) "Aman.ai — Generation Error" else "Aman.ai",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (message.isError) MaterialTheme.colorScheme.error else TerracottaPrimary
                        )
                    }
                }

                // Thinking / Loading details animation when streaming
                if (!isUser && message.isStreaming) {
                    ThinkingIndicatorCard(
                        hasContent = message.content.isNotEmpty()
                    )
                }

                // Attachment preview if any
                if (!message.imageUri.isNullOrEmpty()) {
                    val uri = message.imageUri
                    AsyncImage(
                        model = uri,
                        contentDescription = "Attachment Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onImageClick(uri) }
                            .padding(bottom = 8.dp),
                        contentScale = ContentScale.Crop
                    )
                }

                if (!message.attachmentName.isNullOrEmpty() && message.imageUri.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier
                            .padding(bottom = 8.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.InsertDriveFile,
                            contentDescription = "Attachment",
                            tint = if (isUser) Color.White else TerracottaPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = message.attachmentName ?: "Attachment",
                            fontSize = 12.sp,
                            color = if (isUser) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Content wrapped in SelectionContainer only when drawer is closed so selection handles do not leak over drawer
                if (isDrawerOpen) {
                    if (isUser) {
                        Text(
                            text = message.content,
                            fontSize = fontSizeSp.sp,
                            lineHeight = (fontSizeSp * 1.4f).sp,
                            color = Color.White
                        )
                    } else if (message.content.isNotEmpty()) {
                        MarkdownText(
                            markdown = message.content,
                            fontSizeSp = fontSizeSp,
                            onImageClick = onImageClick
                        )
                    }
                } else {
                    SelectionContainer {
                        if (isUser) {
                            Text(
                                text = message.content,
                                fontSize = fontSizeSp.sp,
                                lineHeight = (fontSizeSp * 1.4f).sp,
                                color = Color.White
                            )
                        } else if (message.content.isNotEmpty()) {
                            MarkdownText(
                                markdown = message.content,
                                fontSizeSp = fontSizeSp,
                                onImageClick = onImageClick
                            )
                        }
                    }
                }
            }
        }

        // Action footer
        Row(
            modifier = Modifier
                .padding(top = 4.dp, start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = formattedTime,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )

            if (isUser) {
                // Copy User Prompt Button
                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy User Prompt",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(15.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Prompt", message.content))
                            Toast.makeText(context, "Prompt copied to clipboard!", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("copy_prompt_button")
                )

                // Reuse / Edit Prompt Button
                Icon(
                    imageVector = Icons.Default.EditNote,
                    contentDescription = "Reuse / Edit Query",
                    tint = TerracottaPrimary,
                    modifier = Modifier
                        .size(18.dp)
                        .clickable {
                            onEditPrompt(message.content)
                            Toast.makeText(context, "Loaded query into message bar", Toast.LENGTH_SHORT).show()
                        }
                        .testTag("reuse_prompt_button")
                )
            }

            if (!isUser && !message.isStreaming && message.content.isNotEmpty()) {
                val isSpeakingThis = speakingMessageId == message.id

                Icon(
                    imageVector = if (isSpeakingThis) Icons.Default.VolumeUp else Icons.Outlined.VolumeUp,
                    contentDescription = "Read Aloud",
                    tint = if (isSpeakingThis) TerracottaPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable { onSpeakClick(message.id, message.content) }
                )

                Icon(
                    imageVector = Icons.Outlined.ContentCopy,
                    contentDescription = "Copy Message",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(15.dp)
                        .clickable {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Message", message.content))
                            Toast.makeText(context, "Message copied to clipboard", Toast.LENGTH_SHORT).show()
                        }
                )

                // Export to PDF Note
                Icon(
                    imageVector = Icons.Default.PictureAsPdf,
                    contentDescription = "Export PDF Note",
                    tint = TerracottaPrimary,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            scope.launch {
                                val firstLine = message.content.lines().firstOrNull { it.isNotBlank() } ?: "Study Note"
                                val title = firstLine.take(40).removePrefix("#").trim()
                                val uri = PdfExporter.exportNoteToPdf(
                                    context = context,
                                    title = title,
                                    content = message.content,
                                    modelName = "Aman.ai Note"
                                )
                                if (uri != null) {
                                    isPdfExported = true
                                    exportedPdfUri = uri
                                }
                            }
                        }
                )

                // Appears ONLY IF user exports the PDF
                if (isPdfExported) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(TerracottaPrimary.copy(alpha = 0.12f))
                            .clickable {
                                PdfExporter.openDocumentFolder(context, exportedPdfUri)
                            }
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FolderOpen,
                            contentDescription = "Open Document Path",
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "Open Folder",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TerracottaPrimary
                        )
                    }
                }

                // Save Note to AI Memory
                Icon(
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = "Save to Notes & Memory",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(16.dp)
                        .clickable {
                            val key = "Note ${SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date())}"
                            onSaveToMemory(key, message.content)
                            Toast.makeText(context, "Saved note to AI Memory!", Toast.LENGTH_SHORT).show()
                        }
                )


                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share Message",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(15.dp)
                        .clickable {
                            val sendIntent = Intent().apply {
                                action = Intent.ACTION_SEND
                                putExtra(Intent.EXTRA_TEXT, message.content)
                                type = "text/plain"
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Share message via"))
                        }
                )

                Icon(
                    imageVector = Icons.Outlined.Refresh,
                    contentDescription = "Regenerate Response",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(15.dp)
                        .clickable { onRegenerate() }
                )
            }
        }
    }

    if (showHighlightDialog) {
        HighlightReviewDialog(
            initialContent = message.content,
            onDismiss = { showHighlightDialog = false },
            onSave = { updatedText ->
                onUpdateContent(message.id, updatedText)
                showHighlightDialog = false
                Toast.makeText(context, "Highlights saved to message!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun ThinkingIndicatorCard(
    hasContent: Boolean = false
) {
    var isExpanded by remember { mutableStateOf(true) }
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = TerracottaContainer.copy(alpha = 0.25f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp)
            .border(
                width = 1.dp,
                color = TerracottaPrimary.copy(alpha = alpha * 0.5f),
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isExpanded = !isExpanded }
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = TerracottaPrimary.copy(alpha = alpha),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (hasContent) "Thinking process complete" else "Thinking & reasoning...",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
                )
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle Thinking",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 8.dp, start = 24.dp)) {
                    Text(
                        text = if (hasContent) "✓ Formulated markdown output, code blocks, and context" else "• Processing query and streaming detailed response step-by-step...",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        lineHeight = 15.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorDots() {
    val infiniteTransition = rememberInfiniteTransition(label = "dots")
    val dot1Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, delayMillis = 400, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TerracottaPrimary.copy(alpha = dot1Alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TerracottaPrimary.copy(alpha = dot2Alpha))
        )
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(TerracottaPrimary.copy(alpha = dot3Alpha))
        )
    }
}

@Composable
fun HighlightReviewDialog(
    initialContent: String,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit
) {
    var contentText by remember { mutableStateOf(initialContent) }
    var selectedColor by remember { mutableStateOf("yellow") } // yellow, green, pink, cyan, orange

    val colorsMap = listOf(
        "yellow" to (Color(0xFFFFF176) to "Yellow"),
        "green" to (Color(0xFFA5D6A7) to "Green"),
        "pink" to (Color(0xFFF48FB1) to "Pink"),
        "cyan" to (Color(0xFF80DEEA) to "Blue"),
        "orange" to (Color(0xFFFFCC80) to "Orange")
    )

    // Split content by lines / sentences so ALL lines can be selected and highlighted!
    val lines = remember(initialContent) {
        initialContent
            .split("\n")
            .filter { it.isNotBlank() }
            .ifEmpty { listOf(initialContent) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FormatColorFill,
                        contentDescription = null,
                        tint = TerracottaPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Highlight Chat Text", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }

                TextButton(
                    onClick = {
                        contentText = contentText
                            .replace(Regex("<mark[^>]*>"), "")
                            .replace("</mark>", "")
                    }
                ) {
                    Text("Reset", fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 460.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 5 Color Palette
                Text(
                    text = "Select Color:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    colorsMap.forEach { (key, colorPair) ->
                        val (colorVal, label) = colorPair
                        val isSelected = selectedColor == key
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.clickable { selectedColor = key }
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(colorVal)
                                    .border(
                                        width = if (isSelected) 3.dp else 1.dp,
                                        color = if (isSelected) TerracottaPrimary else Color.Gray.copy(alpha = 0.5f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = label,
                                        tint = Color.Black,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) TerracottaPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                HorizontalDivider()

                Text(
                    text = "Tap any line below to highlight or remove highlight:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Scrollable list of ALL lines in the message
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(8.dp)
                        )
                        .padding(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(lines.size) { index ->
                        val lineText = lines[index].trim()
                        val isLineHighlighted = contentText.contains(lineText) && contentText.contains("<mark")
                        val activeColor = colorsMap.find { it.first == selectedColor }?.second?.first ?: Color(0xFFFFF176)

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isLineHighlighted) activeColor.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surface
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (isLineHighlighted) activeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val regex = Regex("<mark[^>]*>(${Regex.escape(lineText)})</mark>")
                                    if (contentText.contains(regex)) {
                                        // Remove highlight from this line
                                        contentText = contentText.replace(regex, "$1")
                                    } else {
                                        // Highlight line in selected color
                                        val tagged = "<mark color=\"$selectedColor\">$lineText</mark>"
                                        contentText = if (contentText.contains(lineText)) {
                                            contentText.replace(lineText, tagged)
                                        } else {
                                            contentText + "\n" + tagged
                                        }
                                    }
                                }
                        ) {
                            Text(
                                text = lineText,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Live Preview:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Box(
                    modifier = Modifier
                        .height(80.dp)
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f), RoundedCornerShape(8.dp))
                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    MarkdownText(
                        markdown = contentText,
                        fontSizeSp = 12f
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(contentText) },
                colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Apply Highlights", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun SelectMessageToHighlightDialog(
    messages: List<ChatMessage>,
    onSelectMessage: (ChatMessage) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FormatColorFill,
                    contentDescription = null,
                    tint = TerracottaPrimary,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Select Message to Highlight", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tap any message in this conversation to apply multi-color highlights:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(messages.size) { index ->
                        val msg = messages[index]
                        val isUser = msg.role == "user"
                        val hasHighlight = msg.content.contains("<mark")

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isUser) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            ),
                            border = BorderStroke(
                                1.dp,
                                if (hasHighlight) TerracottaPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectMessage(msg) }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (isUser) "👤 User Prompt" else "🤖 AI Response",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isUser) TerracottaPrimary else MaterialTheme.colorScheme.primary
                                    )

                                    if (hasHighlight) {
                                        Surface(
                                            color = Color(0xFFFFF176),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = "Highlighted",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.Black,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                val cleanText = msg.content
                                    .replace(Regex("<mark[^>]*>"), "")
                                    .replace("</mark>", "")
                                    .trim()

                                Text(
                                    text = cleanText.take(90) + if (cleanText.length > 90) "..." else "",
                                    fontSize = 12.sp,
                                    maxLines = 2,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
