package com.example.data.remote

import com.example.BuildConfig
import com.example.domain.models.AiModel
import com.example.domain.models.ProviderRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

class GeminiApiClient {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun fetchRemoteModels(
        customEndpoint: String = "",
        customApiKey: String = "",
        providerId: String = "aerolink"
    ): List<AiModel> = withContext(Dispatchers.IO) {
        val endpoint = customEndpoint.ifBlank { BuildConfig.ANTHROPIC_BASE_URL.ifBlank { "https://capi.aerolink.lat/" } }
            .trimEnd('/') + "/"
        val apiKey = customApiKey.ifBlank { BuildConfig.ANTHROPIC_API_KEY.ifBlank { BuildConfig.GEMINI_API_KEY } }

        val urlsToTry = listOf("${endpoint}v1/models", "${endpoint}models")
        val fetchedModels = mutableListOf<AiModel>()

        for (url in urlsToTry) {
            try {
                val requestBuilder = Request.Builder()
                    .url(url)
                    .get()
                    .addHeader("Accept", "application/json")

                if (apiKey.isNotBlank() && apiKey != "YOUR_AEROLINK_API_KEY" && apiKey != "MY_GEMINI_API_KEY") {
                    requestBuilder.addHeader("x-api-key", apiKey)
                    requestBuilder.addHeader("Authorization", "Bearer $apiKey")
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                if (response.isSuccessful && response.body != null) {
                    val responseStr = response.body!!.string()
                    val modelsArray = try {
                        val jsonObj = JSONObject(responseStr)
                        jsonObj.optJSONArray("data") ?: jsonObj.optJSONArray("models")
                    } catch (e: Exception) {
                        try { JSONArray(responseStr) } catch (e2: Exception) { null }
                    }

                    if (modelsArray != null && modelsArray.length() > 0) {
                        for (i in 0 until modelsArray.length()) {
                            val item = modelsArray.optJSONObject(i)
                            val modelId = item?.optString("id") ?: item?.optString("name") ?: ""
                            if (modelId.isNotBlank()) {
                                val displayName = item?.optString("display_name")?.takeIf { it.isNotBlank() }
                                    ?: item?.optString("name")?.takeIf { it.isNotBlank() }
                                    ?: modelId
                                val owner = item?.optString("owned_by")?.let { " ($it)" } ?: ""
                                fetchedModels.add(
                                    AiModel(
                                        id = modelId,
                                        name = displayName,
                                        description = "Loaded dynamically from endpoint$owner",
                                        providerId = providerId,
                                        isDefault = (i == 0)
                                    )
                                )
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore and try next endpoint candidate
            }
            if (fetchedModels.isNotEmpty()) break
        }

        if (fetchedModels.isNotEmpty()) {
            ProviderRegistry.updateDynamicModels(providerId, fetchedModels)
        }
        return@withContext fetchedModels
    }

    fun streamGenerateContent(
        prompt: String,
        history: List<Pair<String, String>> = emptyList(),
        systemPrompt: String = "",
        modelId: String = "claude-3-5-sonnet-20241022",
        customApiKey: String = "",
        customEndpoint: String = "",
        imageBase64: String? = null,
        imageMimeType: String? = null
    ): Flow<String> = flow {
        val apiKey = customApiKey.ifBlank { BuildConfig.ANTHROPIC_API_KEY.ifBlank { BuildConfig.GEMINI_API_KEY } }
        val endpointUrl = customEndpoint.ifBlank { BuildConfig.ANTHROPIC_BASE_URL.ifBlank { "https://capi.aerolink.lat/" } }
            .trimEnd('/') + "/"

        val isRealApiKey = apiKey.isNotBlank() && apiKey != "YOUR_AEROLINK_API_KEY" && apiKey != "MY_GEMINI_API_KEY"

        var lastErrorMsg: String? = null

        if (isRealApiKey) {
            var isStreamSuccess = false

            // Try Anthropic / Aerolink Messages endpoint first if not explicitly gemini
            if (!modelId.startsWith("gemini")) {
                try {
                    val url = "${endpointUrl}v1/messages"
                    val messagesArray = JSONArray()

                    history.takeLast(10).forEach { (role, text) ->
                        val item = JSONObject().apply {
                            put("role", if (role == "assistant") "assistant" else "user")
                            put("content", text)
                        }
                        messagesArray.put(item)
                    }

                    val userMessageObj = JSONObject().apply {
                        put("role", "user")
                        if (!imageBase64.isNullOrEmpty()) {
                            val contentArray = JSONArray()
                            contentArray.put(JSONObject().apply {
                                put("type", "image")
                                put("source", JSONObject().apply {
                                    put("type", "base64")
                                    put("media_type", imageMimeType ?: "image/jpeg")
                                    put("data", imageBase64)
                                })
                            })
                            contentArray.put(JSONObject().apply {
                                put("type", "text")
                                put("text", prompt.ifBlank { "Analyze this image in detail." })
                            })
                            put("content", contentArray)
                        } else {
                            put("content", prompt)
                        }
                    }
                    messagesArray.put(userMessageObj)

                    val requestJson = JSONObject().apply {
                        put("model", modelId)
                        put("max_tokens", 8192)
                        if (systemPrompt.isNotBlank()) put("system", systemPrompt)
                        put("messages", messagesArray)
                        put("stream", true)
                    }

                    val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                    val request = Request.Builder()
                        .url(url)
                        .post(body)
                        .addHeader("x-api-key", apiKey)
                        .addHeader("Authorization", "Bearer $apiKey")
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Accept", "text/event-stream")
                        .build()

                    val response = okHttpClient.newCall(request).execute()
                    response.use { resp ->
                        if (resp.isSuccessful && resp.body != null) {
                            val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
                            var line: String?
                            while (reader.readLine().also { line = it } != null) {
                                val currentLine = line?.trim() ?: continue
                                if (currentLine.startsWith("data:")) {
                                    val jsonStr = currentLine.removePrefix("data:").trim()
                                    if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                                        try {
                                            val json = JSONObject(jsonStr)
                                            val deltaObj = json.optJSONObject("delta")
                                            val text = deltaObj?.optString("text")
                                                ?: json.optString("text", "")
                                            if (text.isNotEmpty()) {
                                                emit(text)
                                                isStreamSuccess = true
                                            }
                                        } catch (e: Exception) {
                                            // Ignore parse line error
                                        }
                                    }
                                }
                            }
                        } else {
                            lastErrorMsg = "HTTP ${resp.code}: ${resp.message.ifBlank { "Request failed" }}"
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMsg = e.localizedMessage ?: "Network request failed"
                    isStreamSuccess = false
                }

                // If Anthropic format failed, try OpenAI chat completions endpoint format
                if (!isStreamSuccess) {
                    try {
                        val url = "${endpointUrl}v1/chat/completions"
                        val messagesArray = JSONArray()
                        if (systemPrompt.isNotBlank()) {
                            messagesArray.put(JSONObject().apply {
                                put("role", "system")
                                put("content", systemPrompt)
                            })
                        }
                        history.takeLast(10).forEach { (role, text) ->
                            messagesArray.put(JSONObject().apply {
                                put("role", if (role == "assistant") "assistant" else "user")
                                put("content", text)
                            })
                        }

                        val userMessageObj = JSONObject().apply {
                            put("role", "user")
                            if (!imageBase64.isNullOrEmpty()) {
                                val contentArray = JSONArray()
                                contentArray.put(JSONObject().apply {
                                    put("type", "image_url")
                                    put("image_url", JSONObject().apply {
                                        put("url", "data:${imageMimeType ?: "image/jpeg"};base64,$imageBase64")
                                    })
                                })
                                contentArray.put(JSONObject().apply {
                                    put("type", "text")
                                    put("text", prompt.ifBlank { "Analyze this image in detail." })
                                })
                                put("content", contentArray)
                            } else {
                                put("content", prompt)
                            }
                        }
                        messagesArray.put(userMessageObj)

                        val requestJson = JSONObject().apply {
                            put("model", modelId)
                            put("max_tokens", 8192)
                            put("messages", messagesArray)
                            put("stream", true)
                        }

                        val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                        val request = Request.Builder()
                            .url(url)
                            .post(body)
                            .addHeader("Authorization", "Bearer $apiKey")
                            .addHeader("x-api-key", apiKey)
                            .addHeader("Accept", "text/event-stream")
                            .build()

                        val response = okHttpClient.newCall(request).execute()
                        response.use { resp ->
                            if (resp.isSuccessful && resp.body != null) {
                                val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
                                var line: String?
                                while (reader.readLine().also { line = it } != null) {
                                    val currentLine = line?.trim() ?: continue
                                    if (currentLine.startsWith("data:")) {
                                        val jsonStr = currentLine.removePrefix("data:").trim()
                                        if (jsonStr.isNotBlank() && jsonStr != "[DONE]") {
                                            try {
                                                val json = JSONObject(jsonStr)
                                                val choices = json.optJSONArray("choices")
                                                if (choices != null && choices.length() > 0) {
                                                    val delta = choices.getJSONObject(0).optJSONObject("delta")
                                                    val content = delta?.optString("content", "") ?: ""
                                                    if (content.isNotEmpty()) {
                                                        emit(content)
                                                        isStreamSuccess = true
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                // Ignore line parse exception
                                            }
                                        }
                                    }
                                }
                            } else {
                                lastErrorMsg = "HTTP ${resp.code}: ${resp.message.ifBlank { "Request failed" }}"
                            }
                        }
                    } catch (e: Exception) {
                        lastErrorMsg = e.localizedMessage ?: "Network request failed"
                        isStreamSuccess = false
                    }
                }
            } else {
                // Gemini API streaming call
                try {
                    val effectiveModel = if (modelId.contains("pro")) "gemini-3.1-pro-preview" else "gemini-3.5-flash"
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/$effectiveModel:streamGenerateContent?alt=sse&key=$apiKey"

                    val contentsArray = JSONArray()
                    history.takeLast(10).forEach { (role, text) ->
                        val geminiRole = if (role == "assistant") "model" else "user"
                        val contentObj = JSONObject().apply {
                            put("role", geminiRole)
                            put("parts", JSONArray().put(JSONObject().put("text", text)))
                        }
                        contentsArray.put(contentObj)
                    }
                    val partsArray = JSONArray()
                    if (!imageBase64.isNullOrEmpty()) {
                        partsArray.put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", imageMimeType ?: "image/jpeg")
                                put("data", imageBase64)
                            })
                        })
                    }
                    partsArray.put(JSONObject().put("text", prompt.ifBlank { "Analyze this image in detail." }))

                    contentsArray.put(JSONObject().apply {
                        put("role", "user")
                        put("parts", partsArray)
                    })

                    val requestJson = JSONObject().apply {
                        put("contents", contentsArray)
                        if (systemPrompt.isNotBlank()) {
                            put("systemInstruction", JSONObject().apply {
                                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
                            })
                        }
                    }

                    val body = requestJson.toString().toRequestBody("application/json".toMediaType())
                    val httpRequest = Request.Builder()
                        .url(url)
                        .post(body)
                        .build()

                    val response = okHttpClient.newCall(httpRequest).execute()

                    response.use { resp ->
                        if (resp.isSuccessful && resp.body != null) {
                            val reader = BufferedReader(InputStreamReader(resp.body!!.byteStream()))
                            var line: String?

                            while (reader.readLine().also { line = it } != null) {
                                val currentLine = line?.trim() ?: continue
                                if (currentLine.startsWith("data:")) {
                                    val jsonString = currentLine.removePrefix("data:").trim()
                                    if (jsonString.isNotBlank() && jsonString != "[DONE]") {
                                        try {
                                            val json = JSONObject(jsonString)
                                            val candidates = json.optJSONArray("candidates")
                                            if (candidates != null && candidates.length() > 0) {
                                                val cand = candidates.getJSONObject(0)
                                                val content = cand.optJSONObject("content")
                                                val parts = content?.optJSONArray("parts")
                                                if (parts != null && parts.length() > 0) {
                                                    val text = parts.getJSONObject(0).optString("text", "")
                                                    if (text.isNotEmpty()) {
                                                        emit(text)
                                                        isStreamSuccess = true
                                                    }
                                                }
                                            }
                                        } catch (e: Exception) {
                                            // Ignore line parse exception
                                        }
                                    }
                                }
                            }
                        } else {
                            lastErrorMsg = "HTTP ${resp.code}: ${resp.message.ifBlank { "Request failed" }}"
                        }
                    }
                } catch (e: Exception) {
                    lastErrorMsg = e.localizedMessage ?: "Network request failed"
                    isStreamSuccess = false
                }
            }

            if (isStreamSuccess) return@flow

            throw Exception(lastErrorMsg ?: "Model '$modelId' is currently unavailable or unreachable at $endpointUrl. Please check your API key, endpoint, or model selection.")
        } else {
            throw Exception("No valid API Key is configured for model '$modelId'. Please open Settings to set your API Key.")
        }
    }.flowOn(Dispatchers.IO)

    private fun generateSmartResponse(prompt: String, modelId: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("hello") || lower.contains("hi") || lower.contains("hey") -> {
                "Hello! I am **Aman.ai**, powered by model `$modelId`.\n\nHow can I help you today? You can ask me to:\n- 📝 Write or edit text & code\n- 📊 Analyze complex ideas & data\n- 💡 Brainstorm creative projects\n- 🧮 Solve mathematical equations"
            }
            lower.contains("code") || lower.contains("kotlin") || lower.contains("function") || lower.contains("example") -> {
                "Here is a complete, production-ready example in **Kotlin** demonstrating reactive state management with `StateFlow`:\n\n```kotlin\npackage com.aman.ai.example\n\nimport kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n\nclass SmartStateEngine {\n    private val _dataState = MutableStateFlow<List<String>>(emptyList())\n    val dataState: StateFlow<List<String>> = _dataState.asStateFlow()\n\n    fun addItem(newItem: String) {\n        _dataState.value = _dataState.value + newItem\n    }\n}\n```\n\n### Key Highlights\n1. **Encapsulation**: Private `MutableStateFlow` with public `StateFlow` exposure.\n2. **Thread Safety**: Atomic state updates on background coroutine dispatchers.\n3. **Compose Ready**: Easily collect in Jetpack Compose using `collectAsStateWithLifecycle()`."
            }
            lower.contains("math") || lower.contains("equation") || lower.contains("formula") || lower.contains("latex") -> {
                "Here is the mathematical formulation for the **Gaussian Distribution**:\n\n$$ f(x) = \\frac{1}{\\sigma \\sqrt{2\\pi}} e^{-\\frac{1}{2}\\left(\\frac{x-\\mu}{\\sigma}\\right)^2} $$\n\nWhere:\n- $\\mu$ represents the mean (expected value)\n- $\\sigma$ represents the standard deviation\n- $\\sigma^2$ is the variance"
            }
            lower.contains("table") || lower.contains("compare") -> {
                "Here is a comparison table of modern AI models:\n\n| Model | Reasoning | Speed | Context Window |\n| :--- | :---: | :---: | :---: |\n| **Claude 3.5 Sonnet** | High | Ultra Fast | 200,000 tokens |\n| **Claude 3.5 Haiku** | High | Very Fast | 200,000 tokens |\n| **Gemini 3.5 Flash** | High | Ultra Fast | 1,000,000 tokens |\n| **DeepSeek R1** | Step-by-Step | Fast | 128,000 tokens |"
            }
            else -> {
                "Thank you for your message regarding: **\"$prompt\"**.\n\nAs your **Aman.ai** assistant (running with model `$modelId`), I've processed your prompt carefully.\n\n### Core Insights\n- **Precision**: Synthesized response adhering strictly to your request bounds.\n- **Structure**: Clean markdown output formatted with typography hierarchy and visual spacing.\n\n> *Tip*: You can update API key or endpoint in Settings to automatically fetch remote models from https://capi.aerolink.lat/."
            }
        }
    }
}

