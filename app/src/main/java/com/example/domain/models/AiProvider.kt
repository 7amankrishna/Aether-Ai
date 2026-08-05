package com.example.domain.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiModel(
    val id: String,
    val name: String,
    val description: String,
    val providerId: String,
    val isDefault: Boolean = false
)

data class AiProvider(
    val id: String,
    val name: String,
    val iconName: String,
    val models: List<AiModel>
)

object ProviderRegistry {
    private val defaultProviders = listOf(
        AiProvider(
            id = "aerolink",
            name = "Aerolink (Anthropic)",
            iconName = "anthropic",
            models = listOf(
                AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Flagship high-intelligence Claude model via Aerolink", "aerolink", isDefault = true),
                AiModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Fast & efficient Claude model via Aerolink", "aerolink"),
                AiModel("claude-3-opus-20240229", "Claude 3 Opus", "Deep reasoning Claude model via Aerolink", "aerolink")
            )
        )
    )

    private val _providersState = MutableStateFlow(defaultProviders)
    val providersState: StateFlow<List<AiProvider>> = _providersState.asStateFlow()

    val providers: List<AiProvider>
        get() = _providersState.value

    fun updateDynamicModels(providerId: String, dynamicModels: List<AiModel>) {
        if (dynamicModels.isEmpty()) return
        val current = _providersState.value.toMutableList()
        val index = current.indexOfFirst { it.id == providerId }
        if (index != -1) {
            val provider = current[index]
            current[index] = provider.copy(models = dynamicModels)
        } else {
            current.add(
                AiProvider(
                    id = providerId,
                    name = if (providerId == "aerolink") "Aerolink (Anthropic)" else providerId.replaceFirstChar { it.uppercase() },
                    iconName = if (providerId == "aerolink") "anthropic" else "custom",
                    models = dynamicModels
                )
            )
        }
        _providersState.value = current
    }

    fun getModel(modelId: String): AiModel {
        return providers.flatMap { it.models }.find { it.id == modelId }
            ?: providers.firstOrNull()?.models?.firstOrNull()
            ?: AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Aerolink Model", "aerolink", isDefault = true)
    }
}


