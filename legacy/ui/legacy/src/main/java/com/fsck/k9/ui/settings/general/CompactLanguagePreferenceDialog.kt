package com.fsck.k9.ui.settings.general

import android.view.Gravity
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.preference.ListPreference
import androidx.preference.ListPreferenceDialogFragmentCompat

/** The existing language values and persistence, in a bounded scrollable glass dialog. */
class CompactLanguagePreferenceDialog : ListPreferenceDialogFragmentCompat() {
    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        val width = minOf(
            (MAX_WIDTH_DP * metrics.density).toInt(),
            metrics.widthPixels - (MARGIN_DP * metrics.density).toInt(),
        )
        dialog?.window?.apply {
            setGravity(Gravity.CENTER)
            setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        (dialog as? AlertDialog)?.listView?.apply {
            layoutParams = layoutParams.apply {
                height =
                    minOf(
                        (MAX_LIST_HEIGHT_DP * metrics.density).toInt(),
                        (metrics.heightPixels * HEIGHT_FRACTION).toInt(),
                    )
            }
            val language = preference as ListPreference
            setSelection(language.findIndexOfValue(language.value).coerceAtLeast(0))
        }
    }

    private companion object {
        const val MAX_WIDTH_DP = 480
        const val MAX_LIST_HEIGHT_DP = 360
        const val MARGIN_DP = 48
        const val HEIGHT_FRACTION = 0.55f
    }
}
