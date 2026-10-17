package net.thunderbird.feature.mail.processing.internal

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.feature.mail.processing.api.MailProcessingExtension
import net.thunderbird.feature.mail.processing.api.ProcessingMessage
import net.thunderbird.feature.mail.processing.api.ReaderContentSlot
import net.thunderbird.feature.mail.processing.api.RemoteImageAccess
import net.thunderbird.feature.mail.processing.api.SenderListKind
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.data.contentHash
import net.thunderbird.feature.mail.processing.internal.data.updateConfiguration
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import net.thunderbird.feature.mail.processing.internal.domain.RemoteImagePolicy
import net.thunderbird.feature.mail.processing.internal.domain.hasSender
import net.thunderbird.feature.mail.processing.internal.domain.withSender
import net.thunderbird.feature.mail.processing.internal.ui.ProcessingReaderContent
import net.thunderbird.feature.mail.processing.internal.ui.ProcessingReaderViewModel
import net.thunderbird.feature.mail.processing.internal.ui.ProcessingSettingsActivity
import org.koin.compose.viewmodel.koinViewModel

internal class DefaultMailProcessingExtension(
    private val storage: ProcessingStorage,
    private val manager: MailProcessingManager,
) : MailProcessingExtension {
    override fun openSettings(context: Context) {
        context.startActivity(
            Intent(context, ProcessingSettingsActivity::class.java).apply {
                if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            },
        )
    }

    override fun openRemoteImageSettings(context: Context) {
        context.startActivity(
            Intent(context, net.thunderbird.feature.mail.processing.internal.ui.RemoteImageSettingsActivity::class.java)
                .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }

    override fun openSenderListSettings(context: Context, kind: SenderListKind) {
        context.startActivity(
            Intent(context, net.thunderbird.feature.mail.processing.internal.ui.RemoteImageSettingsActivity::class.java)
                .putExtra("senderListKind", kind.name)
                .apply { if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) },
        )
    }

    override fun senderMembership(sender: String): Set<SenderListKind> =
        SenderListKind.entries.filter { storage.configuration.value.hasSender(sender, it) }.toSet()

    override suspend fun setSenderMembership(sender: String, kind: SenderListKind, enabled: Boolean): Boolean =
        withContext(Dispatchers.IO) {
            if (RemoteImagePolicy.normalize(sender) == null) return@withContext false
            try {
                storage.updateConfiguration { it.withSender(sender, kind, enabled) }
                true
            } catch (_: java.io.IOException) {
                false
            } catch (_: net.thunderbird.feature.mail.processing.internal.data.ProcessingException) {
                false
            } catch (_: IllegalStateException) {
                false
            }
        }

    override fun shouldLoadRemoteImages(accountId: String, senders: List<String>): Boolean =
        remoteImageAccess(accountId, senders) == RemoteImageAccess.AUTOMATIC

    override fun remoteImageAccess(accountId: String, senders: List<String>): RemoteImageAccess =
        RemoteImagePolicy.access(storage.configuration.value, senders)

    override fun clearAccount(accountId: String) = manager.clearAccount(accountId)

    @Composable
    override fun Content(
        message: ProcessingMessage,
        onDisplayHtml: (String?) -> Unit,
        modifier: Modifier,
        slot: ReaderContentSlot,
    ) {
        val configuration by storage.configuration.collectAsState()
        val viewModel = koinViewModel<ProcessingReaderViewModel>(
            key = "mail-processing-" + contentHash(message.accountId + ":" + message.folder + ":" + message.messageId),
        )
        val context = androidx.compose.ui.platform.LocalContext.current
        ProcessingReaderContent(
            message,
            configuration,
            viewModel,
            onSettings = {
                context.startActivity(
                    Intent(context, ProcessingSettingsActivity::class.java).apply {
                        putExtra("accountId", message.accountId)
                        putExtra("folderId", message.folder)
                    },
                )
            },
            onDisplayHtml = onDisplayHtml,
            modifier = modifier,
            slot = slot,
        )
    }
}
