package net.thunderbird.feature.account.settings.impl.alias

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewModelScope
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import net.thunderbird.core.android.account.Identity
import net.thunderbird.feature.account.settings.alias.AliasAccount
import net.thunderbird.feature.account.settings.alias.AliasRequest
import net.thunderbird.feature.account.settings.alias.AliasResolutionListener
import net.thunderbird.feature.account.settings.alias.AliasService
import net.thunderbird.feature.account.settings.alias.SenderAlias
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class AliasManagementViewModelTest {
    @Test
    fun `local candidates are suggestions until explicit add and are scanned once per page`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val service = FakeAliasService()
        val testSubject = AliasManagementViewModel("account", service)
        try {
            val loaded = testSubject.state.first { !it.busy }
            assertThat(loaded.candidates).isEqualTo(listOf("candidate@example.org"))
            assertThat(service.adds).isEqualTo(0)
            testSubject.event(AliasManagementEvent.Address("candidate@example.org"))
            assertThat(service.adds).isEqualTo(0)
            testSubject.event(AliasManagementEvent.Add)
            testSubject.state.first { !it.busy && it.address.isEmpty() }
            assertThat(service.adds).isEqualTo(1)
            assertThat(testSubject.state.value.aliases.size).isEqualTo(2)
            assertThat(testSubject.state.value.candidates).isEqualTo(emptyList())
            assertThat(service.scans).isEqualTo(1)
        } finally {
            testSubject.viewModelScope.cancel()
            Dispatchers.resetMain()
        }
    }

    private class FakeAliasService : AliasService {
        private val identities = mutableListOf(Identity(email = "primary@example.org"))
        var adds = 0
        var scans = 0
        override fun accounts() = listOf(AliasAccount("account", "Account"))
        override fun aliases(accountId: String) = identities.mapIndexed { index, identity ->
            SenderAlias(identity, isDefault = index == 0, isFavorite = false, isPrimary = index == 0)
        }
        override fun defaultIdentity(accountId: String) = identities.first()
        override fun openManagement(context: Context, accountId: String) = Unit
        override fun add(accountId: String, address: String) {
            adds++
            identities += Identity(email = address)
        }
        override fun remove(accountId: String, address: String) = Unit
        override fun setDefault(accountId: String, address: String) = Unit
        override fun favorite(accountId: String, address: String, favorite: Boolean) = Unit
        override fun clearAccount(accountId: String) = Unit
        override fun resolve(owner: LifecycleOwner, request: AliasRequest, listener: AliasResolutionListener) = Unit
        override suspend fun localCandidates(accountId: String): Set<String> {
            scans++
            return setOf("PRIMARY@example.org", "candidate@example.org", "Candidate@example.org")
        }
    }
}
