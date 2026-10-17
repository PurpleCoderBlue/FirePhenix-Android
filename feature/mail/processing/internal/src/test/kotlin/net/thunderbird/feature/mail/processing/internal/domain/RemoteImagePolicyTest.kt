package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess
import net.thunderbird.feature.mail.processing.api.SenderListEntry
import net.thunderbird.feature.mail.processing.api.SenderListKind
import org.junit.Test

class RemoteImagePolicyTest {
    private val rule = SenderListEntry("Allowed@Example.org", SenderListKind.AUTO_IMAGES)
    private fun access(rules: List<SenderListEntry>, senders: List<String>) =
        RemoteImagePolicy.access(ProcessingConfiguration(senderListsVersion = 1, senderLists = rules), senders)

    @Test
    fun `exact sender permission is required for non catalogue images`() {
        assertThat(access(listOf(rule), listOf("allowed@example.org"))).isEqualTo(RemoteImageAccess.AUTOMATIC)
        assertThat(access(listOf(rule), listOf("unknown@example.org"))).isEqualTo(RemoteImageAccess.MANUAL)
        assertThat(access(listOf(rule.copy(enabled = false)), listOf(rule.sender))).isEqualTo(RemoteImageAccess.MANUAL)
    }

    @Test
    fun `missing multiple display name or lookalike senders never inherit trust`() {
        val senders = listOf(
            emptyList(),
            listOf("allowed@example.org", "unknown@example.org"),
            listOf("Allowed <allowed@example.org>"),
            listOf("allowed@example.org.evil.invalid"),
            listOf(""),
        )
        senders.forEach { assertThat(access(listOf(rule), it)).isEqualTo(RemoteImageAccess.MANUAL) }
    }

    @Test
    fun `GitHub notification and account messages load images from bundled defaults`() {
        listOf("notifications@github.com", "noreply@github.com").forEach { sender ->
            assertThat(access(emptyList(), listOf(sender))).isEqualTo(RemoteImageAccess.AUTOMATIC)
        }
        assertThat(access(emptyList(), listOf("noreply@github.com.evil.invalid")))
            .isEqualTo(RemoteImageAccess.MANUAL)
        assertThat(
            access(
                listOf(SenderListEntry("noreply@github.com", SenderListKind.BLOCK_IMAGES)),
                listOf("noreply@github.com"),
            ),
        ).isEqualTo(RemoteImageAccess.BLOCKED)
    }
}
