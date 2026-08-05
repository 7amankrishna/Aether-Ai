package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.CodeBlockBackground
import com.example.ui.theme.CodeBlockHeader
import com.example.ui.theme.CodeBlockText
import com.example.ui.theme.TerracottaPrimary
import com.example.utils.ImageUtils
import kotlinx.coroutines.launch

sealed class MarkdownBlock {
    data class Paragraph(val text: String) : MarkdownBlock()
    data class Heading(val level: Int, val text: String) : MarkdownBlock()
    data class CodeBlock(val language: String, val code: String) : MarkdownBlock()
    data class Blockquote(val text: String) : MarkdownBlock()
    data class ListItem(val text: String, val isNumbered: Boolean = false, val number: Int = 1) : MarkdownBlock()
    data class TableBlock(val headers: List<String>, val rows: List<List<String>>) : MarkdownBlock()
    data class MathBlock(val latex: String) : MarkdownBlock()
    data class ImageBlock(val alt: String, val imageUrl: String) : MarkdownBlock()
}

@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    fontSizeSp: Float = 15f,
    onImageClick: (String) -> Unit = {}
) {
    val blocks = remember(markdown) {
        try {
            parseMarkdown(markdown)
        } catch (e: Exception) {
            listOf(MarkdownBlock.Paragraph(markdown))
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        blocks.forEach { block ->
            when (block) {
                is MarkdownBlock.Heading -> {
                    val size = when (block.level) {
                        1 -> (fontSizeSp * 1.35f).sp
                        2 -> (fontSizeSp * 1.2f).sp
                        else -> (fontSizeSp * 1.1f).sp
                    }
                    Text(
                        text = block.text,
                        fontSize = size,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(top = 6.dp, bottom = 2.dp)
                    )
                }
                is MarkdownBlock.Paragraph -> {
                    FormattedRichText(block.text, fontSizeSp = fontSizeSp)
                }
                is MarkdownBlock.CodeBlock -> {
                    CodeBlockCard(block.language, block.code)
                }
                is MarkdownBlock.ImageBlock -> {
                    GeneratedImageCard(alt = block.alt, imageUrl = block.imageUrl, onImageClick = onImageClick)
                }
                is MarkdownBlock.Blockquote -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(36.dp)
                                .background(TerracottaPrimary, RoundedCornerShape(2.dp))
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        FormattedRichText(
                            text = block.text,
                            fontSizeSp = fontSizeSp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                is MarkdownBlock.ListItem -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 4.dp, top = 2.dp, bottom = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = if (block.isNumbered) "${block.number}. " else "• ",
                            fontWeight = FontWeight.Bold,
                            fontSize = fontSizeSp.sp,
                            color = TerracottaPrimary
                        )
                        FormattedRichText(text = block.text, fontSizeSp = fontSizeSp)
                    }
                }
                is MarkdownBlock.TableBlock -> {
                    TableDisplay(block.headers, block.rows, fontSizeSp)
                }
                is MarkdownBlock.MathBlock -> {
                    MathDisplay(block.latex)
                }
            }
        }
    }
}

@Composable
fun FormattedRichText(
    text: String,
    fontSizeSp: Float,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    val annotated = buildAnnotatedString {
        var index = 0
        val regex = Regex("(\\$\\$[\\s\\S]*?\\$\\$|\\$[^\$]+\\$|```[\\s\\S]*?```|`[^`]+`|\\*\\*.*?\\*\\*|\\*.*?\\*|_.*?_)")
        val matches = regex.findAll(text)

        for (match in matches) {
            if (match.range.first > index) {
                append(text.substring(index, match.range.first))
            }

            val value = match.value
            when {
                value.startsWith("**") && value.endsWith("**") && value.length >= 4 -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(value.substring(2, value.length - 2))
                    }
                }
                ((value.startsWith("*") && value.endsWith("*")) || (value.startsWith("_") && value.endsWith("_"))) && value.length >= 2 -> {
                    withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                        append(value.substring(1, value.length - 1))
                    }
                }
                value.startsWith("`") && value.endsWith("`") && value.length >= 2 -> {
                    withStyle(
                        SpanStyle(
                            fontFamily = FontFamily.Monospace,
                            background = MaterialTheme.colorScheme.surfaceVariant,
                            color = TerracottaPrimary,
                            fontSize = (fontSizeSp * 0.9f).sp
                        )
                    ) {
                        append(value.substring(1, value.length - 1))
                    }
                }
                else -> {
                    append(value)
                }
            }
            index = match.range.last + 1
        }

        if (index < text.length) {
            append(text.substring(index))
        }
    }

    Text(
        text = annotated,
        fontSize = fontSizeSp.sp,
        lineHeight = (fontSizeSp * 1.45f).sp,
        color = color
    )
}

@Composable
fun CodeBlockCard(language: String, code: String) {
    val context = LocalContext.current
    var isCopied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CodeBlockBackground)
            .border(1.dp, Color(0xFF332E2A), RoundedCornerShape(12.dp))
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(CodeBlockHeader)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" }.lowercase(),
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFD9C5B2)
            )

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .clickable {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Code", code))
                        isCopied = true
                        Toast.makeText(context, "Code copied", Toast.LENGTH_SHORT).show()
                    }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isCopied) Icons.Default.Done else Icons.Default.ContentCopy,
                    contentDescription = "Copy code",
                    tint = if (isCopied) Color(0xFF81C784) else Color(0xFFB8A99A),
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCopied) "Copied!" else "Copy",
                    fontSize = 11.sp,
                    color = if (isCopied) Color(0xFF81C784) else Color(0xFFB8A99A)
                )
            }
        }

        // Code Area
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        ) {
            Text(
                text = code.trimEnd(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = CodeBlockText
            )
        }
    }
}

