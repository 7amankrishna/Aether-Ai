package com.example.utils

import android.content.Context
import android.net.Uri
import android.util.Base64

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
}
