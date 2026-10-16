package net.thunderbird.feature.mail.processing.internal.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.feature.mail.processing.api.ProcessingAction
import net.thunderbird.feature.mail.processing.api.ProcessingConfiguration
import net.thunderbird.feature.mail.processing.api.ProcessingRule
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import org.junit.Test

class ProcessingAccountCleanupTest {
    @Test
    fun `account removal deletes only scoped rules and preserves shared services and other accounts`() {
        // Arrange
        val rule = ProcessingRule("owned", "Scoped", ProcessingAction.TRANSLATE, "shared", accountId = "removed")
        val shared = rule.copy(id = "global", accountId = "")
        val other = rule.copy(id = "other", accountId = "retained")
        val configuration = ProcessingConfiguration(
            profiles = listOf(ServiceProfile("shared", "Shared", ProcessingAction.TRANSLATE)),
            rules = listOf(rule, shared, other),
            automaticTranslation = true,
        )
        // Act
        val result = configuration.withoutAccount("removed")
        // Assert
        assertThat(result.rules).isEqualTo(listOf(shared, other))
        assertThat(result.profiles).isEqualTo(configuration.profiles)
        assertThat(result.automaticTranslation).isEqualTo(true)
    }
}
