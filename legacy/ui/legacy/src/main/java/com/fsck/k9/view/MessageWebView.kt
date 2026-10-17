package com.fsck.k9.view

import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.ScrollView
import kotlin.math.abs
import kotlin.math.min
import android.util.TypedValue
import android.util.AttributeSet
import android.webkit.WebView
import androidx.lifecycle.findViewTreeLifecycleOwner
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import com.fsck.k9.core.BuildConfig
import com.fsck.k9.mailstore.AttachmentResolver
import kotlin.math.roundToInt
import net.thunderbird.core.android.common.view.showInDarkMode
import net.thunderbird.core.android.common.view.showInLightMode
import net.thunderbird.core.android.webkit.ThunderbirdWebViewSettings
import net.thunderbird.legacy.logging.Log
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MessageWebView : WebView, KoinComponent, ThunderbirdWebViewSettings {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context, attrs: AttributeSet?, defStyle: Int) : super(context, attrs, defStyle)

    private val webViewClientFactory: WebViewClientFactory by inject()
    internal var bodyViewportEnabled = false
    private var useDarkMode = false
    private val htmlRenderer = MessageHtmlRenderer()
    private val fallbackRenderScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override var loadWithOverviewMode: Boolean
        get() = settings.loadWithOverviewMode
        set(value) {
            settings.loadWithOverviewMode = value
        }

    private var gestureX = 0f
    private var gestureY = 0f

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (!bodyViewportEnabled) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec)
            return
        }
        var ancestor = parent
        var viewport = rootView.height.takeIf { it > 0 } ?: resources.displayMetrics.heightPixels
        while (ancestor is View) {
            if (ancestor is ScrollView && ancestor.height > 0) {
                viewport = min(viewport, ancestor.height)
                break
            }
            ancestor = ancestor.parent
        }
        val mode = MeasureSpec.getMode(heightMeasureSpec)
        val requested = MeasureSpec.getSize(heightMeasureSpec)
        val height = if (mode == MeasureSpec.UNSPECIFIED) viewport else min(requested, viewport)
        val boundedMode = if (mode == MeasureSpec.EXACTLY) MeasureSpec.EXACTLY else MeasureSpec.AT_MOST
        super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(height.coerceAtLeast(1), boundedMode))
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (bodyViewportEnabled) {
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    gestureX = event.x
                    gestureY = event.y
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = gestureX - event.x
                    val dy = gestureY - event.y
                    val owns = if (abs(dy) >= abs(dx)) canScrollVertically(dy.toInt())
                    else canScrollHorizontally(dx.toInt())
                    parent?.requestDisallowInterceptTouchEvent(owns)
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return super.onTouchEvent(event)
    }

    override fun blockNetworkData(shouldBlockNetworkData: Boolean) {
        // Images with content: URIs will not be blocked, nor will network images that are already in the WebView cache.
        try {
            settings.blockNetworkLoads = shouldBlockNetworkData
        } catch (e: SecurityException) {
            Log.e(e, "Failed to unblock network loads. Missing INTERNET permission?")
        }
    }

    override fun configure(config: WebViewConfig) {
        useDarkMode = config.useDarkMode
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
        scrollBarStyle = SCROLLBARS_INSIDE_OVERLAY
        isLongClickable = true

        // Do not force a view-specific hardware layer. Long messages can make this WebView taller than the
        // device's maximum GPU texture size, causing WebView to crash while creating the layer.
        setLayerType(LAYER_TYPE_NONE, null)

        configureDarkLightMode(this, config)

        with(settings) {
            setSupportZoom(true)
            builtInZoomControls = true
            useWideViewPort = true

            if (config.autoFitWidth) {
                loadWithOverviewMode = true
            }

            disableDisplayZoomControls()

            javaScriptEnabled = false
            loadsImagesAutomatically = true

            overScrollMode = OVER_SCROLL_NEVER

            textZoom = config.textZoom

            // Values range from smaller than default (1.0) to double size: 0.85, 1.0, 1.15, 1.3, 1.5, 1.8, 2.0
            val fontScale = Resources.getSystem().configuration.fontScale
            settings.textZoom = (settings.textZoom * fontScale).roundToInt()
        }

        // Disable network images by default. This is overridden by preferences.
        blockNetworkData(true)
        setWebContentsDebuggingEnabled(BuildConfig.DEBUG)
    }

    private fun configureDarkLightMode(
        webView: WebView,
        config: WebViewConfig,
    ) {
        if (config.useDarkMode && !usesGlass()) {
            webView.showInDarkMode()
        } else {
            webView.showInLightMode()
        }
    }

    private fun disableDisplayZoomControls() {
        val packageManager = context.packageManager
        val supportsMultiTouch = packageManager.hasSystemFeature(PackageManager.FEATURE_TOUCHSCREEN_MULTITOUCH) ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_FAKETOUCH_MULTITOUCH_DISTINCT)

        settings.displayZoomControls = !supportsMultiTouch
    }

    fun displayHtmlContentWithInlineAttachments(
        htmlText: String,
        attachmentResolver: AttachmentResolver?,
        onPageFinishedListener: OnPageFinishedListener?,
    ) {
        val glass = usesGlass()
        val started = android.os.SystemClock.elapsedRealtime()
        val scope = findViewTreeLifecycleOwner()?.lifecycleScope ?: fallbackRenderScope
        htmlRenderer.render(scope, htmlText, glass, useDarkMode) { prepared ->
            net.thunderbird.core.ui.theme.manager.GlassMotion.trace(
                context, "mail-html-ready", android.os.SystemClock.elapsedRealtime() - started,
            )
            setWebViewClient(attachmentResolver, onPageFinishedListener)
            setHtmlContent(prepared, glass)
        }
    }

    private fun setWebViewClient(
        attachmentResolver: AttachmentResolver?,
        onPageFinishedListener: OnPageFinishedListener?,
    ) {
        var delivered = false
        val notifyReady: () -> Unit = {
            if (!delivered) {
                delivered = true
                onPageFinishedListener?.onPageFinished()
            }
        }
        val webViewClient = webViewClientFactory.create(attachmentResolver) {
            net.thunderbird.core.ui.theme.manager.GlassMotion.trace(
                context, "mail-viewport-${width}x$height-content-$contentHeight-bounded-$bodyViewportEnabled",
            )
            notifyReady()
        }
        if (bodyViewportEnabled) {
            // Reading can reveal committed text while remote images continue loading.
            // Printing/signatures leave this unset and still wait for onPageFinished.
            webViewClient.onPageVisible = notifyReady
        }
        setWebViewClient(webViewClient)
    }

    private fun usesGlass(): Boolean = bodyViewportEnabled || TypedValue().let {
        val glassAttribute = app.k9mail.core.ui.legacy.designsystem.R.attr.firephenixLiquidGlass
        context.theme.resolveAttribute(glassAttribute, it, true) && it.data != 0
    }

    private fun setHtmlContent(htmlText: String, glass: Boolean) {
        if (glass) {
            setBackgroundColor(Color.TRANSPARENT)
            showInLightMode()
        }
        loadDataWithBaseURL("about:blank", htmlText, "text/html", "utf-8", null)
        resumeTimers()
    }

    override fun destroy() {
        htmlRenderer.cancel()
        fallbackRenderScope.cancel()
        super.destroy()
    }

    fun interface OnPageFinishedListener {
        fun onPageFinished()
    }
}
