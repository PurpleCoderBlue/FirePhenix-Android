package app.k9mail.legacy.ui.folder

import android.content.res.Configuration
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.util.Locale
import kotlin.test.Test
import net.thunderbird.feature.mail.folder.api.Folder
import net.thunderbird.feature.mail.folder.api.FolderType
import net.thunderbird.feature.mail.folder.api.RemoteFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class FolderNameFormatterTest {
    private val formatter: FolderNameFormatter
        get() {
            val configuration = Configuration().apply { setLocale(Locale.SIMPLIFIED_CHINESE) }
            val context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration)
            return FolderNameFormatter(context.resources)
        }

    @Test
    fun `remote semantic folders use Chinese UI names`() {
        for ((type, label) in labels) {
            val folder = Folder(id = 1, name = "Server Folder", type = type, isLocalOnly = false)
            assertThat(formatter.displayName(folder)).isEqualTo(label)
            val remote = RemoteFolder(id = 1, serverId = "remote-id", name = "Server Folder", type = type)
            assertThat(formatter.displayName(remote)).isEqualTo(label)
        }
    }

    @Test
    fun `local folders use the same localized role names`() {
        for ((type, label) in labels) {
            val folder = Folder(id = 1, name = "Local Folder", type = type, isLocalOnly = true)
            assertThat(formatter.displayName(folder)).isEqualTo(label)
        }
    }

    @Test
    fun `user folder names and server identifiers are retained`() {
        val folder = RemoteFolder(1, "Drafts/Research", "Drafts/Research", FolderType.REGULAR)
        assertThat(formatter.displayName(folder)).isEqualTo("Drafts/Research")
        assertThat(folder.serverId).isEqualTo("Drafts/Research")
    }

    private companion object {
        val labels = mapOf(
            FolderType.INBOX to "收件箱",
            FolderType.OUTBOX to "发件箱",
            FolderType.DRAFTS to "草稿",
            FolderType.SENT to "已发送邮件",
            FolderType.TRASH to "已删除邮件",
            FolderType.ARCHIVE to "归档",
            FolderType.SPAM to "垃圾邮件",
        )
    }
}
