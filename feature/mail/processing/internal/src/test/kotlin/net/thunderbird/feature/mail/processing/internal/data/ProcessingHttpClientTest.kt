package net.thunderbird.feature.mail.processing.internal.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test

class ProcessingHttpClientTest {
    private val profile = ServiceProfile("service", "Test", ProcessingAction.TRANSLATE, model = "test-model")

    @Test
    fun `connection check only requests model metadata without sending prompts or mail`() = runTest {
        var calls = 0
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            calls++
            assertThat(chain.request().method).isEqualTo("GET")
            assertThat(chain.request().body).isEqualTo(null)
            assertThat(
                chain.request().url.toString(),
            ).isEqualTo("https://maas.qianwenaiapi.com/compatible-mode/v1/models")
            Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1).code(200).message("OK")
                .body("{\"data\":[]}".toResponseBody("application/json".toMediaType())).build()
        }.build()
        ProcessingHttpClient(ProcessingProtocol(), client).check(
            profile.copy(endpoint = "https://maas.qianwenaiapi.com/compatible-mode/v1"),
            "fake-test-key",
        )
        assertThat(calls).isEqualTo(1)
    }

    @Test
    fun `HTTP errors have explicit statuses and never retry another provider`() = runTest {
        // Arrange
        val expected = mapOf(
            400 to ProcessingFailure.INVALID_CONFIGURATION,
            401 to ProcessingFailure.AUTHENTICATION,
            402 to ProcessingFailure.BILLING,
            429 to ProcessingFailure.RATE_LIMIT,
            504 to ProcessingFailure.TIMEOUT,
            302 to ProcessingFailure.ENDPOINT,
            404 to ProcessingFailure.ENDPOINT,
            422 to ProcessingFailure.INVALID_CONFIGURATION,
            503 to ProcessingFailure.SERVER,
        )
        for ((status, failure) in expected) {
            var calls = 0
            val client = OkHttpClient.Builder().addInterceptor { chain ->
                calls++
                Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status).message("Test response")
                    .body("{}".toResponseBody("application/json".toMediaType())).build()
            }.build()
            val testSubject = ProcessingHttpClient(ProcessingProtocol(), client)
            // Act
            val actual = try {
                testSubject.execute(profile, "fake-test-key", listOf("Hello"), "en")
                null
            } catch (exception: ProcessingException) {
                exception.failure
            }
            // Assert
            assertThat(actual).isEqualTo(failure)
            assertThat(calls).isEqualTo(1)
        }
    }

    @Test
    fun `transport diagnostics distinguish failures and never contain exception messages or credentials`() = runTest {
        val failures = listOf(
            UnknownHostException("private-endpoint") to ProcessingFailure.DNS,
            SSLHandshakeException("private-certificate") to ProcessingFailure.TLS,
            InterruptedIOException("private-call") to ProcessingFailure.TIMEOUT,
            IOException("private-header") to ProcessingFailure.NETWORK,
        )
        for ((exception, expected) in failures) {
            val logs = mutableListOf<String>()
            val client = OkHttpClient.Builder().addInterceptor { throw exception }.build()
            val testSubject = ProcessingHttpClient(ProcessingProtocol(), client, logs::add)
            val actual = try {
                testSubject.execute(profile, "fake-test-key", listOf("private-mail-content"), "en")
                null
            } catch (failure: ProcessingException) {
                failure.failure
            }
            assertThat(actual).isEqualTo(expected)
            assertThat(logs.any { "private" in it || "fake-test-key" in it }).isEqualTo(false)
            assertThat(logs.any { "failure=${expected.name}" in it }).isEqualTo(true)
        }
    }

    @Test
    fun `socket timeout is surfaced without exposing the request or secret`() = runTest {
        // Arrange
        val client = OkHttpClient.Builder().addInterceptor { throw SocketTimeoutException("test-only") }.build()
        val testSubject = ProcessingHttpClient(ProcessingProtocol(), client)
        // Act
        val actual = try {
            testSubject.execute(profile, "fake-test-key", listOf("Hello"), "en")
            null
        } catch (exception: ProcessingException) {
            exception.failure
        }
        // Assert
        assertThat(actual).isEqualTo(ProcessingFailure.TIMEOUT)
    }
}
