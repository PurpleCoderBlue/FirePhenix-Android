package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ProcessingRule
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess
import net.thunderbird.feature.mail.processing.api.RemoteImageRule
import net.thunderbird.feature.mail.processing.api.SenderListEntry
import net.thunderbird.feature.mail.processing.api.SenderListKind
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import org.junit.Test

class SenderListsTest {
    @Test
    fun `upgrade disables every legacy rule without copying any sender or changing services`() {
        val old = ProcessingConfiguration(
            profiles = listOf(ServiceProfile("p", "Keep", ProcessingAction.TRANSLATE)),
            defaultTranslationProfile = "p",
            automaticTranslation = true,
            automaticSummary = true,
            rules = listOf(ProcessingRule("old", "Old", ProcessingAction.TRANSLATE, "p", sender = "a@example.org")),
            remoteImageRules = listOf(RemoteImageRule("image", "a@example.org")),
        )
        val migrated = old.withSenderListsMigration()
        assertThat(migrated.senderLists).isEqualTo(emptyList())
        assertThat(migrated.rules.all { !it.enabled }).isEqualTo(true)
        assertThat(migrated.remoteImageRules.all { !it.enabled }).isEqualTo(true)
        assertThat(migrated.automaticTranslation || migrated.automaticSummary).isEqualTo(false)
        assertThat(migrated.profiles).isEqualTo(old.profiles)
        assertThat(migrated.defaultTranslationProfile).isEqualTo("p")
        val edited = migrated.copy(
            senderLists = listOf(SenderListEntry("new@example.org", SenderListKind.AUTO_TRANSLATE)),
        )
        assertThat(edited.withSenderListsMigration()).isEqualTo(edited)
    }

    @Test
    fun `automatic processing uses only exact sender membership and the default service`() {
        val config = ProcessingConfiguration(
            profiles = listOf(ServiceProfile("p", "Translate", ProcessingAction.TRANSLATE)),
            defaultTranslationProfile = "p",
            senderListsVersion = 1,
            senderLists = listOf(SenderListEntry("Sender@Example.org", SenderListKind.AUTO_TRANSLATE)),
        )
        val mail = ProcessingMessage("account", "id", "inbox", "sender@example.org", "", "Hello", false)
        assertThat(config.matchingRule(mail, ProcessingAction.TRANSLATE, 5)?.profileId).isEqualTo("p")
        assertThat(config.matchingRule(mail.copy(sender = "other@example.org"), ProcessingAction.TRANSLATE, 5)).isNull()
        assertThat(config.matchingRule(mail, ProcessingAction.SUMMARIZE, 5)).isNull()
        assertThat(config.matchingRule(mail.copy(isSpam = true), ProcessingAction.TRANSLATE, 5)).isNull()
        assertThat(config.matchingRule(mail.copy(isComplete = false), ProcessingAction.TRANSLATE, 5)).isNull()
        assertThat(config.matchingRule(mail, ProcessingAction.TRANSLATE, 30_001)).isNull()
        assertThat(config.copy(defaultTranslationProfile = "missing").matchingRule(mail, ProcessingAction.TRANSLATE, 5))
            .isNull()
    }

    @Test
    fun `image deny wins over allow and unknown or multiple senders never auto load`() {
        val sender = "person@example.org"
        val config = ProcessingConfiguration(
            senderListsVersion = 1,
            senderLists = listOf(
                SenderListEntry(sender, SenderListKind.AUTO_IMAGES),
                SenderListEntry(sender, SenderListKind.BLOCK_IMAGES),
            ),
        )
        assertThat(RemoteImagePolicy.access(config, listOf(sender))).isEqualTo(RemoteImageAccess.BLOCKED)
        assertThat(RemoteImagePolicy.access(config, listOf(sender, "other@example.org")))
            .isEqualTo(RemoteImageAccess.BLOCKED)
        assertThat(RemoteImagePolicy.access(ProcessingConfiguration(), listOf(sender)))
            .isEqualTo(RemoteImageAccess.MANUAL)
        assertThat(RemoteImagePolicy.access(config.copy(senderLists = config.senderLists.take(1)), listOf(sender)))
            .isEqualTo(RemoteImageAccess.AUTOMATIC)
        assertThat(
            RemoteImagePolicy.access(config.copy(senderLists = config.senderLists.take(1)), listOf(sender, sender)),
        )
            .isEqualTo(RemoteImageAccess.MANUAL)
    }

    @Test
    fun `built in image defaults cannot trigger AI and exact user choices override catalogue`() {
        val config = ProcessingConfiguration(senderListsVersion = 1)
        assertThat(RemoteImagePolicy.access(config, listOf("news@anthropic.com"))).isEqualTo(RemoteImageAccess.BLOCKED)
        assertThat(RemoteImagePolicy.access(config, listOf("noreply@steampowered.com")))
            .isEqualTo(RemoteImageAccess.AUTOMATIC)
        assertThat(RemoteImagePolicy.access(config, listOf("noreply@steampowered.com.evil.org")))
            .isEqualTo(RemoteImageAccess.MANUAL)
        assertThat(RemoteImagePolicy.access(config, listOf("news@unknown.anthropic.com")))
            .isEqualTo(RemoteImageAccess.MANUAL)
        val allowed = config.copy(
            senderLists = listOf(SenderListEntry("news@anthropic.com", SenderListKind.AUTO_IMAGES)),
        )
        assertThat(RemoteImagePolicy.access(allowed, listOf("news@anthropic.com")))
            .isEqualTo(RemoteImageAccess.AUTOMATIC)
        val mail = ProcessingMessage("a", "id", "inbox", "noreply@steampowered.com", "", "Hi", false)
        assertThat(config.matchingRule(mail, ProcessingAction.TRANSLATE, 2)).isNull()
        assertThat(config.matchingRule(mail, ProcessingAction.SUMMARIZE, 2)).isNull()
    }
}