@Composable
fun TableDisplay(headers: List<String>, rows: List<List<String>>, fontSizeSp: Float) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        // Header Row
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp)
        ) {
            headers.forEach { header ->
                Text(
                    text = header.trim(),
                    fontWeight = FontWeight.Bold,
                    fontSize = (fontSizeSp * 0.9f).sp,
                    modifier = Modifier.width(120.dp).padding(horizontal = 4.dp)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        // Data Rows
        rows.forEach { row ->
            Row(modifier = Modifier.padding(8.dp)) {
                row.forEachIndexed { idx, cell ->
                    val width = if (idx < headers.size) 120.dp else 100.dp
                    Text(
                        text = cell.trim(),
                        fontSize = (fontSizeSp * 0.88f).sp,
                        modifier = Modifier.width(width).padding(horizontal = 4.dp)
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        }
    }
}

@Composable
fun MathDisplay(latex: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = latex,
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Medium,
            fontSize = 16.sp,
            color = TerracottaPrimary
        )
    }
}

@Composable
fun GeneratedImageCard(
    alt: String,
    imageUrl: String,
    onImageClick: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isSaving by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .border(1.dp, TerracottaPrimary.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            AsyncImage(
                model = imageUrl,
                contentDescription = alt.ifBlank { "AI Generated Image" },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onImageClick(imageUrl) },
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = alt.ifBlank { "AI Generated Image" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        isSaving = true
                        scope.launch {
                            ImageUtils.saveOrDownloadImage(context, imageUrl)
                            isSaving = false
                        }
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = TerracottaPrimary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Download Image",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isSaving) "Saving..." else "Download",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

fun parseMarkdown(text: String): List<MarkdownBlock> {
    val blocks = mutableListOf<MarkdownBlock>()
    val lines = text.lines()
    var idx = 0

    while (idx < lines.size) {
        val line = lines[idx]

        // Image detection ![alt](url)
        val imageMatch = Regex("!\\[(.*?)\\]\\((.*?)\\)").find(line.trim())
        if (imageMatch != null) {
            val alt = imageMatch.groupValues[1]
            val imageUrl = imageMatch.groupValues[2]
            blocks.add(MarkdownBlock.ImageBlock(alt, imageUrl))
            idx++
            continue
        }

        // Code block
        if (line.trim().startsWith("```")) {
            val language = line.trim().removePrefix("```").trim()
            val codeSb = StringBuilder()
            idx++
            while (idx < lines.size && !lines[idx].trim().startsWith("```")) {
                codeSb.append(lines[idx]).append("\n")
                idx++
            }
            blocks.add(MarkdownBlock.CodeBlock(language, codeSb.toString()))
            idx++
            continue
        }

        // Table detection
        if (line.contains("|") && idx + 1 < lines.size && lines[idx + 1].contains("---")) {
            val headers = line.split("|").filter { it.isNotBlank() }
            val rows = mutableListOf<List<String>>()
            idx += 2 // Skip header and separator
            while (idx < lines.size && lines[idx].contains("|")) {
                val rowCells = lines[idx].split("|").filter { it.isNotBlank() }
                rows.add(rowCells)
                idx++
            }
            blocks.add(MarkdownBlock.TableBlock(headers, rows))
            continue
        }

        // Math block $$ ... $$
        if (line.trim().startsWith("$$") && line.trim().endsWith("$$") && line.trim().length > 4) {
            val latex = line.trim().removePrefix("$$").removeSuffix("$$").trim()
            blocks.add(MarkdownBlock.MathBlock(latex))
            idx++
            continue
        }

        // Headings
        if (line.startsWith("# ")) {
            blocks.add(MarkdownBlock.Heading(1, line.removePrefix("# ").trim()))
            idx++
            continue
        } else if (line.startsWith("## ")) {
            blocks.add(MarkdownBlock.Heading(2, line.removePrefix("## ").trim()))
            idx++
            continue
        } else if (line.startsWith("### ")) {
            blocks.add(MarkdownBlock.Heading(3, line.removePrefix("### ").trim()))
            idx++
            continue
        }

        // Blockquotes
        if (line.startsWith("> ")) {
            blocks.add(MarkdownBlock.Blockquote(line.removePrefix("> ").trim()))
            idx++
            continue
        }

        // List items
        if (line.trim().startsWith("- ") || line.trim().startsWith("* ")) {
            blocks.add(MarkdownBlock.ListItem(line.trim().substring(2).trim()))
            idx++
            continue
        }

        // Numbered list items
        val numberedMatch = Regex("^(\\d+)\\.\\s+(.*)").find(line.trim())
        if (numberedMatch != null) {
            val num = numberedMatch.groupValues[1].toIntOrNull() ?: 1
            val itemText = numberedMatch.groupValues[2]
            blocks.add(MarkdownBlock.ListItem(itemText, isNumbered = true, number = num))
            idx++
            continue
        }

        // Regular paragraph
        if (line.isNotBlank()) {
            blocks.add(MarkdownBlock.Paragraph(line.trim()))
        }
        idx++
    }

    return blocks
}
