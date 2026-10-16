package net.thunderbird.feature.mail.processing.internal.data

import java.io.IOException
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

internal interface ProcessingService {
    suspend fun execute(profile: ServiceProfile, secret: String, texts: List<String>, language: String): List<String>
}

internal interface ProcessingConnectionTester {
    suspend fun check(profile: ServiceProfile, secret: String)
}

internal class ProcessingHttpClient(
    private val protocol: ProcessingProtocol,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .retryOnConnectionFailure(false)
        .followRedirects(false)
        .followSslRedirects(false)
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .callTimeout(CALL_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .build(),
    private val diagnostic: (String) -> Unit = {},
) : ProcessingService, ProcessingConnectionTester {
    override suspend fun execute(
        profile: ServiceProfile,
        secret: String,
        texts: List<String>,
        language: String,
    ): List<String> {
        val prefix = "provider=${profile.provider.name} action=${profile.action.name}"
        val wire = protocol.request(profile, secret, texts, language)
        diagnostic("$prefix stage=request")
        val request = Request.Builder().url(wire.url).apply {
            wire.headers.forEach { (name, value) -> header(name, value) }
        }.post(wire.body.toRequestBody("application/json; charset=utf-8".toMediaType())).build()
        val body = send(request, prefix)
        val expected =
            if (profile.action == ProcessingAction.SUMMARIZE) 1 else texts.size
        return try {
            protocol.response(profile, body, expected).also { diagnostic("$prefix stage=complete") }
        } catch (exception: ProcessingException) {
            diagnostic("$prefix stage=decode failure=${exception.failure.name}")
            throw exception
        }
    }

    /** Authenticated metadata GET: never sends mail, prompts or inference parameters. */
    override suspend fun check(profile: ServiceProfile, secret: String) {
        val wire = protocol.connectionRequest(profile, secret)
        val request = Request.Builder().url(wire.url).apply {
            wire.headers.forEach { (name, value) -> header(name, value) }
        }.get().build()
        val body = send(request, "provider=${profile.provider.name} action=CONNECTION_CHECK")
        protocol.modelsResponse(body)
    }

    private suspend fun send(request: Request, prefix: String): String {
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(
                object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        if (continuation.isActive) {
                            val failure = ioFailure(e)
                            diagnostic("$prefix stage=transport failure=${failure.name} type=${e.javaClass.simpleName}")
                            continuation.resumeWithException(ProcessingException(failure))
                        }
                    }

                    override fun onResponse(call: Call, response: Response) {
                        response.use {
                            try {
                                diagnostic("$prefix stage=response http=${response.code}")
                                if (!response.isSuccessful) {
                                    throw ProcessingException(httpFailure(response.code))
                                }
                                val source = response.body.source()
                                val text = if (response.body.contentType()?.subtype == "event-stream") {
                                    diagnostic("$prefix stage=stream-start")
                                    CompletionStream.read(source) { phase -> diagnostic("$prefix stage=$phase") }
                                } else {
                                    if (source.request(MAX_RESPONSE_BYTES + 1)) {
                                        throw ProcessingException(ProcessingFailure.OUTPUT)
                                    }
                                    source.readUtf8()
                                }
                                if (continuation.isActive) continuation.resume(text)
                            } catch (exception: IOException) {
                                val failure = ioFailure(exception)
                                diagnostic(
                                    "$prefix stage=read failure=${failure.name} type=${exception.javaClass.simpleName}",
                                )
                                if (continuation.isActive) {
                                    continuation.resumeWithException(ProcessingException(failure))
                                }
                            } catch (exception: ProcessingException) {
                                if (continuation.isActive) continuation.resumeWithException(exception)
                            }
                        }
                    }
                },
            )
        }
    }

    private fun ioFailure(exception: IOException): ProcessingFailure = when (exception) {
        is UnknownHostException -> ProcessingFailure.DNS
        is SSLException -> ProcessingFailure.TLS
        is SocketTimeoutException, is InterruptedIOException -> ProcessingFailure.TIMEOUT
        else -> ProcessingFailure.NETWORK
    }

    @Suppress("MagicNumber") // Standard HTTP status codes are clearer at this protocol boundary.
    private fun httpFailure(code: Int): ProcessingFailure = when (code) {
        400, 422 -> ProcessingFailure.INVALID_CONFIGURATION
        401, 403 -> ProcessingFailure.AUTHENTICATION
        402 -> ProcessingFailure.BILLING
        429 -> ProcessingFailure.RATE_LIMIT
        408, 504 -> ProcessingFailure.TIMEOUT
        404, 405, in 300..399 -> ProcessingFailure.ENDPOINT
        in 500..599 -> ProcessingFailure.SERVER
        else -> ProcessingFailure.NETWORK
    }

    private companion object {
        const val CONNECT_TIMEOUT_SECONDS = 20L
        const val READ_TIMEOUT_SECONDS = 120L
        const val CALL_TIMEOUT_SECONDS = 180L
        const val MAX_RESPONSE_BYTES = 16L * 1024 * 1024
    }
}
