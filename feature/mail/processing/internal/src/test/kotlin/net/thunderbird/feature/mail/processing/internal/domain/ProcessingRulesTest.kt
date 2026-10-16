package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ProcessingRule
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import org.junit.Test

class ProcessingRulesTest {
    private val message =
        ProcessingMessage("account", "mail", "inbox", "person@example.org", "Invoice", "<p>Hello</p>", false)
    private val profile = ServiceProfile("service", "Configured service", ProcessingAction.TRANSLATE)
    private val rule = ProcessingRule("rule", "Invoices", ProcessingAction.TRANSLATE, profile.id)

    @Test
    fun `fresh configuration never processes mail automatically`() {
        // Arrange
        val testSubject = ProcessingConfiguration(profiles = listOf(profile), rules = listOf(rule))
        // Act
        val result = testSubject.matchingRule(message, ProcessingAction.TRANSLATE, 10)
        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `spam incomplete and long messages cannot bypass exclusion with any conditions`() {
        // Arrange
        val testSubject = ProcessingConfiguration(
            profiles = listOf(profile),
            rules = listOf(rule.copy(matchAll = false, subject = "Invoice")),
            automaticTranslation = true,
        )
        // Act / Assert
        assertThat(testSubject.matchingRule(message.copy(isSpam = true), ProcessingAction.TRANSLATE, 10)).isNull()
        assertThat(testSubject.matchingRule(message.copy(isComplete = false), ProcessingAction.TRANSLATE, 10)).isNull()
        assertThat(
            testSubject.matchingRule(
                message,
                ProcessingAction.TRANSLATE,
                AUTOMATIC_CHARACTER_LIMIT + 1,
            ),
        ).isNull()
    }

    @Test
    fun `first match keeps its own service model and language independently for each action`() {
        // Arrange
        val summaryProfile = profile.copy(id = "summary", action = ProcessingAction.SUMMARIZE)
        val first = rule.copy(model = "model-one", targetLanguage = "ja")
        val second = rule.copy(id = "later", model = "model-two")
        val summary = rule.copy(
            id = "summary-rule",
            action = ProcessingAction.SUMMARIZE,
            profileId = "summary",
            targetLanguage = "ko",
        )
        val testSubject = ProcessingConfiguration(
            profiles = listOf(profile, summaryProfile),
            rules = listOf(first, summary, second),
            automaticTranslation = true,
            automaticSummary = true,
        )
        // Act / Assert
        assertThat(testSubject.matchingRule(message, ProcessingAction.TRANSLATE, 10)).isEqualTo(first)
        assertThat(testSubject.matchingRule(message, ProcessingAction.SUMMARIZE, 10)).isEqualTo(summary)
    }

    @Test
    fun `unknown language and deleted services do not accidentally match`() {
        // Arrange
        val testSubject = ProcessingConfiguration(
            profiles = listOf(profile),
            rules = listOf(rule.copy(sourceLanguage = "en"), rule.copy(id = "deleted", profileId = "missing")),
            automaticTranslation = true,
        )
        // Act / Assert
        assertThat(testSubject.matchingRule(message, ProcessingAction.TRANSLATE, 10)).isNull()
    }

    @Test
    fun `sender domains match complete domain boundaries`() {
        // Arrange
        val testSubject = rule.copy(sender = "@example.org")
        // Act / Assert
        assertThat(testSubject.matches(message)).isTrue()
        assertThat(testSubject.matches(message.copy(sender = "person@badexample.org"))).isEqualTo(false)
    }

    @Test
    fun `unicode segmentation preserves the complete message and whitespace`() {
        // Arrange
        val source = ("Hello 🔥 凤凰\n" + "x".repeat(100)).repeat(100)
        // Act
        val result = splitText(source, 63)
        // Assert
        assertThat(result.joinToString("")).isEqualTo(source)
        assertThat(result.all { it.codePointCount(0, it.length) <= 63 }).isTrue()
        assertThat(result.none { it.lastOrNull()?.isHighSurrogate() == true }).isTrue()
    }
}
