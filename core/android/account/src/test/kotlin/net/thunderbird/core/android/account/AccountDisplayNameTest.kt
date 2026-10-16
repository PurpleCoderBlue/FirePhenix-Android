package net.thunderbird.core.android.account

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AccountDisplayNameTest {
    @Test
    fun `unmodified email labels use their known provider`() {
        assertThat(resolveAccountDisplayName("person@outlook.com", "person@outlook.com", null)).isEqualTo("Outlook")
        assertThat(resolveAccountDisplayName(null, "person@GMAIL.COM", null)).isEqualTo("Gmail")
        assertThat(resolveAccountDisplayName("", "person@hotmail.com", null)).isEqualTo("Outlook")
    }

    @Test
    fun `custom domain accounts use their configured provider host`() {
        assertThat(resolveAccountDisplayName(null, "person@example.com", "IMAP.GMAIL.COM.")).isEqualTo("Gmail")
        assertThat(resolveAccountDisplayName(null, "person@example.com", "outlook.office365.com")).isEqualTo("Outlook")
    }

    @Test
    fun `explicit nicknames are preserved exactly`() {
        assertThat(resolveAccountDisplayName("工作邮箱", "person@gmail.com", "imap.gmail.com")).isEqualTo("工作邮箱")
        assertThat(resolveAccountDisplayName("Personal", "person@outlook.com", null)).isEqualTo("Personal")
    }

    @Test
    fun `unknown and misleading provider suffixes retain the original email`() {
        val email = "person@gmail.com.example.org"
        assertThat(resolveAccountDisplayName(null, email, "imap.gmail.com.example.org")).isEqualTo(email)
        assertThat(resolveAccountDisplayName(null, "person@example.org", null)).isEqualTo("person@example.org")
    }

    @Test
    fun `display nickname leaves stored profile and sending identity unchanged`() {
        val account = LegacyAccountDto("171b55b1-6482-42f5-ab62-59e38fd212d2").apply {
            identities = mutableListOf(Identity(email = "person@outlook.com", name = "Sender Name"))
            name = "person@outlook.com"
        }
        assertThat(account.displayName).isEqualTo("Outlook")
        assertThat(account.name).isEqualTo("person@outlook.com")
        account.name = "Project Mail"
        assertThat(account.displayName).isEqualTo("Project Mail")
        assertThat(account.email).isEqualTo("person@outlook.com")
        assertThat(account.senderName).isEqualTo("Sender Name")
    }
}
