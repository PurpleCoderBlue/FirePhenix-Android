package app.k9mail.core.ui.legacy.designsystem

import android.content.Context
import android.util.TypedValue
import androidx.appcompat.content.res.AppCompatResources
import com.google.android.material.dialog.MaterialAlertDialogBuilder

/**
 * Reuses the original builder and all its actions. Original appearance executes
 * only the superclass constructor; glass substitutes the shared optical background.
 */
class LiquidGlassAlertDialogBuilder @JvmOverloads constructor(
    context: Context,
    overrideThemeResId: Int = 0,
) : MaterialAlertDialogBuilder(context, overrideThemeResId) {
    init {
        val enabled = TypedValue()
        if (context.theme.resolveAttribute(R.attr.firephenixLiquidGlass, enabled, true) && enabled.data != 0) {
            val dark = TypedValue()
            context.theme.resolveAttribute(R.attr.firephenixGlassDark, dark, true)
            getContext().theme.applyStyle(
                if (dark.data != 0) R.style.FirePhenixGlassDialogDark else R.style.FirePhenixGlassDialogLight,
                true,
            )
            setBackground(AppCompatResources.getDrawable(getContext(), R.drawable.firephenix_liquid_popup))
        }
    }
}
