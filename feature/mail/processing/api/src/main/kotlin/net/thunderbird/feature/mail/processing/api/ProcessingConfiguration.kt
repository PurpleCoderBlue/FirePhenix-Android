package net.thunderbird.feature.mail.processing.api

import kotlinx.serialization.Serializable

@Serializable
enum class ProcessingAction { TRANSLATE, SUMMARIZE }

@Serializable
enum class ProcessingProvider(val label: String, val endpoint: String) {
    QWEN("Qwen", "https://maas.qianwenaiapi.com/compatible-mode/v1"),
    OPENAI("OpenAI", "https://api.openai.com/v1"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com/v1"),
    CUSTOM("OpenAI compatible", ""),
    ANTHROPIC("Anthropic / Claude", "https://api.anthropic.com/v1"),
    MICROSOFT("Microsoft Translator", "https://api.cognitive.microsofttranslator.com"),
    GOOGLE("Google Cloud Translation", "https://translation.googleapis.com/language/translate/v2"),
}

@Serializable
data class ServiceProfile(
    val id: String,
    val name: String,
    val action: ProcessingAction,
    val provider: ProcessingProvider = ProcessingProvider.QWEN,
    val endpoint: String = provider.endpoint,
    val model: String = "",
    val region: String = "",
    val parameters: Map<String, String> = emptyMap(),
    val revision: String = "",
)

@Serializable
data class ProcessingRule(
    val id: String,
    val name: String,
    val action: ProcessingAction,
    val profileId: String,
    val enabled: Boolean = true,
    val matchAll: Boolean = true,
    val accountId: String = "",
    val folder: String = "",
    val sender: String = "",
    val subject: String = "",
    val sourceLanguage: String = "",
    val model: String = "",
    val targetLanguage: String = "zh-Hans",
)

@Serializable
data class RemoteImageRule(val id: String, val sender: String, val enabled: Boolean = true)

@Serializable
enum class SenderListKind { AUTO_TRANSLATE, AUTO_SUMMARIZE, AUTO_IMAGES, BLOCK_IMAGES }

@Serializable
data class SenderListEntry(val sender: String, val kind: SenderListKind, val enabled: Boolean = true)

enum class RemoteImageAccess { MANUAL, AUTOMATIC, BLOCKED }

@Serializable
data class ProcessingConfiguration(
    val profiles: List<ServiceProfile> = emptyList(),
    val rules: List<ProcessingRule> = emptyList(),
    val automaticTranslation: Boolean = false,
    val automaticSummary: Boolean = false,
    val defaultTranslationProfile: String = "",
    val defaultSummaryProfile: String = "",
    val remoteImageRules: List<RemoteImageRule> = emptyList(),
    val senderLists: List<SenderListEntry> = emptyList(),
    val senderListsVersion: Int = 0,

)

data class ProcessingMessage(
    val accountId: String,
    val messageId: String,
    val folder: String,
    val sender: String,
    val subject: String,
    val html: String,
    val isSpam: Boolean,
    val isComplete: Boolean = true,
    val sourceLanguage: String? = null,
)
