package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class AppThemeMode { SYSTEM, LIGHT, DARK }

data class UserSettings(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val fontSizeSp: Float = 15f,
    val isStreamingEnabled: Boolean = true,
    val isHapticsEnabled: Boolean = true,
    val selectedProviderId: String = "aerolink",
    val selectedModelId: String = "claude-3-5-sonnet-20241022",
    val customApiKey: String = "",
    val customEndpointUrl: String = "https://capi.aerolink.lat/",
    val isAdminMode: Boolean = false,
    val temperature: Float = 0.7f,
    val systemPrompt: String = "You are Aether AI, an exceptionally smart, elegant, helpful, and concise AI assistant with a friendly and polished tone."
)

class UserPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("aether_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<UserSettings> = _settings.asStateFlow()

    private fun loadSettings(): UserSettings {
        val themeOrdinal = prefs.getInt("theme_mode", AppThemeMode.SYSTEM.ordinal)
        return UserSettings(
            themeMode = AppThemeMode.values().getOrElse(themeOrdinal) { AppThemeMode.SYSTEM },
            fontSizeSp = prefs.getFloat("font_size", 15f),
            isStreamingEnabled = prefs.getBoolean("streaming_enabled", true),
            isHapticsEnabled = prefs.getBoolean("haptics_enabled", true),
            selectedProviderId = prefs.getString("provider_id", "aerolink") ?: "aerolink",
            selectedModelId = prefs.getString("model_id", "claude-3-5-sonnet-20241022") ?: "claude-3-5-sonnet-20241022",
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            customEndpointUrl = prefs.getString("custom_endpoint", "https://capi.aerolink.lat/") ?: "https://capi.aerolink.lat/",
            isAdminMode = prefs.getBoolean("is_admin", false),
            temperature = prefs.getFloat("temperature", 0.7f),
            systemPrompt = prefs.getString(
                "system_prompt",
                "You are Aether AI, an exceptionally smart, elegant, helpful, and concise AI assistant with a friendly and polished tone."
            ) ?: ""
        )
    }

    fun updateThemeMode(mode: AppThemeMode) {
        prefs.edit().putInt("theme_mode", mode.ordinal).apply()
        _settings.value = _settings.value.copy(themeMode = mode)
    }

    fun updateFontSize(sizeSp: Float) {
        prefs.edit().putFloat("font_size", sizeSp).apply()
        _settings.value = _settings.value.copy(fontSizeSp = sizeSp)
    }

    fun updateStreamingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("streaming_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isStreamingEnabled = enabled)
    }

    fun updateHapticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("haptics_enabled", enabled).apply()
        _settings.value = _settings.value.copy(isHapticsEnabled = enabled)
    }

    fun updateProviderAndModel(providerId: String, modelId: String) {
        prefs.edit().putString("provider_id", providerId).putString("model_id", modelId).apply()
        _settings.value = _settings.value.copy(selectedProviderId = providerId, selectedModelId = modelId)
    }

    fun updateCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key).apply()
        _settings.value = _settings.value.copy(customApiKey = key)
    }

    fun updateCustomEndpoint(url: String) {
        prefs.edit().putString("custom_endpoint", url).apply()
        _settings.value = _settings.value.copy(customEndpointUrl = url)
    }

    fun updateAdminMode(isAdmin: Boolean) {
        prefs.edit().putBoolean("is_admin", isAdmin).apply()
        _settings.value = _settings.value.copy(isAdminMode = isAdmin)
    }

    fun updateTemperature(temp: Float) {
        prefs.edit().putFloat("temperature", temp).apply()
        _settings.value = _settings.value.copy(temperature = temp)
    }

    fun updateSystemPrompt(prompt: String) {
        prefs.edit().putString("system_prompt", prompt).apply()
        _settings.value = _settings.value.copy(systemPrompt = prompt)
    }
}
