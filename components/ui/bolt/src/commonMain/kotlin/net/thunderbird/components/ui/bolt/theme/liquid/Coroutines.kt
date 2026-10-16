/* Copyright 2025 Kyant. Apache-2.0.
 * Adaptation: use Compose frame clock for both project targets.
 */
package net.thunderbird.components.ui.bolt.theme.liquid

import androidx.compose.runtime.withFrameNanos

internal suspend fun awaitFrame() {
    withFrameNanos { }
}
