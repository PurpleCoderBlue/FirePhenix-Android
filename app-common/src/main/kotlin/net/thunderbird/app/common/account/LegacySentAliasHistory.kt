package net.thunderbird.app.common.account

import app.k9mail.legacy.mailstore.MessageStoreManager
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.account.settings.alias.SentAliasHistory

/** Adapts the existing sent folder; no new mail backend or OAuth scopes. */
internal class LegacySentAliasHistory(
    private val accounts: LegacyAccountDtoManager,
    private val stores: MessageStoreManager,
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

    override fun localCandidates(accountId: String): Set<String> {
        val account = requireNotNull(accounts.getAccount(accountId))
        val result = mutableSetOf<String>()
        stores.getMessageStore(account).getMessages<Nothing>(
            selection = "deleted = 0 AND empty = 0",
            selectionArgs = emptyArray(),
            sortOrder = "date DESC",
        ) { message ->
            val addresses = if (message.folderId == account.sentFolderId) {
                message.fromAddresses
            } else {
                message.toAddresses + message.ccAddresses
            }
            addresses.forEach { result.add(it.address) }
            null
        }
        return result
    }
}
