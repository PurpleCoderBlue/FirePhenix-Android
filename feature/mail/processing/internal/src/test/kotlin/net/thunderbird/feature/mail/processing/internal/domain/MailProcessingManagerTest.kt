package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingCredentials
import net.thunderbird.feature.mail.processing.internal.data.ProcessingException
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.data.withoutAccount
import org.junit.Test

class MailProcessingManagerTest {
    private val message = ProcessingMessage(
        "account",
        "mail",
        "inbox",
        "sender@example.org",
        "Subject",
        "<p>Hello</p>",
        false,
    )
    private val selection = ProcessingSelection("service", ProcessingAction.TRANSLATE)

    @Test
    fun `long processing holds one credential snapshot across every chunk`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val secrets = mutableListOf<String>()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                secrets += secret
                storage.secretValue = "changed-credential"
                return texts
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        testSubject.process(message.copy(html = "<p>" + "x".repeat(7_000) + "</p>"), selection, false) { _, _ -> }
        // Assert
        assertThat(secrets).isEqualTo(listOf("fake", "fake"))
    }

    @Test
    fun `summary processes all current text with its own model and language before reducing`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val summaryProfile = storage.configuration.value.profiles.single().copy(
            id = "summary",
            action = ProcessingAction.SUMMARIZE,
        )
        storage.configuration.value = storage.configuration.value.copy(
            profiles = storage.configuration.value.profiles + summaryProfile,
        )
        val current = "x".repeat(4_600)
        val seen = mutableListOf<String>()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                assertThat(profile.model).isEqualTo("summary-model")
                assertThat(language).isEqualTo("ko")
                seen += texts.single()
                return listOf("Condensed " + texts.single().take(20))
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        val result = testSubject.process(
            message.copy(html = "<p>$current</p><blockquote>Old history</blockquote>"),
            ProcessingSelection("summary", ProcessingAction.SUMMARIZE, model = "summary-model", language = "ko"),
            false,
        ) { _, _ -> }
        // Assert
        assertThat(seen.size).isEqualTo(3)
        assertThat(seen.take(2).joinToString("")).isEqualTo(current)
        assertThat(result.startsWith("Condensed")).isTrue()
    }

    @Test
    fun `a second reader observes progress while sharing the existing request`() = runTest {
        // Arrange
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val observed = CompletableDeferred<Pair<Int, Int>>()
        var calls = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                calls++
                entered.complete(Unit)
                release.await()
                return texts
            }
        }
        val testSubject = MailProcessingManager(
            FakeProcessingStorage(),
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        val first = async { testSubject.process(message, selection, false) { _, _ -> } }
        entered.await()
        val second = async {
            testSubject.process(message, selection, false) { done, total ->
                if (total > 0) observed.complete(done to total)
            }
        }
        val progress = observed.await()
        release.complete(Unit)
        first.await()
        second.await()
        // Assert
        assertThat(calls).isEqualTo(1)
        assertThat(progress).isEqualTo(0 to 1)
    }

    @Test
    fun `concurrent readers share one request and valid cache avoids another request`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var calls = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                calls++
                entered.complete(Unit)
                release.await()
                return texts.map { "Translated $it" }
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        val first = async { testSubject.process(message, selection, false) { _, _ -> } }
        entered.await()
        val second = async { testSubject.process(message, selection, false) { _, _ -> } }
        release.complete(Unit)
        val firstResult = first.await()
        val secondResult = second.await()
        val cached = testSubject.process(message, selection, false) { _, _ -> }
        // Assert
        assertThat(calls).isEqualTo(1)
        assertThat(firstResult).isEqualTo(secondResult)
        assertThat(cached).isEqualTo(firstResult)
    }

    @Test
    fun `explicit regeneration always starts a fresh completed request`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        var calls = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                calls++
                return texts.map { "Translation $calls" }
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        testSubject.process(message, selection, false) { _, _ -> }
        testSubject.process(message, selection, true) { _, _ -> }
        // Assert
        assertThat(calls).isEqualTo(2)
    }

    @Test
    fun `cache keys isolate accounts bodies models languages and configuration revisions`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ) = texts
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        val original = testSubject.requestKey(message, selection)
        // Assert
        assertThat(testSubject.requestKey(message.copy(accountId = "other"), selection)).isNotEqualTo(original)
        assertThat(testSubject.requestKey(message.copy(html = "<p>Changed</p>"), selection)).isNotEqualTo(original)
        assertThat(testSubject.requestKey(message, selection.copy(model = "other-model"))).isNotEqualTo(original)
        assertThat(testSubject.requestKey(message, selection.copy(language = "ja"))).isNotEqualTo(original)
        assertThat(testSubject.requestKey(message, selection.copy(includeQuotedContext = true))).isEqualTo(original)
        storage.configuration.value = storage.configuration.value.copy(
            profiles = storage.configuration.value.profiles.map { it.copy(revision = "changed") },
        )
        assertThat(testSubject.requestKey(message, selection)).isNotEqualTo(original)
    }

    @Test
    fun `clearing cache during a request prevents late cache repopulation`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                entered.complete(Unit)
                release.await()
                return texts
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        // Act
        val pending = async { testSubject.process(message, selection, false) { _, _ -> } }
        entered.await()
        storage.clearCache()
        release.complete(Unit)
        pending.await()
        // Assert
        assertThat(storage.cacheEntries.isEmpty()).isTrue()
    }

    @Test
    fun `later chunk failure never saves a partial translation`() = runTest {
        // Arrange
        val storage = FakeProcessingStorage()
        var calls = 0
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                calls++
                if (calls == 2) throw ProcessingException(ProcessingFailure.NETWORK)
                return texts
            }
        }
        val testSubject = MailProcessingManager(
            storage,
            service,
            backgroundScope,
            StandardTestDispatcher(testScheduler),
        )
        val long = message.copy(html = "<p>" + "x".repeat(7000) + "</p>")
        // Act
        val failed = try {
            testSubject.process(long, selection, false) { _, _ -> }
            false
        } catch (_: ProcessingException) {
            true
        }
        // Assert
        assertThat(failed).isTrue()
        assertThat(storage.cacheEntries.isEmpty()).isTrue()
    }
}

