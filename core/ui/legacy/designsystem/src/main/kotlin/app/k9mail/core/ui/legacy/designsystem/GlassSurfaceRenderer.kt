package app.k9mail.core.ui.legacy.designsystem

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.annotation.RequiresApi
import app.k9mail.core.ui.legacy.designsystem.glass.DEFAULT_HIGHLIGHT_SHADER
import app.k9mail.core.ui.legacy.designsystem.glass.ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER
import java.lang.ref.WeakReference
import java.util.WeakHashMap
import kotlin.math.PI

/**
 * View bridge for the same Apache-2.0 Backdrop optics used by Bolt.
 * Records the live app into a GPU display list, excluding every glass control's
 * foreground and external rendering surfaces (including email WebView).
 * No readback, screenshot file, network request or permission is used.
 */
class GlassSurfaceRenderer(private val root: ViewGroup) {
    private val renderer = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) OpticalRenderer(root) else null

    fun configure(dark: Boolean, blur: Int) {
        active = WeakReference(this)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) renderer?.configure(dark, blur)
    }

    fun material(view: View, color: Int, pill: Boolean, showHighlight: Boolean = true): Drawable {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            renderer?.material(view, color, pill, showHighlight)?.let { return it }
        }
        return GradientDrawable().apply {
            cornerRadius = root.resources.displayMetrics.density *
                if (pill) NativeGlassSpec.FALLBACK_PILL_RADIUS else NativeGlassSpec.CORNER_RADIUS
            setColor((color and NativeGlassSpec.RGB_MASK) or NativeGlassSpec.FALLBACK_ALPHA)
            if (showHighlight) {
                setStroke(
                    root.resources.displayMetrics.density.toInt().coerceAtLeast(1),
                    NativeGlassSpec.FALLBACK_STROKE,
                )
            }
        }
    }

    fun close() {
        if (active.get() === this) active.clear()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) renderer?.close()
    }

    companion object {
        private var active = WeakReference<GlassSurfaceRenderer>(null)

        internal fun popupMaterial(view: View): Drawable? {
            val renderer = active.get() ?: return null
            val color = android.util.TypedValue()
            view.context.theme.resolveAttribute(android.R.attr.colorBackground, color, true)
            return renderer.material(view, color.data, false, showHighlight = false)
        }
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class OpticalRenderer(private val root: ViewGroup) : ViewTreeObserver.OnPreDrawListener {
    val source = RenderNode("FirePhenix live glass backdrop")
    private val targets = WeakHashMap<View, WeakReference<OpticalDrawable>>()
    private val backdrop = GlassBackdropCapture(root)
    private var capturing = false
    val isCapturing: Boolean get() = capturing
    var dark = false
        private set
    var blur = NativeGlassSpec.DEFAULT_BLUR
        private set

    init {
        root.viewTreeObserver.addOnPreDrawListener(this)
    }

    fun configure(dark: Boolean, blur: Int) {
        if (this.dark == dark && this.blur == blur) return
        this.dark = dark
        this.blur = blur.coerceIn(0, NativeGlassSpec.MAX_BLUR)
        targets.values.forEach { it.get()?.invalidateSelf() }
    }

    fun material(view: View, color: Int, pill: Boolean, showHighlight: Boolean): Drawable {
        return OpticalDrawable(this, root, view, color, pill, showHighlight).also {
            targets[view] = WeakReference(it)
            root.invalidate()
        }
    }

    override fun onPreDraw(): Boolean {
        val ready = root.width > 0 && root.height > 0
        val sampling = capturing || GlassBackdropCapture.isCapturing
        if (sampling || targets.isEmpty() || !ready) return true
        // RecyclerView reattaches controls with the same background; retain their weak registration.
        capturing = true
        try {
            source.setPosition(0, 0, root.width, root.height)
            val canvas = source.beginRecording()
            try {
                canvas.drawColor(if (dark) NativeGlassSpec.DARK_BACKGROUND else NativeGlassSpec.LIGHT_BACKGROUND)
                backdrop.draw(canvas, targets.keys.toSet())
            } finally {
                source.endRecording()
            }
        } finally {
            capturing = false
        }
        return true
    }

    fun close() {
        root.viewTreeObserver.removeOnPreDrawListener(this)
        targets.values.forEach { it.get()?.close() }
        targets.clear()
        source.discardDisplayList()
    }
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
private class OpticalDrawable(
    private val renderer: OpticalRenderer,
    private val root: View,
    private val target: View,
    private val tint: Int,
    private val pill: Boolean,
    private val showHighlight: Boolean,
) : Drawable(), GlassOpticalMaterial {
    private val region = RenderNode("FirePhenix glass control")
    private val lens = RuntimeShader(ROUNDED_RECT_REFRACTION_WITH_DISPERSION_SHADER)
    private val highlight = RuntimeShader(DEFAULT_HIGHLIGHT_SHADER)
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val clip = Path()
    private val rect = RectF()
    private val rootLocation = IntArray(2)
    private val targetLocation = IntArray(2)
    private var progress = 0f
    private var animation: ValueAnimator? = null
    private var configured = ""

    override fun draw(canvas: Canvas) {
        // Final guard for custom wrappers: the source must never reference one of its own regions.
        val sampling = GlassBackdropCapture.isCapturing || renderer.isCapturing
        if (sampling || !canvas.isHardwareAccelerated || !renderer.source.hasDisplayList()) {
            return
        }
        val density = target.resources.displayMetrics.density
        val inset = if (pill && target is ViewGroup) NativeGlassSpec.TOOLBAR_INSET * density else density
        val width = bounds.width() - inset * 2f
        val height = bounds.height() - inset * 2f
        if (width <= 0 || height <= 0) return
        val radius = if (pill) height / 2f else minOf(NativeGlassSpec.CORNER_RADIUS * density, height / 2f)
        root.getLocationOnScreen(rootLocation)
        target.getLocationOnScreen(targetLocation)
        val recording = region.beginRecording(width.toInt(), height.toInt())
        recording.translate(
            -(targetLocation[0] - rootLocation[0] + inset),
            -(targetLocation[1] - rootLocation[1] + inset),
        )
        recording.drawRenderNode(renderer.source)
        region.endRecording()
        region.setPosition(0, 0, width.toInt(), height.toInt())
        configure(width, height, radius, density)
        val saved = canvas.save()
        canvas.translate(bounds.left + inset, bounds.top + inset)
        rect.set(0f, 0f, width, height)
        clip.reset()
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.clipPath(clip)
        // Keep the unfiltered source from showing through translucent pixels of the optical layer.
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = tint
        paint.alpha = NativeGlassSpec.OPAQUE_ALPHA
        canvas.drawRoundRect(rect, radius, radius, paint)
        canvas.drawRenderNode(region)
        paint.shader = null
        paint.style = Paint.Style.FILL
        paint.color = tint
        val channels = listOf(Color.red(tint), Color.green(tint), Color.blue(tint))
        paint.alpha = if (channels.max() - channels.min() > NativeGlassSpec.SATURATED_THRESHOLD) {
            NativeGlassSpec.COLORED_TINT_ALPHA
        } else {
            val level = renderer.blur / NativeGlassSpec.MAX_BLUR.toFloat()
            (NativeGlassSpec.CLEAR_TINT_ALPHA + NativeGlassSpec.FROST_TINT_ALPHA_RANGE * level).toInt()
        }
        canvas.drawRoundRect(rect, radius, radius, paint)
        if (showHighlight) {
            paint.shader = highlight
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = density * NativeGlassSpec.HIGHLIGHT_WIDTH
            paint.alpha = NativeGlassSpec.HIGHLIGHT_ALPHA
            canvas.drawRoundRect(rect, radius, radius, paint)
        }
        canvas.restoreToCount(saved)
    }

    private fun configure(width: Float, height: Float, radius: Float, density: Float) {
        val key = "$width/$height/$radius/${renderer.blur}/$progress"
        if (configured == key) return
        configured = key
        lens.setFloatUniform("size", width, height)
        lens.setFloatUniform("offset", 0f, 0f)
        lens.setFloatUniform("cornerRadii", radius, radius, radius, radius)
        lens.setFloatUniform("refractionHeight", minOf(NativeGlassSpec.LENS_HEIGHT * density, height / 2f))
        lens.setFloatUniform(
            "refractionAmount",
            -NativeGlassSpec.LENS_AMOUNT * density * (1f + progress * NativeGlassSpec.PRESS_REFRACTION),
        )
        lens.setFloatUniform("depthEffect", 1f)
        lens.setFloatUniform("chromaticAberration", 1f)
        val optical = RenderEffect.createRuntimeShaderEffect(lens, "content")
        val color = RenderEffect.createColorFilterEffect(
            ColorMatrixColorFilter(
                ColorMatrix().apply {
                    setSaturation(NativeGlassSpec.SATURATION)
                },
            ),
        )
        val level = renderer.blur / NativeGlassSpec.MAX_BLUR.toFloat()
        val blur = NativeGlassSpec.MAX_BLUR_RADIUS_DP * level * level * density
        val background = if (blur > 0f) {
            RenderEffect.createBlurEffect(blur, blur, color, Shader.TileMode.CLAMP)
        } else {
            color
        }
        region.setRenderEffect(RenderEffect.createChainEffect(optical, background))
        highlight.setFloatUniform("size", width, height)
        highlight.setFloatUniform("cornerRadii", radius, radius, radius, radius)
        highlight.setColorUniform("color", Color.WHITE)
        highlight.setFloatUniform(
            "angle",
            (
                (NativeGlassSpec.LIGHT_ANGLE + progress * NativeGlassSpec.PRESS_LIGHT_ANGLE) *
                    PI /
                    NativeGlassSpec.HALF_TURN
                ).toFloat(),
        )
        highlight.setFloatUniform("falloff", NativeGlassSpec.LIGHT_FALLOFF)
    }

    override fun getOutline(outline: android.graphics.Outline) {
        val density = target.resources.displayMetrics.density
        val inset = if (pill &&
            target is ViewGroup
        ) {
            (NativeGlassSpec.TOOLBAR_INSET * density).toInt()
        } else {
            density.toInt()
        }
        val rect = android.graphics.Rect(bounds)
        rect.inset(inset, inset)
        outline.setRoundRect(rect, if (pill) rect.height() / 2f else NativeGlassSpec.CORNER_RADIUS * density)
    }

    override fun isStateful(): Boolean = true

    override fun onStateChange(state: IntArray): Boolean {
        val pressed = state.contains(android.R.attr.state_pressed) || state.contains(android.R.attr.state_focused)
        animation?.cancel()
        animation = ValueAnimator.ofFloat(progress, if (pressed) 1f else 0f).apply {
            duration = NativeGlassSpec.PRESS_DURATION
            addUpdateListener {
                progress = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
        return true
    }

    override fun setAlpha(alpha: Int) = Unit
    override fun setColorFilter(colorFilter: ColorFilter?) = Unit

    @Deprecated("Deprecated in Java")
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun close() {
        animation?.cancel()
        region.discardDisplayList()
    }
}

private object NativeGlassSpec {
    const val DEFAULT_BLUR = 16
    const val MAX_BLUR = 32
    const val FALLBACK_PILL_RADIUS = 28f
    const val CORNER_RADIUS = 24f
    const val TOOLBAR_INSET = 6f
    const val RGB_MASK = 0x00FFFFFF
    val FALLBACK_ALPHA = 0xDD000000.toInt()
    const val FALLBACK_STROKE = 0x66FFFFFF
    val DARK_BACKGROUND = 0xFF15171D.toInt()
    val LIGHT_BACKGROUND = 0xFFF8F8FC.toInt()
    const val SATURATED_THRESHOLD = 45
    const val OPAQUE_ALPHA = 255
    const val COLORED_TINT_ALPHA = 200
    const val HIGHLIGHT_ALPHA = 210
    const val HIGHLIGHT_WIDTH = 1.3f
    const val LENS_HEIGHT = 16f
    const val LENS_AMOUNT = 24f
    const val PRESS_REFRACTION = 0.12f
    const val SATURATION = 1.25f
    const val MAX_BLUR_RADIUS_DP = 96f
    const val CLEAR_TINT_ALPHA = 20
    const val FROST_TINT_ALPHA_RANGE = 179
    const val LIGHT_ANGLE = 45f
    const val PRESS_LIGHT_ANGLE = 20f
    const val HALF_TURN = 180f
    const val LIGHT_FALLOFF = 1.4f
    const val PRESS_DURATION = 180L
}
