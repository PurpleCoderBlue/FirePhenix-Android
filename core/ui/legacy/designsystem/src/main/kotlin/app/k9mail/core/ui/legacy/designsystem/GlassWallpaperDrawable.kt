package app.k9mail.core.ui.legacy.designsystem

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import java.lang.ref.WeakReference
import kotlin.math.max

/** Draws one wallpaper crop in window coordinates, including independently scrolling content hosts. */
class GlassWallpaperDrawable(view: View, private val image: Bitmap) : Drawable() {
    private val anchor = WeakReference(view)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG or Paint.DITHER_FLAG)
    private val destination = RectF()
    private val location = IntArray(2)

    override fun draw(canvas: Canvas) {
        val view = anchor.get() ?: return
        val width = view.rootView.width.coerceAtLeast(view.width)
        val height = view.rootView.height.coerceAtLeast(view.height)
        if (width == 0 || height == 0) return
        view.getLocationInWindow(location)
        val scale = max(width.toFloat() / image.width, height.toFloat() / image.height)
        val drawWidth = image.width * scale
        val drawHeight = image.height * scale
        destination.set(
            (width - drawWidth) / 2f - location[0],
            (height - drawHeight) / 2f - location[1],
            (width + drawWidth) / 2f - location[0],
            (height + drawHeight) / 2f - location[1],
        )
        canvas.drawBitmap(image, null, destination, paint)
    }

    override fun setAlpha(alpha: Int) {
        paint.alpha = alpha
    }
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Android")
    override fun getOpacity(): Int = PixelFormat.OPAQUE
}