internal class FakeProcessingStorage : ProcessingStorage {
    override val configuration = MutableStateFlow(
        ProcessingConfiguration(
            profiles = listOf(
                ServiceProfile("service", "Test", ProcessingAction.TRANSLATE, model = "test-model"),
            ),
        ),
    )
    override val loadFailed = false
    override var cacheGeneration: Long = 0
    val cacheEntries = mutableMapOf<String, String>()
    override fun saveConfiguration(configuration: ProcessingConfiguration) {
        this.configuration.value = configuration
    }

    private val savedSecrets = mutableMapOf<String, String>()
    override fun saveProfile(profile: ServiceProfile, secret: String) {
        savedSecrets[profile.id] = secret
        configuration.value =
            configuration.value.copy(
                profiles = configuration.value.profiles.filterNot {
                    it.id == profile.id
                } + profile,
            )
    }
    override fun removeProfile(id: String) = Unit
    var secretValue = "fake"
    override fun credentials(profileId: String): ProcessingCredentials? = configuration.value.profiles
        .firstOrNull { it.id == profileId }?.let { ProcessingCredentials(it, savedSecrets[profileId] ?: secretValue) }

    override fun cached(accountId: String, key: String) = cacheEntries[accountId + key]
    override fun cache(accountId: String, key: String, content: String, generation: Long) {
        if (cacheGeneration != generation) return
        cacheEntries[accountId + key] = content
    }

    override fun clearAccount(accountId: String) {
        configuration.value = configuration.value.withoutAccount(accountId)
        clearCache(accountId)
    }

    override fun clearCache(accountId: String?) {
        cacheGeneration++
        cacheEntries.keys.removeAll { accountId == null || it.startsWith(accountId) }
    }
}
