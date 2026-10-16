package net.thunderbird.feature.mail.processing.internal.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingProvider
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import org.junit.Test

class ProcessingProtocolTest {
    private val testSubject = ProcessingProtocol()
    private val profile = ServiceProfile("service", "Test", ProcessingAction.TRANSLATE, model = "user-selected")

    @Test
    fun `Qwen uses the exact user endpoint and Chat Completions without fallback`() {
        // Arrange / Act
        val request = testSubject.request(profile, "fake-test-key", listOf("Hello"), "zh-Hans")
        // Assert
        assertThat(request.url).isEqualTo("https://maas.qianwenaiapi.com/compatible-mode/v1/chat/completions")
        assertThat(request.headers["Authorization"]).isEqualTo("Bearer fake-test-key")
        assertThat(
            Json.parseToJsonElement(request.body).jsonObject.getValue("model").jsonPrimitive.content,
        ).isEqualTo("user-selected")
    }

    @Test
    fun `Qwen hybrid mail translation disables thinking unless the user overrides it`() {
        val hybrid = profile.copy(model = "qwen3.8-flash")
        val defaultBody = Json.parseToJsonElement(
            testSubject.request(hybrid, "fake", listOf("Hello"), "zh-Hans").body,
        ).jsonObject
        val explicitBody = Json.parseToJsonElement(
            testSubject.request(
                hybrid.copy(parameters = mapOf("enable_thinking" to "true")),
                "fake",
                listOf("Hello"),
                "zh-Hans",
            ).body,
        ).jsonObject
        assertThat(defaultBody["enable_thinking"]?.jsonPrimitive?.content).isEqualTo("false")
        assertThat(explicitBody["enable_thinking"]?.jsonPrimitive?.content).isEqualTo("true")
    }

    @Test
    fun `Anthropic uses Messages and its own authentication and system field`() {
        // Arrange
        val config =
            profile.copy(provider = ProcessingProvider.ANTHROPIC, endpoint = ProcessingProvider.ANTHROPIC.endpoint)
        // Act
        val request = testSubject.request(config, "fake-test-key", listOf("Hello"), "zh-Hans")
        val body = Json.parseToJsonElement(request.body).jsonObject
        // Assert
        assertThat(request.url).isEqualTo("https://api.anthropic.com/v1/messages")
        assertThat(request.headers["anthropic-version"]).isEqualTo("2023-06-01")
        assertThat(request.headers.containsKey("Authorization")).isEqualTo(false)
        assertThat(body.containsKey("system")).isTrue()
        assertThat(body.getValue("messages").jsonArray.size).isEqualTo(1)
    }

    @Test
    fun `traditional translators use official protocols with automatic source detection`() {
        // Arrange
        val microsoft = profile.copy(
            provider = ProcessingProvider.MICROSOFT,
            endpoint = ProcessingProvider.MICROSOFT.endpoint,
            region = "eastasia",
        )
        val google = profile.copy(provider = ProcessingProvider.GOOGLE, endpoint = ProcessingProvider.GOOGLE.endpoint)
        // Act
        val azure = testSubject.request(microsoft, "fake", listOf("Hello"), "zh-Hans")
        val cloud = testSubject.request(google, "fake", listOf("Hello"), "zh-Hans")
        // Assert
        assertThat(
            azure.url,
        ).isEqualTo("https://api.cognitive.microsofttranslator.com/translate?api-version=3.0&to=zh-Hans")
        assertThat(azure.headers["Ocp-Apim-Subscription-Region"]).isEqualTo("eastasia")
        assertThat(cloud.url).isEqualTo("https://translation.googleapis.com/language/translate/v2")
        assertThat(cloud.headers["X-Goog-Api-Key"]).isEqualTo("fake")
        assertThat(
            Json.parseToJsonElement(cloud.body).jsonObject.getValue("target").jsonPrimitive.content,
        ).isEqualTo("zh-CN")
    }

    @Test
    fun `truncated model output and missing translation items are failures`() {
        // Arrange
        val truncated = """{"choices":[{"finish_reason":"length","message":{"content":"partial"}}]}"""
        val missing = """{"choices":[{"finish_reason":"stop","message":{"content":"[]"}}]}"""
        // Act / Assert
        assertThat(failure { testSubject.response(profile, truncated, 1) }).isEqualTo(ProcessingFailure.INCOMPLETE)
        assertThat(failure { testSubject.response(profile, missing, 1) }).isEqualTo(ProcessingFailure.OUTPUT)
    }

    @Test
    fun `credentials cannot be sent to HTTP or embedded user info URLs`() {
        // Arrange / Act / Assert
        assertThat(
            failure {
                testSubject.request(
                    profile.copy(endpoint = "http://example.org"),
                    "fake",
                    listOf("x"),
                    "en",
                )
            },
        )
            .isEqualTo(ProcessingFailure.INVALID_CONFIGURATION)
        assertThat(
            failure {
                testSubject.request(
                    profile.copy(endpoint = "https://user:password@example.org"),
                    "fake",
                    listOf("x"),
                    "en",
                )
            },
        )
            .isEqualTo(ProcessingFailure.INVALID_CONFIGURATION)
    }

    private fun failure(block: () -> Any): ProcessingFailure? = try {
        block()
        null
    } catch (exception: ProcessingException) {
        exception.failure
    }
}
