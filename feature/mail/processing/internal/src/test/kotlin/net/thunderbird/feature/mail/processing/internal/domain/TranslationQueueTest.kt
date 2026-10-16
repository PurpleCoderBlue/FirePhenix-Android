package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingException
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import org.junit.Test

class TranslationQueueTest {
    @Test
    fun `three batches run two at a time and retain document order despite out of order completion`() = runTest {
        val storage = FakeProcessingStorage()
        var active = 0
        var peak = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                active++
                peak = maxOf(peak, active)
                delay(if (texts.first().startsWith("A")) 1_000 else 500)
                active--
                return texts.map { it.take(1) + " translated" }
            }
        }
        val testSubject =
            MailProcessingManager(
                storage,
                service,
                backgroundScope,
                StandardTestDispatcher(testScheduler),
            )
        val html = listOf("A", "B", "C").joinToString("") { "<p>" + it.repeat(3_000) + "</p>" }
        val message = ProcessingMessage("account", "mail", "inbox", "sender@example.org", "Subject", html, false)
        val result = testSubject.process(message, ProcessingSelection("service", ProcessingAction.TRANSLATE), false) {
                _,
                _,
            ->
        }
        assertThat(peak).isEqualTo(2)
        assertThat(testScheduler.currentTime).isEqualTo(1_000L)
        assertThat(result.indexOf("A translated") < result.indexOf("B translated")).isTrue()
        assertThat(result.indexOf("B translated") < result.indexOf("C translated")).isTrue()
    }

    @Test
    fun `different messages share the global concurrency bound`() = runTest {
        val storage = FakeProcessingStorage()
        var active = 0
        var peak = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                active++
                peak = maxOf(peak, active)
                delay(500)
                active--
                return texts
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        val html = "<p>" + "x".repeat(9_000) + "</p>"
        listOf("first", "second").map { id ->
            async {
                testSubject.process(
                    ProcessingMessage("account", id, "inbox", "", "", html, false),
                    ProcessingSelection("service", ProcessingAction.TRANSLATE),
                    false,
                ) { _, _ -> }
            }
        }.awaitAll()
        assertThat(peak).isEqualTo(2)
        assertThat(storage.cacheEntries.size).isEqualTo(2)
    }

    @Test
    fun `one failed batch cancels its sibling and never stores partial output`() = runTest {
        val storage = FakeProcessingStorage()
        var active = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                active++
                try {
                    if (texts.first().startsWith("A")) {
                        delay(500)
                        throw ProcessingException(ProcessingFailure.RATE_LIMIT)
                    }
                    delay(5_000)
                    return texts
                } finally {
                    active--
                }
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        val html = "<p>" + "A".repeat(3_000) + "</p><p>" + "B".repeat(3_000) + "</p>"
        val failure = runCatching {
            testSubject.process(
                ProcessingMessage("account", "failed", "inbox", "", "", html, false),
                ProcessingSelection("service", ProcessingAction.TRANSLATE),
                false,
            ) { _, _ -> }
        }.exceptionOrNull() as? ProcessingException
        assertThat(failure?.failure).isEqualTo(ProcessingFailure.RATE_LIMIT)
        assertThat(active).isEqualTo(0)
        assertThat(storage.cacheEntries.isEmpty()).isTrue()
    }
}
