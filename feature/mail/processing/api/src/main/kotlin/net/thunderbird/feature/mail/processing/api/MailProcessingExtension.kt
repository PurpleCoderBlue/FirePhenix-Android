package net.thunderbird.feature.mail.processing.api

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ReaderContentSlot { ACTIONS, RESULTS, INLINE }

/**
 * Reader and settings bridge. Implementations never modify the original server message.
 */
interface MailProcessingExtension {
    fun openSettings(context: Context)

    fun openRemoteImageSettings(context: Context) = Unit

    /** Fail closed when no rules implementation or verified Outlook contacts are available. */
    fun shouldLoadRemoteImages(accountId: String, senders: List<String>): Boolean = false

    fun clearAccount(accountId: String)

    @Composable
    fun Content(
        message: ProcessingMessage,
        onDisplayHtml: (String?) -> Unit,
        modifier: Modifier = Modifier,
        slot: ReaderContentSlot = ReaderContentSlot.INLINE,
    )
}
