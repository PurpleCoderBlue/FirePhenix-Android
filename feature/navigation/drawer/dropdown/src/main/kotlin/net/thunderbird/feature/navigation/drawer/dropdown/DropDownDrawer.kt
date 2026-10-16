package net.thunderbird.feature.navigation.drawer.dropdown

import android.view.MotionEvent
import android.view.ViewConfiguration
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider
import net.thunderbird.feature.navigation.drawer.api.NavigationDrawer
import net.thunderbird.feature.navigation.drawer.api.R
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayAccount
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.UnifiedDisplayFolderType
import net.thunderbird.feature.navigation.drawer.dropdown.domain.entity.createMailDisplayAccountFolderId
import net.thunderbird.feature.navigation.drawer.dropdown.ui.DrawerView
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

internal data class FolderDrawerState(
    val selectedAccountUuid: String? = null,
    val selectedFolderId: String? = null,
)

@Suppress("LongParameterList")
class DropDownDrawer(
    override val parent: AppCompatActivity,
    private val openAccount: (accountId: String) -> Unit,
    private val openFolder: (accountId: String, folderId: Long) -> Unit,
    private val openUnifiedFolder: () -> Unit,
    private val openManageFolders: () -> Unit,
    private val openSettings: () -> Unit,
    private val openAccountSettings: (accountId: String) -> Unit,
    private val openAddAccount: () -> Unit,
    createDrawerListener: () -> DrawerLayout.DrawerListener,
    motionDurationMillis: () -> Long,
    onMotion: (String) -> Unit = {},
) : NavigationDrawer, KoinComponent {

    private val themeProvider: FeatureThemeProvider by inject()
    private val drawer: DrawerLayout = parent.findViewById(R.id.navigation_drawer_layout)
    private val drawerContent: ComposeView = parent.findViewById(R.id.navigation_drawer_content)

    private val drawerState = MutableStateFlow(FolderDrawerState())
    private val motion = DrawerMotion(
        drawerContent,
        { isOpen },
        { open ->
            if (open) drawer.openDrawer(GravityCompat.START, false) else drawer.closeDrawer(GravityCompat.START, false)
        },
        motionDurationMillis,
        onMotion,
    )

    init {
        drawer.addDrawerListener(createDrawerListener())
        drawer.addDrawerListener(object : DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerStateChanged(newState: Int) {
                if (newState == DrawerLayout.STATE_DRAGGING) motion.cancel()
            }
        })
        installOutsideClose()

        // Make insets available to the drawer's Compose content
        ViewCompat.setOnApplyWindowInsetsListener(drawer) { _, insets ->
            drawerContent.dispatchApplyWindowInsets(insets.toWindowInsets())
            insets
        }

        drawerContent.setContent {
            themeProvider.WithTheme {
                val state = drawerState.collectAsStateWithLifecycle()

                DrawerView(
                    drawerState = state.value,
                    openAccount = openAccount,
                    openFolder = openFolder,
                    openUnifiedFolder = openUnifiedFolder,
                    openManageFolders = openManageFolders,
                    openSettings = openSettings,
                    openAccountSettings = openAccountSettings,
                    openAddAccount = openAddAccount,
                    closeDrawer = { close() },
                )
            }
        }
    }

    private fun installOutsideClose() {
        var downX = 0f
        var downY = 0f
        var outside = false
        val slop = ViewConfiguration.get(parent).scaledTouchSlop
        drawer.setOnTouchListener { view, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                downX = event.x
                downY = event.y
                outside = isOpen && (event.x < drawerContent.left || event.x > drawerContent.right)
            }
            val tap = kotlin.math.abs(event.x - downX) < slop && kotlin.math.abs(event.y - downY) < slop
            if (event.actionMasked == MotionEvent.ACTION_UP && outside && tap) {
                view.performClick()
                close()
                true
            } else {
                false
            }
        }
    }

    override val isOpen: Boolean
        get() = drawer.isDrawerOpen(GravityCompat.START)

    override fun selectAccount(accountUuid: String) {
        drawerState.update {
            it.copy(selectedAccountUuid = accountUuid)
        }
    }

    override fun selectFolder(accountUuid: String, folderId: Long) {
        drawerState.update {
            it.copy(
                selectedAccountUuid = accountUuid,
                selectedFolderId = createMailDisplayAccountFolderId(accountUuid, folderId),
            )
        }
    }

    override fun selectUnifiedInbox() {
        drawerState.update {
            it.copy(
                selectedAccountUuid = UnifiedDisplayAccount.UNIFIED_ACCOUNT_ID,
                selectedFolderId = UnifiedDisplayFolderType.INBOX.id,
            )
        }
    }

    override fun deselect() {
        drawerState.update {
            it.copy(
                selectedFolderId = null,
            )
        }
    }

    override fun open() {
        motion.show(true)
    }

    override fun close() {
        motion.show(false)
    }

    override fun lock() {
        motion.cancel()
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
    }

    override fun unlock() {
        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
    }
}
