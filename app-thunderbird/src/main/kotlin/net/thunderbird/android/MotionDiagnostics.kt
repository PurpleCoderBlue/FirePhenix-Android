package net.thunderbird.android

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.FrameMetrics
import android.view.Window
import androidx.annotation.RequiresApi
import java.util.WeakHashMap
import java.util.concurrent.atomic.AtomicLong
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.LogSink
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.ui.theme.manager.GlassMotion

/** Debug-only, local UI timing diagnostics. No mail, account, URL, token or user input is recorded. */
internal class MotionDiagnosticSink(context: android.content.Context) : LogSink {
    private val file = java.io.File(context.cacheDir, "ui-motion.log")
    private val writer = java.util.concurrent.Executors.newSingleThreadExecutor { task ->
        Thread(task, "FirePhenixMotionLog")
    }
    private companion object {
        const val MAX_LOG_BYTES = 262_144
    }

    override val level = LogLevel.INFO
    override fun log(event: LogEvent) {
        if (event.tag != MOTION_TAG && event.tag != "FP-AI") return
        android.util.Log.println(android.util.Log.INFO, event.tag, event.message)
        val line = "${android.os.SystemClock.uptimeMillis()} ${event.tag} ${event.message}\n"
        writer.execute {
            try {
                if (file.length() > MAX_LOG_BYTES) file.writeText("")
                file.appendText(line)
            } catch (_: java.io.IOException) {
                // Diagnostics must never interrupt mail handling or recurse through the Logger.
            }
        }
    }
}

internal const val MOTION_TAG = "FP-Motion"

@RequiresApi(Build.VERSION_CODES.N)
internal class MotionDiagnostics(private val logger: Logger) : Application.ActivityLifecycleCallbacks {
    private val thread = HandlerThread("FirePhenixFrameMetrics").apply { start() }
    private val handler = Handler(thread.looper)
    private val listeners = WeakHashMap<Activity, Window.OnFrameMetricsAvailableListener>()
    private val deadline = AtomicLong()

    init {
        GlassMotion.diagnostics = { event ->
            deadline.set(SystemClock.uptimeMillis() + OBSERVE_MS)
            logger.info(tag = MOTION_TAG) { event }
        }
    }

    override fun onActivityResumed(activity: Activity) {
        GlassMotion.trace(activity, "resumed")
        val screen = activity.javaClass.simpleName
        val listener = Window.OnFrameMetricsAvailableListener { _, metrics, dropped ->
            val total = metrics.getMetric(FrameMetrics.TOTAL_DURATION) / NANOS_PER_MS
            if (SystemClock.uptimeMillis() <= deadline.get() && total >= SLOW_FRAME_MS) {
                val layout = metrics.getMetric(FrameMetrics.LAYOUT_MEASURE_DURATION) / NANOS_PER_MS
                val draw = metrics.getMetric(FrameMetrics.DRAW_DURATION) / NANOS_PER_MS
                val sync = metrics.getMetric(FrameMetrics.SYNC_DURATION) / NANOS_PER_MS
                val commands = metrics.getMetric(FrameMetrics.COMMAND_ISSUE_DURATION) / NANOS_PER_MS
                val unknown = metrics.getMetric(FrameMetrics.UNKNOWN_DELAY_DURATION) / NANOS_PER_MS
                val animation = metrics.getMetric(FrameMetrics.ANIMATION_DURATION) / NANOS_PER_MS
                val gpu = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    metrics.getMetric(FrameMetrics.GPU_DURATION) / NANOS_PER_MS
                } else {
                    0L
                }
                logger.info(tag = MOTION_TAG) {
                    "screen=$screen stage=slow-frame totalMs=$total layoutMs=$layout drawMs=$draw " +
                        "syncMs=$sync commandsMs=$commands unknownMs=$unknown animationMs=$animation " +
                        "gpuMs=$gpu droppedReports=$dropped"
                }
            }
        }
        listeners.put(activity, listener)?.let(activity.window::removeOnFrameMetricsAvailableListener)
        activity.window.addOnFrameMetricsAvailableListener(listener, handler)
    }

    override fun onActivityPaused(activity: Activity) {
        GlassMotion.trace(activity, "paused-task-${activity.taskId}-finishing-${activity.isFinishing}")
        listeners.remove(activity)?.let(activity.window::removeOnFrameMetricsAvailableListener)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        GlassMotion.trace(activity, "created-task-${activity.taskId}")
    }
    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) {
        GlassMotion.trace(activity, "stopped-task-${activity.taskId}-finishing-${activity.isFinishing}")
    }
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) {
        GlassMotion.trace(
            activity,
            "destroyed-task-${activity.taskId}-configuration-${activity.isChangingConfigurations}",
        )
        onActivityPaused(activity)
    }

    private companion object {
        const val OBSERVE_MS = 1500L
        const val SLOW_FRAME_MS = 32L
        const val NANOS_PER_MS = 1_000_000L
    }
}
