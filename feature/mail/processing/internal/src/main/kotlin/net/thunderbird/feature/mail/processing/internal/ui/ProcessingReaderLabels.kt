package net.thunderbird.feature.mail.processing.internal.ui

import net.thunderbird.feature.mail.processing.internal.R

internal fun translationActionLabel(state: ProcessingReaderState): Int = when {
    state.translation.busy -> R.string.processing_translating
    state.mode != ReadingMode.ORIGINAL -> R.string.processing_original
    else -> R.string.processing_translate
}

internal fun summaryActionLabel(state: ProcessingReaderState): Int = when {
    state.summary.busy -> R.string.processing_summarizing
    state.summary.result == null -> R.string.processing_summary
    state.summaryExpanded -> R.string.processing_hide_summary
    else -> R.string.processing_show_summary
}
