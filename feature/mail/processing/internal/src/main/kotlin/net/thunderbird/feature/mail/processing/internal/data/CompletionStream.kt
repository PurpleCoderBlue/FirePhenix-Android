package net.thunderbird.feature.mail.processing.internal.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okio.BufferedSource

/** Bounded SSE assembly. Reasoning deltas never become translated mail or cached output. */
internal object CompletionStream {
    fun read(source: BufferedSource, onPhase: (String) -> Unit = {}): String {
        val accumulator = Accumulator(onPhase)
        val event = StringBuilder()
        var bytes = 0L
        while (!source.exhausted()) {
            val line = source.readUtf8LineStrict(MAX_BYTES)
            bytes += line.toByteArray(Charsets.UTF_8).size + 1
            if (bytes > MAX_BYTES) throw ProcessingException(ProcessingFailure.OUTPUT)
            if (line.isEmpty()) {
                val done = accumulator.consume(event.toString())
                event.setLength(0)
                if (done) break
            } else if (line.startsWith(DATA_PREFIX)) {
                if (event.isNotEmpty()) event.append('\n')
                event.append(line.substring(DATA_PREFIX.length).removePrefix(" "))
            }
        }
        if (event.isNotEmpty()) accumulator.consume(event.toString())
        return accumulator.result()
    }

    private class Accumulator(private val onPhase: (String) -> Unit) {
        private val content = StringBuilder()
        private var finish: String? = null
        private var reasoningSeen = false
        private var answerSeen = false

        fun consume(data: String): Boolean {
            if (data.isNotBlank() && data != "[DONE]") {
                val root = parseEvent(data)
                if ("error" in root) throw ProcessingException(ProcessingFailure.OUTPUT)
                try {
                    val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    choice?.get("delta")?.jsonObject?.let(::appendDelta)
                    choice?.get("finish_reason")?.jsonPrimitive?.contentOrNull?.let { finish = it }
                } catch (_: IllegalArgumentException) {
                    throw ProcessingException(ProcessingFailure.OUTPUT)
                }
            }
            return data == "[DONE]"
        }

        private fun appendDelta(delta: JsonObject) {
            if (!reasoningSeen && !delta["reasoning_content"]?.jsonPrimitive?.contentOrNull.isNullOrEmpty()) {
                reasoningSeen = true
                onPhase("reasoning-first")
            }
            delta["content"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }?.let {
                if (!answerSeen) {
                    answerSeen = true
                    onPhase("answer-first")
                }
                content.append(it)
            }
        }

        fun result(): String {
            val reason = finish ?: throw ProcessingException(ProcessingFailure.INCOMPLETE)
            return buildJsonObject {
                put(
                    "choices",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("finish_reason", reason)
                                put("message", buildJsonObject { put("content", content.toString()) })
                            },
                        )
                    },
                )
            }.toString()
        }
    }

    private fun parseEvent(data: String): JsonObject = try {
        Json.parseToJsonElement(data).jsonObject
    } catch (_: Exception) {
        throw ProcessingException(ProcessingFailure.OUTPUT)
    }

    private const val DATA_PREFIX = "data:"
    private const val MAX_BYTES = 16L * 1024 * 1024
}
