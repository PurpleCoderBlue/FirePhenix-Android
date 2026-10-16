package net.thunderbird.feature.mail.processing.internal.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okio.Buffer
import org.junit.Test

class CompletionStreamTest {
    @Test
    fun `stream assembles Chinese content ignoring reasoning and heartbeat events`() {
        val source = Buffer().writeUtf8(
            ": heartbeat\n\ndata: {\"choices\":[{\"delta\":{\"reasoning_content\":\"private reasoning\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n" +
                "data: {\"choices\":[{\"delta\":{\"content\":\"世界\"},\"finish_reason\":\"stop\"}]}\n\n" +
                "data: [DONE]\n\n",
        )
        val phases = mutableListOf<String>()
        val result = Json.parseToJsonElement(CompletionStream.read(source, phases::add)).jsonObject
        assertThat(phases).isEqualTo(listOf("reasoning-first", "answer-first"))
        val choice = result.getValue("choices").jsonArray.single().jsonObject
        assertThat(choice.getValue("message").jsonObject.getValue("content").jsonPrimitive.content).isEqualTo("你好世界")
        assertThat(choice.getValue("finish_reason").jsonPrimitive.content).isEqualTo("stop")
    }

    @Test
    fun `EOF without a finish event never turns partial output into a completed translation`() {
        val source = Buffer().writeUtf8("data: {\"choices\":[{\"delta\":{\"content\":\"partial\"}}]}\n\n")
        val failure = try {
            CompletionStream.read(source)
            null
        } catch (
            exception: ProcessingException,
        ) {
            exception.failure
        }
        assertThat(failure).isEqualTo(ProcessingFailure.INCOMPLETE)
    }
}
