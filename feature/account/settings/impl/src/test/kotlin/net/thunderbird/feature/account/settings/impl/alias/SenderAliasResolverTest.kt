package net.thunderbird.feature.account.settings.impl.alias

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import net.thunderbird.feature.account.settings.alias.AliasRequest
import org.junit.Test

class SenderAliasResolverTest {
    private val testSubject = SenderAliasResolver()
    private val aliases = listOf("primary@example.org", "alias@example.org")
    private fun request(
        explicit: String? = null,
        original: List<String> = emptyList(),
        reply: List<String> = emptyList(),
    ) = AliasRequest("account", explicit, original, reply, listOf("contact@example.org"))

    @Test
    fun `a removed original From cannot silently fall back to another registered From`() {
        // Arrange / Act
        val result = testSubject.resolve(
            request(original = listOf("removed@example.org", aliases[0])),
            aliases,
            aliases[0],
            emptySet(),
        )
        // Assert
        assertThat(result.requiresChoice).isTrue()
        assertThat(result.invalid).isEqualTo("removed@example.org")
    }

    @Test
    fun `explicit choice cannot be overwritten by reply draft or history evidence`() {
        // Arrange / Act
        val result = testSubject.resolve(
            request(
                explicit = aliases[1],
                original = listOf(aliases[0]),
            ),
            aliases,
            aliases[0],
            setOf(aliases[0]),
        )
        // Assert
        assertThat(result.selected).isEqualTo(aliases[1])
    }

    @Test
    fun `draft identity wins over reply and history evidence`() {
        // Arrange / Act
        val result = testSubject.resolve(
            request(
                original = listOf(aliases[1]),
                reply = listOf(aliases[0]),
            ),
            aliases,
            aliases[0],
            setOf(aliases[0]),
        )
        // Assert
        assertThat(result.selected).isEqualTo(aliases[1])
    }

    @Test
    fun `multiple recipient aliases require a choice instead of using default`() {
        // Arrange / Act
        val result = testSubject.resolve(request(reply = aliases), aliases, aliases[0], emptySet())
        // Assert
        assertThat(result.requiresChoice).isTrue()
        assertThat(result.choices).isEqualTo(aliases)
    }

    @Test
    fun `conflicting contact history requires a choice`() {
        // Arrange / Act
        val result = testSubject.resolve(request(), aliases, aliases[0], aliases.toSet())
        // Assert
        assertThat(result.requiresChoice).isTrue()
    }

    @Test
    fun `removed draft sender is invalid and cannot silently fall back`() {
        // Arrange / Act
        val result = testSubject.resolve(
            request(original = listOf("removed@example.org")),
            aliases,
            aliases[0],
            emptySet(),
        )
        // Assert
        assertThat(result.requiresChoice).isTrue()
        assertThat(result.invalid).isEqualTo("removed@example.org")
    }

    @Test
    fun `unique history is used and absent evidence uses account default`() {
        // Arrange / Act / Assert
        assertThat(
            testSubject.resolve(
                request(),
                aliases,
                aliases[0],
                setOf(aliases[1]),
            ).selected,
        ).isEqualTo(aliases[1])
        assertThat(testSubject.resolve(request(), aliases, aliases[1], emptySet()).selected).isEqualTo(aliases[1])
    }
}
