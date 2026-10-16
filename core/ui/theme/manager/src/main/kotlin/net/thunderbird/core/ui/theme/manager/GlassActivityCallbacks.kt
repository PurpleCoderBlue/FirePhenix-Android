package net.thunderbird.core.ui.theme.manager

import android.app.Activity
import android.app.Application
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Bundle
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.core.view.WindowCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import app.k9mail.core.ui.legacy.designsystem.GlassBackdropView
import app.k9mail.core.ui.legacy.designsystem.GlassSurfaceRenderer
import app.k9mail.core.ui.legacy.designsystem.GlassThemePalette
import java.util.WeakHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.thunderbird.core.preference.AnimationPreference
import net.thunderbird.core.preference.AppTheme
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.theme.api.FeatureThemeProvider

/** Applies the shared appearance without changing mail rendering or business state. */
class GlassActivityCallbacks(
    private val settings: GeneralSettingsManager,
    private val theme: FeatureThemeProvider,
) : Application.ActivityLifecycleCallbacks {
    private val popupThemes = GlassPopupThemes()
    private val views = WeakHashMap<Activity, GlassViewStyler>()
    private val renderers = WeakHashMap<Activity, GlassSurfaceRenderer>()
    private val systemBarThemes = WeakHashMap<Activity, Boolean>()
    private val observers = WeakHashMap<Activity, ViewTreeObserver.OnPreDrawListener>()
    private val backgrounds = WeakHashMap<Activity, Pair<String, Job>>()
    private val motions = WeakHashMap<Activity, Pair<AnimationPreference, Boolean>>()
    private val contentImages = WeakHashMap<Activity, Bitmap>()
    private val palettes = WeakHashMap<Activity, GlassThemePalette>()

    override fun onActivityResumed(activity: Activity) {
        systemBarThemes.remove(activity)
        motions.remove(activity)
        val root = activity.window.decorView
        val observer = observers.getOrPut(activity) {
            ViewTreeObserver.OnPreDrawListener {
                if (!activity.isDestroyed) apply(activity)
                true
            }
        }
        root.viewTreeObserver.removeOnPreDrawListener(observer)
        root.viewTreeObserver.addOnPreDrawListener(observer)
        root.post { if (!activity.isDestroyed) apply(activity) }
    }

    private fun apply(activity: Activity) {
        val root = activity.findViewById<ViewGroup>(android.R.id.content) ?: return
        val config = settings.getConfig().display.coreSettings
        val dark = isDark(activity)
        popupThemes.apply(activity, dark, animated(activity))
        if (systemBarThemes[activity] != dark) {
            systemBarThemes[activity] = dark
            WindowCompat.getInsetsController(activity.window, activity.window.decorView).apply {
                isAppearanceLightStatusBars = !dark
                isAppearanceLightNavigationBars = !dark
            }
        }
        val existing = root.findViewWithTag<GlassBackdropView>(BACKDROP_TAG)
        val backdrop = existing ?: GlassBackdropView(activity).apply {
            tag = BACKDROP_TAG
            root.addView(
                this,
                0,
                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
            )
        }
        backdrop.configure(dark)
        applyBackground(activity, root, backdrop, config, dark)
        val renderer = renderers.getOrPut(activity) { GlassSurfaceRenderer(root) }
        renderer.configure(dark, config.glassBlur)
        views.getOrPut(activity) { GlassViewStyler(theme) }
            .visitWindow(activity.window.decorView, root, renderer, contentImages[activity], palettes[activity])
    }

    private fun applyBackground(
        activity: Activity,
        root: ViewGroup,
        backdrop: GlassBackdropView,
        config: net.thunderbird.core.preference.display.coreSettings.DisplayCoreSettings,
        dark: Boolean,
    ) {
        val key = "${config.glassBackground}/${config.glassBackgroundImage}/${config.glassBackgroundBlur}/$dark/" +
            "${config.glassAutomaticColor}/${config.glassThemeColor}"
        if (backgrounds[activity]?.first != key) {
            backgrounds.remove(activity)?.second?.cancel()
            val owner = activity as? LifecycleOwner
            if (owner != null) {
                val job = owner.lifecycleScope.launch {
                    val image = withContext(Dispatchers.IO) {
                        GlassBackgroundStore.load(
                            activity.applicationContext,
                            config.glassBackground,
                            config.glassBackgroundImage,
                            dark,
                            config.glassBackgroundBlur,
                        )
                    }
                    backdrop.configure(dark, image)
                    val seed = withContext(Dispatchers.IO) {
                        if (config.glassAutomaticColor) {
                            GlassBackgroundStore.color(
                                activity.applicationContext,
                                config.glassBackground,
                                config.glassBackgroundImage,
                            )
                        } else {
                            config.glassThemeColor
                        }
                    }
                    contentImages[activity] = image
                    palettes[activity] = GlassThemePalette.from(seed, dark)
                    GlassThemePalette.attach(activity, palettes.getValue(activity))
                    root.invalidate()
                }
                backgrounds[activity] = key to job
            }
        }
    }

    private fun isDark(activity: Activity): Boolean = when (settings.getConfig().display.coreSettings.appTheme) {
        AppTheme.DARK -> true

        AppTheme.LIGHT -> false

        AppTheme.FOLLOW_SYSTEM ->
            activity.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
    }

    private fun animated(activity: Activity): Boolean {
        val preference = settings.getConfig().display.visualSettings.animationPreference
        motions[activity]?.takeIf { it.first == preference }?.let { return it.second }
        return GlassMotion.enabled(activity, preference).also { motions[activity] = preference to it }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        popupThemes.apply(activity, isDark(activity), animated(activity))
        GlassMotion.configureActivity(activity, settings.getConfig().display.visualSettings.animationPreference)
    }

    override fun onActivityPaused(activity: Activity) {
        if (activity.isFinishing) {
            GlassMotion.configureActivity(
                activity,
                settings.getConfig().display.visualSettings.animationPreference,
                closing = true,
            )
        }
        systemBarThemes.remove(activity)
        observers.remove(activity)?.let { activity.window.decorView.viewTreeObserver.removeOnPreDrawListener(it) }
    }

    override fun onActivityStarted(activity: Activity) = Unit
    override fun onActivityStopped(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

    override fun onActivityDestroyed(activity: Activity) {
        onActivityPaused(activity)
        backgrounds.remove(activity)?.second?.cancel()
        contentImages.remove(activity)
        palettes.remove(activity)
        GlassThemePalette.detach(activity)
        renderers.remove(activity)?.close()
        views.remove(activity)?.close()
        popupThemes.remove(activity)
        motions.remove(activity)
    }

    private companion object {
        const val BACKDROP_TAG = "firephenix-glass-background"
    }
}
