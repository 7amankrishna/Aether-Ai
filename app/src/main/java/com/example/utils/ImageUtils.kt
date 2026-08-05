package com.example.utils

import android.app.DownloadManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

object ImageUtils {
    fun readUriAsBase64(context: Context, uriString: String?): Pair<String, String>? {
        if (uriString.isNullOrEmpty()) return null
        return try {
            val uri = Uri.parse(uriString)
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val bytes = inputStream.readBytes()
            inputStream.close()
            if (bytes.isEmpty()) return null
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Pair(mimeType, base64)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveOrDownloadImage(context: Context, imageUrl: String) {
        withContext(Dispatchers.IO) {
            try {
                if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://")) {
                    val request = DownloadManager.Request(Uri.parse(imageUrl))
                        .setTitle("AI Generated Image")
                        .setDescription("Downloading image to Pictures folder...")
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "Aether_AI_${System.currentTimeMillis()}.png")
                        .setAllowedOverMetered(true)
                        .setAllowedOverRoaming(true)

                    val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                    downloadManager.enqueue(request)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Downloading image to Pictures folder...", Toast.LENGTH_SHORT).show()
                    }
                } else if (imageUrl.startsWith("data:image/")) {
                    val commaIndex = imageUrl.indexOf(",")
                    if (commaIndex != -1) {
                        val base64Data = imageUrl.substring(commaIndex + 1)
                        val bytes = Base64.decode(base64Data, Base64.DEFAULT)
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        if (bitmap != null) {
                            saveBitmapToGallery(context, bitmap)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        val shareIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, imageUrl)
                            type = "text/plain"
                        }
                        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to download image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        withContext(Dispatchers.IO) {
            val filename = "Aether_AI_${System.currentTimeMillis()}.png"
            var fos: OutputStream? = null

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                        put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    }
                    val imageUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    fos = imageUri?.let { resolver.openOutputStream(it) }
                } else {
                    val imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
                    val image = java.io.File(imagesDir, filename)
                    fos = java.io.FileOutputStream(image)
                }

                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }

                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved image to gallery!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to save bitmap: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}

