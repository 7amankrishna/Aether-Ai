package com.example.domain.models

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AiModel(
    val id: String,
    val name: String,
    val description: String,
    val providerId: String,
    val isDefault: Boolean = false,
    val isImageGenerator: Boolean = false
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
                AiModel("claude-3-5-sonnet-20241022", "Claude 3.5 Sonnet", "Flagship high-intelligence Claude model", "aerolink", isDefault = true),
                AiModel("claude-3-5-haiku-20241022", "Claude 3.5 Haiku", "Fast & light Claude model", "aerolink"),
                AiModel("claude-3-opus-20240229", "Claude 3 Opus", "Deep reasoning flagship model", "aerolink")
            )
        ),
        AiProvider(
            id = "openai",
            name = "OpenAI GPT",
            iconName = "openai",
            models = listOf(
                AiModel("gpt-4o", "GPT-4o", "Flagship omnimodal OpenAI model", "openai"),
                AiModel("gpt-4o-mini", "GPT-4o Mini", "Fast & economical GPT model", "openai"),
                AiModel("o1-mini", "o1 Mini", "Advanced reasoning model", "openai"),
                AiModel("o3-mini", "o3 Mini", "Next-gen code & STEM reasoning", "openai")
            )
        ),
        AiProvider(
            id = "gemini",
            name = "Google Gemini",
            iconName = "gemini",
            models = listOf(
                AiModel("gemini-1.5-pro", "Gemini 1.5 Pro", "Complex reasoning with 2M context window", "gemini"),
                AiModel("gemini-1.5-flash", "Gemini 1.5 Flash", "Lightning fast multimodal model", "gemini"),
                AiModel("gemini-2.0-flash-exp", "Gemini 2.0 Flash", "Next generation multimodal intelligence", "gemini")
            )
        ),
        AiProvider(
            id = "deepseek",
            name = "DeepSeek AI",
            iconName = "deepseek",
            models = listOf(
                AiModel("deepseek-chat", "DeepSeek V3", "High performance open weights conversational AI", "deepseek"),
                AiModel("deepseek-reasoner", "DeepSeek R1", "Advanced chain-of-thought reasoning AI", "deepseek")
            )
        ),
        AiProvider(
            id = "image_gen",
            name = "AI Image Generators",
            iconName = "image",
            models = listOf(
                AiModel("dall-e-3", "DALL-E 3", "HD Photorealistic & Artistic AI Image Generation", "image_gen", isImageGenerator = true),
                AiModel("flux-1-dev", "Flux.1 Dev", "Ultra high fidelity open image synthesis", "image_gen", isImageGenerator = true),
                AiModel("imagen-3", "Google Imagen 3", "State of the art visual art generation", "image_gen", isImageGenerator = true)
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
            val mergedModels = (dynamicModels + provider.models).distinctBy { it.id }
            current[index] = provider.copy(models = mergedModels)
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



