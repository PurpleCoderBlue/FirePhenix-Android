package net.thunderbird.core.ui.theme.manager

import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.CompoundButton
import android.widget.TextView
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import net.thunderbird.components.ui.bolt.atom.Switch
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

/** Keeps AndroidX Preference's original checkable/listener as the single source of truth. */
internal class GlassPreferenceToggle(
    private val button: CompoundButton,
    private val theme: FeatureThemeProvider,
) : ViewTreeObserver.OnPreDrawListener {
    private val parent = button.parent as ViewGroup
    private val originalVisibility = button.visibility
    private val originalAccessibility = button.importantForAccessibility
    private val observer = button.viewTreeObserver
    private var checked by mutableStateOf(button.isChecked)
    private var enabled by mutableStateOf(button.isEnabled)
    private var label by mutableStateOf("")
    private val compose = ComposeView(button.context).apply {
        tag = TOGGLE_TAG
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            theme.WithControlTheme {
                Switch(
                    checked = checked,
                    enabled = enabled,
                    onCheckedChange = { selected -> select(selected) },
                    modifier = Modifier.semantics(mergeDescendants = true) {
                        contentDescription = label
                        role = Role.Switch
                        toggleableState = ToggleableState(checked)
                        if (!enabled) disabled()
                        onClick {
                            select(!checked)
                            enabled
                        }
                    },
                )
            }
        }
    }

    init {
        button.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        button.visibility = View.GONE
        val density = button.resources.displayMetrics.density
        parent.addView(compose, ViewGroup.LayoutParams((WIDTH_DP * density).toInt(), (HEIGHT_DP * density).toInt()))
        observer.addOnPreDrawListener(this)
        synchronize()
    }

    private fun synchronize() {
        checked = button.isChecked
        enabled = button.isEnabled
        val row = parent.parent as? View
        label = row?.findViewById<TextView>(android.R.id.title)?.text?.toString().orEmpty()
        button.visibility = View.GONE
    }

    private fun select(selected: Boolean) {
        if (button.isEnabled && selected != button.isChecked) {
            // Preserve Preference's original veto, persistence and listener.
            button.performClick()
            synchronize()
        }
    }

    override fun onPreDraw(): Boolean {
        synchronize()
        return true
    }

    fun close() {
        if (observer.isAlive) observer.removeOnPreDrawListener(this)
        compose.disposeComposition()
        parent.removeView(compose)
        button.visibility = originalVisibility
        button.importantForAccessibility = originalAccessibility
    }

    companion object {
        const val TOGGLE_TAG = "firephenix-preference-liquid-toggle"
        private const val WIDTH_DP = 64
        private const val HEIGHT_DP = 48
    }
}
