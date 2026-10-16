package net.thunderbird.app.common.account

import app.k9mail.legacy.mailstore.MessageStoreManager
import com.fsck.k9.controller.MessagingController
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.core.common.mail.Flag
import net.thunderbird.feature.account.settings.alias.SentAliasHistory

/** Adapts the existing sent folder; no new mail backend or OAuth scopes. */
internal class LegacySentAliasHistory(
    private val accounts: LegacyAccountDtoManager,
    private val stores: MessageStoreManager,
    private val controller: MessagingController,
) : SentAliasHistory {
    override fun candidates(accountId: String, recipients: Set<String>): Set<String> {
        val account = requireNotNull(accounts.getAccount(accountId))
        val folder = account.sentFolderId ?: return emptySet()
        return stores.getMessageStore(account).getMessages(
            selection = "folder_id = ? AND deleted = 0 AND empty = 0",
            selectionArgs = arrayOf(folder.toString()),
            sortOrder = "date DESC",
        ) { message ->
            val related = (message.toAddresses + message.ccAddresses).any { it.address.lowercase() in recipients }
            if (related) message.fromAddresses.map { it.address }.toSet() else emptySet()
        }.flatten().toSet()
    }

    override suspend fun searchRemote(accountId: String, recipient: String) = withContext(Dispatchers.IO) {
        val account = requireNotNull(accounts.getAccount(accountId))
        val folder = requireNotNull(account.sentFolderId)
        val search = controller.searchRemoteMessages(
            accountId,
            folder,
            recipient,
            emptySet<Flag>(),
            emptySet<Flag>(),
            null,
        )
        try {
            search.get(REMOTE_SEARCH_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            Unit
        } finally {
            if (!search.isDone) search.cancel(true)
        }
    }

    private companion object {
        const val REMOTE_SEARCH_TIMEOUT_SECONDS = 60L
    }
}
