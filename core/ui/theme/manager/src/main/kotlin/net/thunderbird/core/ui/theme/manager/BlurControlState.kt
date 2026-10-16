package net.thunderbird.core.ui.theme.manager

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Input owns the draft until its final submitted value is acknowledged by preferences. */
internal class BlurControlState(initial: Int, private val maximum: Int) {
    var value by mutableIntStateOf(initial)
        private set
    var input by mutableStateOf(initial.toString())
        private set
    private var dragging = false
    private var submitted: Int? = null

    fun external(value: Int) {
        if (submitted == value) submitted = null
        if (!dragging && submitted == null) {
            this.value = value
            input = value.toString()
        }
    }

    fun drag(value: Int) {
        dragging = true
        this.value = value.coerceIn(0, maximum)
        input = this.value.toString()
    }

    fun type(input: String): Int? {
        this.input = input
        val number = input.toIntOrNull()?.takeIf { it in 0..maximum } ?: return null
        value = number
        return finish()
    }

    fun finish(): Int {
        dragging = false
        submitted = value
        return value
    }
}
