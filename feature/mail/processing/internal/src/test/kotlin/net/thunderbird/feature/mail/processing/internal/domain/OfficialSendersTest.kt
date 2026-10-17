package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isNotNull
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess
import org.junit.Test

class OfficialSendersTest {
    @Test
    fun `packaged JSON supplies sixteen brands with only Anthropic blocked`() {
        val testSubject = OfficialSenders.bundled
        assertThat(testSubject.entries.size).isEqualTo(16)
        assertThat(testSubject.entries.filter { it.imageAccess == RemoteImageAccess.BLOCKED }.map { it.name })
            .isEqualTo(listOf("Anthropic / Claude"))
        listOf("Steam", "Epic Games", "Microsoft", "GitHub", "Google").forEach { name ->
            assertThat(testSubject.entries.firstOrNull { it.name == name }).isNotNull()
        }
    }

    @Test
    fun `Epic exact official domains load images while lookalikes and unrelated subdomains do not`() {
        val config = ProcessingConfiguration()
        assertThat(RemoteImagePolicy.access(config, listOf("notice@email.epicgames.com")))
            .isEqualTo(RemoteImageAccess.AUTOMATIC)
        listOf("notice@email.epicgames.com.evil.org", "notice@other.epicgames.com").forEach { sender ->
            assertThat(RemoteImagePolicy.access(config, listOf(sender))).isEqualTo(RemoteImageAccess.MANUAL)
        }
    }

    @Test
    fun `packaged directory validator rejects wildcards public domains duplicate and invalid entries`() {
        val catalogue = OfficialSenders.bundled
        val steam = catalogue.entries.first()
        listOf(
            steam.copy(addresses = setOf("*@steampowered.com")),
            steam.copy(domains = setOf("gmail.com")),
            steam.copy(source = "https://[invalid"),
            steam.copy(imageAccess = RemoteImageAccess.MANUAL),
        ).forEach { sender ->
            assertThat(
                runCatching {
                    OfficialSenders.decode(Json.encodeToString(catalogue.copy(entries = listOf(sender))))
                },
            )
                .isFailure()
        }
        assertThat(
            runCatching {
                OfficialSenders.decode(Json.encodeToString(catalogue.copy(entries = listOf(steam, steam))))
            },
        ).isFailure()
    }
}
