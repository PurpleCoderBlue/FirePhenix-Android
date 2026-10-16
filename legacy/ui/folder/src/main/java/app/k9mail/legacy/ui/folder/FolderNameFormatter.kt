package app.k9mail.legacy.ui.folder

import android.content.res.Resources
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder

class FolderNameFormatter(private val resources: Resources) {
    fun displayName(folder: Folder): String = displayName(folder.type, folder.name)

    fun displayName(folder: RemoteFolder): String = displayName(folder.type, folder.name)

    private fun displayName(type: FolderType, serverName: String): String = when (type) {
        FolderType.INBOX -> resources.getString(R.string.special_mailbox_name_inbox)
        FolderType.OUTBOX -> resources.getString(R.string.special_mailbox_name_outbox)
        FolderType.DRAFTS -> resources.getString(R.string.special_mailbox_name_drafts)
        FolderType.SENT -> resources.getString(R.string.special_mailbox_name_sent)
        FolderType.TRASH -> resources.getString(R.string.special_mailbox_name_trash)
        FolderType.ARCHIVE -> resources.getString(R.string.special_mailbox_name_archive)
        FolderType.SPAM -> resources.getString(R.string.special_mailbox_name_spam)
        FolderType.REGULAR -> serverName
    }
}
