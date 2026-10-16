package net.thunderbird.core.ui.theme.manager

import android.app.Application
import android.util.TypedValue
import app.k9mail.core.ui.legacy.designsystem.R
import assertk.assertThat
import assertk.assertions.isTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = Application::class)
class GlassPopupThemesTest {
    @Test
    fun `both glass dialog themes remain floating dialogs`() {
        val context = RuntimeEnvironment.getApplication()
        for (style in listOf(R.style.FirePhenixGlassDialogLight, R.style.FirePhenixGlassDialogDark)) {
            val theme = context.resources.newTheme().apply { applyStyle(style, true) }
            val value = TypedValue()
            assertThat(theme.resolveAttribute(android.R.attr.windowIsFloating, value, true)).isTrue()
            assertThat(value.data != 0).isTrue()
        }
    }
}
