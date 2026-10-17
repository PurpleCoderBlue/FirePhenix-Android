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

    fun openSenderListSettings(context: Context, kind: SenderListKind) = openRemoteImageSettings(context)

    fun senderMembership(sender: String): Set<SenderListKind> = emptySet()

    suspend fun setSenderMembership(sender: String, kind: SenderListKind, enabled: Boolean): Boolean = false

    fun remoteImageAccess(accountId: String, senders: List<String>): RemoteImageAccess =
        if (shouldLoadRemoteImages(accountId, senders)) RemoteImageAccess.AUTOMATIC else RemoteImageAccess.MANUAL

    /** Unknown senders require a manual image-loading action. */
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
