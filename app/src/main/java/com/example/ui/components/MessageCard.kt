package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
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

@Composable
fun MessageCard(
    message: ChatMessage,
    fontSizeSp: Float = 15f,
    speakingMessageId: String? = null,
    onRegenerate: () -> Unit = {},
    onEditPrompt: (String) -> Unit = {},
    onImageClick: (String) -> Unit = {},
    onSpeakClick: (String, String) -> Unit = { _, _ -> }
) {
    val context = LocalContext.current
    val isUser = message.role == "user"

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
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
            },
            tonalElevation = if (isUser) 0.dp else 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = if (isUser) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
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
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TerracottaPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Aether AI",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TerracottaPrimary
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

                // Content
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
                        fontSizeSp = fontSizeSp
                    )
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
                            Toast.makeText(context, "Message copied", Toast.LENGTH_SHORT).show()
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
