package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import net.thunderbird.feature.mail.processing.api.RemoteImageRule
import org.junit.Test

class RemoteImagePolicyTest {
    private val rule = RemoteImageRule("allow", "Allowed@Example.org")

    @Test
    fun `sender rule or verified Outlook contact independently allows images`() {
        val testSubject = RemoteImagePolicy
        assertThat(testSubject.allows(listOf(rule), listOf("allowed@example.org")) { false }).isTrue()
        assertThat(testSubject.allows(emptyList(), listOf("contact@example.org")) { true }).isTrue()
        assertThat(testSubject.allows(listOf(rule), listOf("unknown@example.org")) { false }).isFalse()
        assertThat(testSubject.allows(listOf(rule.copy(enabled = false)), listOf(rule.sender)) { false }).isFalse()
    }

    @Test
    fun `missing multiple display name or lookalike senders never inherit trust`() {
        val testSubject = RemoteImagePolicy
        val senders = listOf(
            emptyList(),
            listOf("allowed@example.org", "unknown@example.org"),
            listOf("Allowed <allowed@example.org>"),
            listOf("allowed@example.org.evil.invalid"),
            listOf(""),
        )
        senders.forEach { assertThat(testSubject.allows(listOf(rule), it) { false }).isFalse() }
    }
}
