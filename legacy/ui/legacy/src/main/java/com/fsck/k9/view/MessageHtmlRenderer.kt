package com.fsck.k9.view

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Owns latest-result delivery; expensive presentation parsing never runs on the rendering thread. */
internal class MessageHtmlRenderer(
    private val worker: CoroutineDispatcher = Dispatchers.Default,
    private val prepare: suspend (String, Boolean, Boolean) -> String = { html, glass, dark ->
        app.k9mail.html.cleaner.ViewportMessageLayout.apply(html, if (glass) dark else null)
    },
) {
    private var pending: Job? = null

    fun render(scope: CoroutineScope, html: String, glass: Boolean, dark: Boolean, ready: (String) -> Unit) {
        cancel()
        if (html.isBlank()) {
            ready(html)
            return
        }
        pending = scope.launch {
            val prepared = withContext(worker) { prepare(html, glass, dark) }
            ready(prepared)
        }
    }

    fun cancel() {
        pending?.cancel()
        pending = null
    }
}
