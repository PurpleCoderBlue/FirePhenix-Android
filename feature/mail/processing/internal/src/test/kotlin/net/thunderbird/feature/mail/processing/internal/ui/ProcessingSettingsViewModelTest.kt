package net.thunderbird.feature.mail.processing.internal.ui

import androidx.lifecycle.viewModelScope
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import net.thunderbird.feature.mail.processing.internal.data.ProcessingProtocol
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import org.junit.After
import org.junit.Before
import org.junit.Test

class ProcessingSettingsViewModelTest {
    private val storage = FakeProcessingStorage()
    private lateinit var testSubject: ProcessingSettingsViewModel

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        testSubject = ProcessingSettingsViewModel(storage, ProcessingProtocol())
    }

    @After
    fun after() {
        runBlocking { testSubject.viewModelScope.coroutineContext[Job]?.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `model tests use fixed public fixtures and keep the saved key outside editor state`() = runTest {
        testSubject.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
        val profile = storage.configuration.value.profiles.single()
        storage.saveProfile(profile, "test-only-key")
        val payloads = mutableListOf<List<String>>()
        val service = object : net.thunderbird.feature.mail.processing.internal.data.ProcessingService {
            override suspend fun execute(
                profile: ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> {
                assertThat(secret).isEqualTo("test-only-key")
                assertThat(language).isEqualTo("zh-Hans")
                payloads.add(texts)
                return listOf("你好。")
            }
        }
        testSubject = ProcessingSettingsViewModel(storage, ProcessingProtocol(), modelService = service)
        testSubject.event(ProcessingSettingsEvent.EditProfile(profile))
        testSubject.event(ProcessingSettingsEvent.TestModel)
        val result = testSubject.state.first { it.modelTestOutput != null || it.error != null }
        assertThat(result.modelTestOutput).isEqualTo("你好。")
        assertThat(result.secret).isEqualTo("")
        assertThat(payloads.single().single().length).isEqualTo(1116)
        assertThat(payloads.single().single().startsWith("Hello. This demonstration")).isEqualTo(true)
    }

    @Test
    fun `connection check reuses encrypted credentials without copying them into editor state or saving`() = runTest {
        testSubject.viewModelScope.coroutineContext[Job]?.cancelAndJoin()
        val profile = storage.configuration.value.profiles.single()
        storage.saveProfile(profile, "test-only-key")
        var calls = 0
        val tester = object : net.thunderbird.feature.mail.processing.internal.data.ProcessingConnectionTester {
            override suspend fun check(profile: ServiceProfile, secret: String) {
                calls++
                assertThat(secret).isEqualTo("test-only-key")
            }
        }
        testSubject = ProcessingSettingsViewModel(storage, ProcessingProtocol(), tester)
        testSubject.event(ProcessingSettingsEvent.EditProfile(profile))
        testSubject.event(ProcessingSettingsEvent.CheckConnection)
        val result = testSubject.state.first { it.connectionChecked || it.error != null }
        assertThat(result.connectionChecked).isEqualTo(true)
        assertThat(result.secret).isEqualTo("")
        assertThat(result.profile?.id).isEqualTo(profile.id)
        assertThat(calls).isEqualTo(1)
        testSubject.event(ProcessingSettingsEvent.DraftProfile(profile.copy(name = "Changed")))
        assertThat(testSubject.state.value.connectionChecked).isEqualTo(false)
    }

    @Test
    fun `blank optional parameters still allow a valid local service to be saved`() = runTest {
        val profile = storage.configuration.value.profiles.single()
        testSubject.event(ProcessingSettingsEvent.EditProfile(profile))
        testSubject.event(ProcessingSettingsEvent.Parameters(""))
        testSubject.event(ProcessingSettingsEvent.Secret("test-only-key"))

        testSubject.event(ProcessingSettingsEvent.SaveProfile)
        val result = testSubject.state.first { !it.saving }

        assertThat(result.error).isEqualTo(null)
        assertThat(result.profile).isEqualTo(null)
        assertThat(storage.credentials(profile.id)?.secret).isEqualTo("test-only-key")
    }

    @Test
    fun `automatic processing remains off when its service is not configured`() = runTest {
        // Arrange
        storage.configuration.value = storage.configuration.value.copy(profiles = emptyList())
        // Act
        testSubject.event(
            ProcessingSettingsEvent.Automatic(
                net.thunderbird.feature.mail.processing.api.ProcessingAction.TRANSLATE,
                true,
            ),
        )
        val result = testSubject.state.first { !it.saving }
        // Assert
        assertThat(result.error).isEqualTo(ProcessingFailure.NOT_CONFIGURED)
        assertThat(result.configuration.automaticTranslation).isEqualTo(false)
    }

    @Test
    fun `copying a model service saves independent credentials without exposing the key in UI state`() = runTest {
        val source = storage.configuration.value.profiles.single()
        testSubject.event(
            ProcessingSettingsEvent.AddProfile(net.thunderbird.feature.mail.processing.api.ProcessingAction.SUMMARIZE),
        )
        testSubject.event(ProcessingSettingsEvent.CopyProfile(source.id))
        val copied = testSubject.state.value.profile!!
        assertThat(testSubject.state.value.secret).isEqualTo("")
        assertThat(copied.id == source.id).isEqualTo(false)

        testSubject.event(ProcessingSettingsEvent.SaveProfile)
        assertThat(testSubject.state.first { !it.saving }.error).isEqualTo(null)
        assertThat(storage.credentials(copied.id)?.secret).isEqualTo("fake")
        storage.saveProfile(source.copy(model = "changed-source-model"), "changed-source-key")

        assertThat(storage.credentials(copied.id)?.secret).isEqualTo("fake")
        assertThat(storage.credentials(copied.id)?.profile?.model).isEqualTo(source.model)
    }

    @Test
    fun `changing a copied destination requires a new key`() = runTest {
        val source = storage.configuration.value.profiles.single()
        testSubject.event(
            ProcessingSettingsEvent.AddProfile(net.thunderbird.feature.mail.processing.api.ProcessingAction.SUMMARIZE),
        )
        testSubject.event(ProcessingSettingsEvent.CopyProfile(source.id))
        val copied = testSubject.state.value.profile!!
        testSubject.event(
            ProcessingSettingsEvent.DraftProfile(copied.copy(endpoint = "https://different.example.invalid/v1")),
        )

        testSubject.event(ProcessingSettingsEvent.SaveProfile)
        val result = testSubject.state.first { !it.saving }

        assertThat(result.error).isEqualTo(ProcessingFailure.NOT_CONFIGURED)
        assertThat(result.profileField).isEqualTo(ProfileField.KEY)
        assertThat(storage.configuration.value.profiles.size).isEqualTo(1)
    }

    @Test
    fun `copy explicitly replaces an existing target key even at the same provider address`() = runTest {
        val source = storage.configuration.value.profiles.single()
        val target = source.copy(
            id = "summary-target",
            action = net.thunderbird.feature.mail.processing.api.ProcessingAction.SUMMARIZE,
        )
        storage.saveProfile(target, "previous-target-key")
        testSubject.event(ProcessingSettingsEvent.EditProfile(target))
        testSubject.event(ProcessingSettingsEvent.CopyProfile(source.id))

        testSubject.event(ProcessingSettingsEvent.SaveProfile)
        assertThat(testSubject.state.first { !it.saving }.error).isEqualTo(null)

        assertThat(storage.credentials(target.id)?.secret).isEqualTo("fake")
        assertThat(testSubject.state.value.secret).isEqualTo("")
    }

    @Test
    fun `changing destination requires a fresh key instead of reusing another service credential`() = runTest {
        // Arrange
        val profile = storage.configuration.value.profiles.single()
        testSubject.event(ProcessingSettingsEvent.EditProfile(profile))
        testSubject.event(
            ProcessingSettingsEvent.DraftProfile(profile.copy(endpoint = "https://other.example.invalid/v1")),
        )
        // Act
        testSubject.event(ProcessingSettingsEvent.SaveProfile)
        val result = testSubject.state.first { !it.saving }
        // Assert
        assertThat(result.error).isEqualTo(ProcessingFailure.NOT_CONFIGURED)
    }
}
