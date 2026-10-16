package com.fsck.k9.view

/** Presentation-only pass after the existing sanitizer; no scripts, requests or server message mutations. */
internal object GlassMailBackground {
    fun apply(html: String, dark: Boolean = false): String =
        app.k9mail.html.cleaner.TransparentMessageBackground.apply(html, dark)
}
