package app.k9mail.core.ui.legacy.designsystem

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.View
import kotlin.math.max

/** Background image only; shared optical materials sample it without blurring content. */
class GlassBackdropView(context: Context) : View(context) {
    private var image: Bitmap? = null
    private var dark = false
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val destination = RectF()
    private val location = IntArray(2)

    init {
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        isClickable = false
        isFocusable = false
    }

    fun configure(dark: Boolean, image: Bitmap? = this.image) {
        if (this.dark == dark && this.image === image) return
        this.dark = dark
        this.image = image
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(if (dark) BLUE_DARK else BLUE_LIGHT)
        val bitmap = image ?: return
        val windowWidth = rootView.width.coerceAtLeast(width)
        val windowHeight = rootView.height.coerceAtLeast(height)
        getLocationInWindow(location)
        val scale = max(windowWidth.toFloat() / bitmap.width, windowHeight.toFloat() / bitmap.height)
        val drawWidth = bitmap.width * scale
        val drawHeight = bitmap.height * scale
        destination.set(
            (windowWidth - drawWidth) / 2f - location[0],
            (windowHeight - drawHeight) / 2f - location[1],
            (windowWidth + drawWidth) / 2f - location[0],
            (windowHeight + drawHeight) / 2f - location[1],
        )
        canvas.drawBitmap(bitmap, null, destination, paint)
    }

    private companion object {
        val BLUE_DARK = 0xFF132D45.toInt()
        val BLUE_LIGHT = 0xFFE8F0F8.toInt()
    }
}
