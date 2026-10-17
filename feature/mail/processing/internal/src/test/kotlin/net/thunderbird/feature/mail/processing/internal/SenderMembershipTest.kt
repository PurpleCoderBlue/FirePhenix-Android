package net.thunderbird.feature.mail.processing.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.test.runTest
import net.thunderbird.feature.mail.processing.api.SenderListKind
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.domain.FakeProcessingStorage
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import org.junit.Test

class SenderMembershipTest {
    @Test
    fun `menu writes all four memberships independently and preserves service credentials`() = runTest {
        val storage = FakeProcessingStorage()
        val service = object : ProcessingService {
            override suspend fun execute(
                profile: net.thunderbird.feature.mail.processing.api.ServiceProfile,
                secret: String,
                texts: List<String>,
                language: String,
            ): List<String> = error("No service calls allowed")
        }
        val profile = storage.configuration.value.profiles.single()
        storage.saveProfile(profile, "test-only-secret")
        val extension = DefaultMailProcessingExtension(
            storage,
            MailProcessingManager(storage, service, backgroundScope),
        )
        SenderListKind.entries.forEach { kind ->
            assertThat(extension.setSenderMembership(" Sender@Example.org ", kind, true)).isEqualTo(true)
        }
        assertThat(extension.senderMembership("sender@example.org")).isEqualTo(SenderListKind.entries.toSet())
        assertThat(extension.setSenderMembership("sender@example.org", SenderListKind.AUTO_TRANSLATE, false))
            .isEqualTo(true)
        assertThat(extension.senderMembership("sender@example.org"))
            .isEqualTo(SenderListKind.entries.toSet() - SenderListKind.AUTO_TRANSLATE)
        assertThat(extension.setSenderMembership("*@example.org", SenderListKind.AUTO_IMAGES, true)).isEqualTo(false)
        assertThat(storage.credentials(profile.id)?.secret).isEqualTo("test-only-secret")
        assertThat(storage.configuration.value.profiles).isEqualTo(listOf(profile))
    }
}
