package com.fsck.k9.ui.messagelist.item

import android.content.res.Resources.Theme
import androidx.annotation.ColorInt
import android.util.TypedValue
import com.fsck.k9.ui.resolveColorAttribute
import com.google.android.material.R as MaterialR

data class MessageViewHolderColors(
    @get:ColorInt
    val active: Int,
    @get:ColorInt
    val selected: Int,
    @get:ColorInt
    val regular: Int,
    @get:ColorInt
    val read: Int,
    @get:ColorInt
    val unread: Int,
    @get:ColorInt
    val previewText: Int,
    @get:ColorInt
    val previewActiveText: Int,
    @get:ColorInt
    val previewSelectedText: Int,
) {
    companion object Companion {
        fun resolveColors(theme: Theme): MessageViewHolderColors {
            return MessageViewHolderColors(
                active = theme.resolveColorAttribute(MaterialR.attr.colorOnSecondaryContainer),
                selected = theme.resolveColorAttribute(MaterialR.attr.colorOnSurface),
                regular = theme.resolveColorAttribute(MaterialR.attr.colorOnSurface),
                read = theme.resolveColorAttribute(
                    MaterialR.attr.colorOnSurface,
                ),
                unread = theme.resolveColorAttribute(MaterialR.attr.colorOnSurface),
                previewText = theme.resolveColorAttribute(
                    MaterialR.attr.colorOnSurface,
                ),
                previewActiveText = theme.resolveColorAttribute(MaterialR.attr.colorOnSecondary),
                previewSelectedText = theme.resolveColorAttribute(MaterialR.attr.colorOnSurface),
            )
        }
    }
}
