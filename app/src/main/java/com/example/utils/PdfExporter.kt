package com.example.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {
    suspend fun exportNoteToPdf(
        context: Context,
        title: String,
        content: String,
        modelName: String = "Aman.ai"
    ): Uri? {
        return withContext(Dispatchers.IO) {
            try {
                val pdfDocument = PdfDocument()
                val pageWidth = 595 // A4 width in points
                val pageHeight = 842 // A4 height in points
                val margin = 40
                var savedUri: Uri? = null
                val usableWidth = pageWidth - (margin * 2)

                var currentPageNum = 1
                var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                var page = pdfDocument.startPage(pageInfo)
                var canvas: Canvas = page.canvas

                // Paints
                val titlePaint = Paint().apply {
                    color = Color.rgb(204, 78, 52) // Terracotta Primary
                    textSize = 18f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    isAntiAlias = true
                }

                val subTitlePaint = Paint().apply {
                    color = Color.DKGRAY
                    textSize = 10f
                    typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
                    isAntiAlias = true
                }

                val bodyPaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    typeface = Typeface.DEFAULT
                    isAntiAlias = true
                }

                val mathTextPaint = Paint().apply {
                    color = Color.rgb(204, 78, 52)
                    textSize = 13f
                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                    isAntiAlias = true
                }

                val mathBoxPaint = Paint().apply {
                    color = Color.rgb(253, 238, 233) // Light terracotta fill
                    style = Paint.Style.FILL
                }

                val mathBorderPaint = Paint().apply {
                    color = Color.rgb(204, 78, 52)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                }

                val linePaint = Paint().apply {
                    color = Color.LTGRAY
                    strokeWidth = 1f
                }

                var yPos = margin.toFloat() + 20f

                // Draw Header
                val displayTitle = if (title.isBlank()) "Aman.ai Student Note" else title
                canvas.drawText(displayTitle, margin.toFloat(), yPos, titlePaint)
                yPos += 22f

                val timeStr = SimpleDateFormat("MMM dd, yyyy - hh:mm a", Locale.getDefault()).format(Date())
                canvas.drawText("Exported from $modelName • $timeStr", margin.toFloat(), yPos, subTitlePaint)
                yPos += 15f

                canvas.drawLine(margin.toFloat(), yPos, (pageWidth - margin).toFloat(), yPos, linePaint)
                yPos += 25f

                // Format lines from content
                val rawLines = content.replace("\r", "").split("\n")
                val lineHeight = 18f

                for (line in rawLines) {
                    if (line.isBlank()) {
                        yPos += lineHeight / 2
                        continue
                    }

                    // Check for Block Math $$...$$
                    val trimmed = line.trim()
                    if (trimmed.startsWith("$$") && trimmed.endsWith("$$") && trimmed.length > 4) {
                        val rawLatex = trimmed.removePrefix("$$").removeSuffix("$$")
                        val formattedMath = formatLatexToUnicode(rawLatex)

                        if (yPos + 40f > pageHeight - margin) {
                            pdfDocument.finishPage(page)
                            currentPageNum++
                            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                            page = pdfDocument.startPage(pageInfo)
                            canvas = page.canvas
                            yPos = margin.toFloat() + 20f
                        }

                        val rect = RectF(margin.toFloat(), yPos, (pageWidth - margin).toFloat(), yPos + 34f)
                        canvas.drawRoundRect(rect, 8f, 8f, mathBoxPaint)
                        canvas.drawRoundRect(rect, 8f, 8f, mathBorderPaint)

                        val mathWidth = mathTextPaint.measureText("Formula: $formattedMath")
                        val mathX = (pageWidth - mathWidth) / 2f
                        canvas.drawText("Formula: $formattedMath", mathX.coerceAtLeast(margin.toFloat() + 10f), yPos + 22f, mathTextPaint)
                        yPos += 44f
                        continue
                    }

                    // Process line with highlights / inline tags
                    val segments = parseLineSegments(line)
                    var xPos = margin.toFloat()

                    for (seg in segments) {
                        val text = seg.text
                        if (text.isEmpty()) continue

                        val words = text.split(" ")
                        for (i in words.indices) {
                            val word = if (i < words.size - 1) words[i] + " " else words[i]
                            val wordWidth = bodyPaint.measureText(word)

                            if (xPos + wordWidth > pageWidth - margin) {
                                xPos = margin.toFloat()
                                yPos += lineHeight
                                if (yPos > pageHeight - margin) {
                                    pdfDocument.finishPage(page)
                                    currentPageNum++
                                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNum).create()
                                    page = pdfDocument.startPage(pageInfo)
                                    canvas = page.canvas
                                    yPos = margin.toFloat() + 20f
                                }
                            }

                            if (seg.highlightColor != null) {
                                val hlPaint = Paint().apply {
                                    color = seg.highlightColor
                                    style = Paint.Style.FILL
                                }
                                val bgRect = RectF(xPos, yPos - 12f, xPos + wordWidth, yPos + 4f)
                                canvas.drawRect(bgRect, hlPaint)
                            }

                            if (seg.isMath) {
                                val mathInlinePaint = Paint(bodyPaint).apply {
                                    color = Color.rgb(204, 78, 52)
                                    typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
                                }
                                canvas.drawText(word, xPos, yPos, mathInlinePaint)
                            } else {
                                canvas.drawText(word, xPos, yPos, bodyPaint)
                            }

                            xPos += wordWidth
                        }
                    }
                    yPos += lineHeight
                }

                pdfDocument.finishPage(page)

                val timestamp = System.currentTimeMillis()
                val filename = "Aman_Note_$timestamp.pdf"
                var outputStream: OutputStream? = null

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOCUMENTS + "/AmanNotes")
                    }
                    val pdfUri = resolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
                    outputStream = pdfUri?.let { resolver.openOutputStream(it) }
                    savedUri = pdfUri
                } else {
                    val docsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "AmanNotes")
                    if (!docsDir.exists()) docsDir.mkdirs()
                    val pdfFile = File(docsDir, filename)
                    outputStream = FileOutputStream(pdfFile)
                    savedUri = Uri.fromFile(pdfFile)
                }

                outputStream?.use {
                    pdfDocument.writeTo(it)
                }
                pdfDocument.close()

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "📄 PDF Note saved to Documents/AmanNotes!", Toast.LENGTH_SHORT).show()
                }
                savedUri
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to export PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
                null
            }
        }
    }

    fun openDocumentFolder(context: Context, pdfUri: Uri?) {
        try {
            val folderUri = Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADocuments%2FAmanNotes")
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(folderUri, "vnd.android.document/directory")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e1: Exception) {
            try {
                if (pdfUri != null) {
                    val intent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(pdfUri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(Intent.createChooser(intent, "Open PDF Note"))
                } else {
                    val intent = Intent(DownloadManager.ACTION_VIEW_DOWNLOADS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                }
            } catch (e2: Exception) {
                Toast.makeText(context, "Document folder: Documents/AmanNotes", Toast.LENGTH_LONG).show()
            }
        }
    }

    private data class LineSegment(
        val text: String,
        val highlightColor: Int? = null,
        val isMath: Boolean = false
    )

    private fun parseLineSegments(line: String): List<LineSegment> {
        val list = mutableListOf<LineSegment>()
        var index = 0
        val regex = Regex("(<mark(?: color=\"(yellow|green|pink|cyan|orange)\")?>[\\s\\S]*?<\\/mark>|==[\\s\\S]*?==|\\$[^\$\\n]+\\$)")
        val matches = regex.findAll(line)

        for (match in matches) {
            if (match.range.first > index) {
                list.add(LineSegment(line.substring(index, match.range.first)))
            }

            val valStr = match.value
            when {
                valStr.startsWith("<mark") -> {
                    val colorName = Regex("color=\"(yellow|green|pink|cyan|orange)\"").find(valStr)?.groupValues?.get(1) ?: "yellow"
                    val hlColor = when (colorName.lowercase()) {
                        "green" -> Color.rgb(165, 214, 167)
                        "pink" -> Color.rgb(244, 143, 177)
                        "cyan" -> Color.rgb(128, 222, 234)
                        "orange" -> Color.rgb(255, 204, 128)
                        else -> Color.rgb(255, 241, 118)
                    }
                    val inner = valStr.replace(Regex("<mark[^>]*>"), "").replace("</mark>", "")
                    list.add(LineSegment(text = inner, highlightColor = hlColor))
                }
                valStr.startsWith("==") -> {
                    val inner = valStr.substring(2, valStr.length - 2)
                    list.add(LineSegment(text = inner, highlightColor = Color.rgb(255, 241, 118)))
                }
                valStr.startsWith("$") -> {
                    val innerMath = valStr.substring(1, valStr.length - 1)
                    val formatted = formatLatexToUnicode(innerMath)
                    list.add(LineSegment(text = " $formatted ", isMath = true))
                }
                else -> {
                    list.add(LineSegment(valStr))
                }
            }
            index = match.range.last + 1
        }

        if (index < line.length) {
            list.add(LineSegment(line.substring(index)))
        }

        return list
    }

    private fun formatLatexToUnicode(latex: String): String {
        return latex
            .replace("\\frac{", "(")
            .replace("}{", " / ")
            .replace("}", ")")
            .replace("\\sqrt{", "√(")
            .replace("\\sum", "∑")
            .replace("\\int", "∫")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\gamma", "γ")
            .replace("\\delta", "δ")
            .replace("\\theta", "θ")
            .replace("\\lambda", "λ")
            .replace("\\pi", "π")
            .replace("\\sigma", "σ")
            .replace("\\infty", "∞")
            .replace("\\pm", "±")
            .replace("\\neq", "≠")
            .replace("\\leq", "≤")
            .replace("\\geq", "≥")
            .replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\approx", "≈")
            .replace("\\cdot", "·")
            .replace("\\Delta", "Δ")
            .replace("^2", "²")
            .replace("^3", "³")
            .replace("^1", "¹")
            .replace("_0", "₀")
            .replace("_1", "₁")
            .replace("_2", "₂")
            .replace("_n", "ₙ")
            .replace("_x", "ₓ")
            .replace("\\text{", "")
            .replace("\\mathrm{", "")
            .replace("\\mathbf{", "")
            .trim()
    }
}
