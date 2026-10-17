package com.fsck.k9.ui.messageview

import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.fsck.k9.ui.R
import com.google.android.material.button.MaterialButton

/** Reparents existing actions only in glass mode; their listeners and visibility state stay intact. */
internal object GlassReaderHeader {
    private const val ACTION_SIZE_DP = 48
    private const val PROCESSING_WIDTH_DP = 240
    private const val COMPACT_WIDTH_DP = 600
    private const val LARGE_FONT_SCALE = 1.3f
    private const val CONTENT_MARGIN_DP = 16

    fun apply(root: View, enabled: Boolean) {
        if (!enabled) return
        val size = (ACTION_SIZE_DP * root.resources.displayMetrics.density).toInt()
        val subject = root.findViewById<View>(R.id.subject)
        val actions = root.findViewById<View>(R.id.mail_processing_actions)
        val download = root.findViewById<MaterialButton>(R.id.download_remainder)
        compact(download, app.k9mail.core.ui.legacy.designsystem.R.drawable.ic_download, size)
        configureSubjectActions(root, subject, actions)

        val participants = root.findViewById<ConstraintLayout>(R.id.participants_container)
        val remote = root.findViewById<ViewGroup>(R.id.extra_header_container)
        val images = root.findViewById<MaterialButton>(R.id.show_pictures)
        compact(images, app.k9mail.core.ui.legacy.designsystem.R.drawable.ic_image, size)
        remote.setPadding(0, 0, 0, 0)
        (download.parent as ViewGroup).removeView(download)
        participants.addView(
            download,
            ConstraintLayout.LayoutParams(size, size).apply {
                topToTop = R.id.menu_primary_action
                endToStart = R.id.menu_primary_action
            },
        )
        (remote.parent as ViewGroup).removeView(remote)
        participants.addView(
            remote,
            ConstraintLayout.LayoutParams(size, size).apply {
                topToTop = R.id.menu_primary_action
                endToStart = download.id
            },
        )
        listOf(R.id.date, R.id.from_address, R.id.recipients).forEach { id ->
            (root.findViewById<View>(id).layoutParams as ConstraintLayout.LayoutParams).endToStart = remote.id
        }
    }

    private fun configureSubjectActions(root: View, subject: View, actions: View) {
        val config = root.resources.configuration
        val compact = config.screenWidthDp < COMPACT_WIDTH_DP || config.fontScale > LARGE_FONT_SCALE
        val margin = (CONTENT_MARGIN_DP * root.resources.displayMetrics.density).toInt()
        (actions.layoutParams as ConstraintLayout.LayoutParams).apply {
            width = if (compact) 0 else (PROCESSING_WIDTH_DP * root.resources.displayMetrics.density).toInt()
            if (compact) {
                startToStart = ConstraintLayout.LayoutParams.PARENT_ID
                endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
                endToStart = ConstraintLayout.LayoutParams.UNSET
                topToTop = ConstraintLayout.LayoutParams.UNSET
                topToBottom = subject.id
                bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
                marginStart = margin
                marginEnd = margin
            } else {
                endToStart = R.id.flagged
            }
        }
        (subject.layoutParams as ConstraintLayout.LayoutParams).apply {
            endToStart = if (compact) R.id.flagged else actions.id
            if (compact) bottomToBottom = ConstraintLayout.LayoutParams.UNSET
        }
    }

    private fun compact(button: MaterialButton, icon: Int, size: Int) {
        button.contentDescription = button.text
        button.text = ""
        button.setIconResource(icon)
        button.iconPadding = 0
        button.iconSize = size / 2
        button.iconGravity = MaterialButton.ICON_GRAVITY_TEXT_START
        button.gravity = android.view.Gravity.CENTER
        button.minWidth = 0
        button.minHeight = 0
        button.minimumWidth = 0
        button.minimumHeight = 0
        button.insetTop = 0
        button.insetBottom = 0
        val padding = size / 4
        button.setPadding(padding, padding, padding, padding)
        button.layoutParams = button.layoutParams.apply {
            width = size
            height = size
        }
    }
}
