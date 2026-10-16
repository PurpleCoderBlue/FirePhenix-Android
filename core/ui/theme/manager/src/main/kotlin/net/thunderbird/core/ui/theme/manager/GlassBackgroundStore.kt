package net.thunderbird.core.ui.theme.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import androidx.annotation.RequiresApi
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.thunderbird.core.preference.display.coreSettings.GlassBackground

/** App-private static wallpapers; the glass material remains the published optical renderer. */
object GlassBackgroundStore {
    private var cacheKey: String? = null
    private var cached: Bitmap? = null
    private var seedKey: String? = null
    private var seedColor = 0

    @Synchronized
    fun load(context: Context, style: GlassBackground, image: String, dark: Boolean, blur: Int = 12): Bitmap {
        val key = "$style/$image/$dark/${blur.coerceIn(0, 512)}"
        if (cacheKey == key) cached?.let { return it }
        val bitmap = source(context, style, image, dark)
        val processed = GlassBackgroundProcessor.process(bitmap, dark, blur)
        if (processed !== bitmap) bitmap.recycle()
        cacheKey = key
        cached = processed
        return processed
    }

    /** Samples the original image, before blur or light/dark readability overlays. */
    @Synchronized
    fun color(context: Context, style: GlassBackground, image: String): Int {
        val key = "$style/$image"
        if (seedKey == key) return seedColor
        val bitmap = source(context, style, image, dark = false)
        seedColor = GlassBackgroundColors.dominantColor(bitmap)
        bitmap.recycle()
        seedKey = key
        return seedColor
    }

    private fun source(context: Context, style: GlassBackground, image: String, dark: Boolean): Bitmap =
        (
            if (style == GlassBackground.CUSTOM && hasImage(context, image)) {
                BitmapFactory.decodeFile(imageFile(context, image).absolutePath)
            } else {
                null
            }
            ) ?: preset(if (style == GlassBackground.CUSTOM) GlassBackground.BLUE else style, dark)

    fun hasImage(context: Context, image: String): Boolean =
        IMAGE_NAME.matches(image) && imageFile(context, image).isFile

    /** Copies the selected URI once; no broad storage or persistent URI permission is requested. */
    suspend fun importImage(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val folder = directory(context).apply { check(isDirectory || mkdirs()) }
        val temporary = File.createTempFile("import-", ".image", folder)
        try {
            context.contentResolver.openInputStream(uri).use { input ->
                requireNotNull(input)
                temporary.outputStream().use { output ->
                    val buffer = ByteArray(BUFFER_BYTES)
                    var total = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        total += count
                        require(total <= MAX_INPUT_BYTES) { "Image exceeds import limit" }
                        output.write(buffer, 0, count)
                    }
                }
            }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(temporary.absolutePath, bounds)
            require(bounds.outWidth > 0 && bounds.outHeight > 0) { "Unreadable image" }
            require(bounds.outWidth.toLong() * bounds.outHeight <= MAX_SOURCE_PIXELS) {
                "Image dimensions exceed limit"
            }
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                decodeModern(temporary)
            } else {
                decodeLegacy(temporary, bounds)
            }
            val bytes = ByteArrayOutputStream().use { stream ->
                check(bitmap.compress(Bitmap.CompressFormat.PNG, PNG_QUALITY, stream))
                stream.toByteArray()
            }
            bitmap.recycle()
            val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
                .joinToString("") { "%02x".format(it) }
            val name = "$digest.png"
            val target = imageFile(context, name)
            if (!target.isFile) {
                val atomic = AtomicFile(target)
                val stream = atomic.startWrite()
                try {
                    stream.write(bytes)
                    atomic.finishWrite(stream)
                } catch (error: java.io.IOException) {
                    atomic.failWrite(stream)
                    throw error
                }
            }
            name
        } finally {
            temporary.delete()
        }
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun decodeModern(file: File): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB))
            val scale = minOf(1f, MAX_EDGE.toFloat() / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(
                (info.size.width * scale).toInt().coerceAtLeast(1),
                (info.size.height * scale).toInt().coerceAtLeast(1),
            )
            decoder.setOnPartialImageListener { false }
        }

    private fun decodeLegacy(file: File, bounds: BitmapFactory.Options): Bitmap {
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > MAX_EDGE) sample *= 2
        return requireNotNull(
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply {
                    inSampleSize = sample
                    inPreferredConfig = Bitmap.Config.ARGB_8888
                },
            ),
        )
    }

    private fun preset(style: GlassBackground, dark: Boolean): Bitmap {
        val base = when (style) {
            GlassBackground.WARM -> if (dark) WARM_DARK else WARM_LIGHT
            else -> if (dark) BLUE_DARK else BLUE_LIGHT
        }
        if (style != GlassBackground.PHOENIX) {
            return Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888).apply { eraseColor(base) }
        }
        return Bitmap.createBitmap(PRESET_SIZE, PRESET_SIZE, Bitmap.Config.ARGB_8888).apply {
            val canvas = Canvas(this)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)
            canvas.drawColor(base)
            paint.color = if (dark) PHOENIX_DARK else PHOENIX_LIGHT
            canvas.drawRoundRect(PHOENIX_BOUNDS, PRESET_RADIUS, PRESET_RADIUS, paint)
            paint.color = if (dark) TEAL_DARK else TEAL_LIGHT
            canvas.drawRoundRect(TEAL_BOUNDS, PRESET_RADIUS, PRESET_RADIUS, paint)
        }
    }

    private fun directory(context: Context) = File(context.noBackupFilesDir, "glass-backgrounds")
    private fun imageFile(context: Context, image: String) = File(directory(context), image)

    private val IMAGE_NAME = Regex("[a-f0-9]{64}\\.png")
    private val PHOENIX_BOUNDS = RectF(PHOENIX_LEFT, PHOENIX_TOP, PHOENIX_RIGHT, PHOENIX_BOTTOM)
    private val TEAL_BOUNDS = RectF(TEAL_LEFT, TEAL_TOP, TEAL_RIGHT, TEAL_BOTTOM)
    private const val PHOENIX_LEFT = -180f
    private const val PHOENIX_TOP = -80f
    private const val PHOENIX_RIGHT = 480f
    private const val PHOENIX_BOTTOM = 660f
    private const val TEAL_LEFT = 340f
    private const val TEAL_TOP = 260f
    private const val TEAL_RIGHT = 860f
    private const val TEAL_BOTTOM = 860f
    private const val PRESET_RADIUS = 180f
    private const val MAX_EDGE = 2560
    private const val MAX_SOURCE_PIXELS = 100_000_000L
    private const val MAX_INPUT_BYTES = 32L * 1024 * 1024
    private const val BUFFER_BYTES = 8192
    private const val PNG_QUALITY = 100
    private const val PRESET_SIZE = 720
    private val BLUE_DARK = 0xFF132D45.toInt()
    private val BLUE_LIGHT = 0xFFE8F0F8.toInt()
    private val WARM_DARK = 0xFF2D2928.toInt()
    private val WARM_LIGHT = 0xFFF3EEE6.toInt()
    private val PHOENIX_DARK = 0xFF623F45.toInt()
    private val PHOENIX_LIGHT = 0xFFE5A7A0.toInt()
    private val TEAL_DARK = 0xFF23575B.toInt()
    private val TEAL_LIGHT = 0xFF9CC9C7.toInt()
}
