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
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import org.junit.After
import org.junit.Before
import org.junit.Test

class RemoteImageSettingsViewModelTest {
    private val storage = FakeProcessingStorage()
    private lateinit var testSubject: RemoteImageSettingsViewModel

    @Before
    fun before() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        testSubject = RemoteImageSettingsViewModel(storage)
    }

    @After
    fun after() {
        runBlocking { testSubject.viewModelScope.coroutineContext[Job]?.cancelAndJoin() }
        Dispatchers.resetMain()
    }

    @Test
    fun `rules validate sender and save disable delete without changing services`() = runTest {
        val services = storage.configuration.value.profiles
        testSubject.event(RemoteImageSettingsEvent.Add)
        testSubject.event(RemoteImageSettingsEvent.Sender("*@example.org"))
        testSubject.event(RemoteImageSettingsEvent.Save)
        assertThat(testSubject.state.value.invalidSender).isEqualTo(true)
        testSubject.event(RemoteImageSettingsEvent.Sender(" Allowed@Example.org "))
        testSubject.event(RemoteImageSettingsEvent.Save)
        val saved = testSubject.state.first { it.rules.isNotEmpty() }.rules.single()
        assertThat(saved.sender).isEqualTo("allowed@example.org")
        testSubject.event(RemoteImageSettingsEvent.Enabled(saved, false))
        testSubject.state.first { it.rules.singleOrNull()?.enabled == false }
        testSubject.event(RemoteImageSettingsEvent.Delete(saved))
        testSubject.state.first { it.rules.isEmpty() }
        assertThat(storage.configuration.value.profiles).isEqualTo(services)
    }
}
