package net.thunderbird.android

import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.K9
import net.thunderbird.app.common.FeatureFlagApplication
import net.thunderbird.core.android.account.LegacyAccountDtoManager
import net.thunderbird.feature.mail.processing.api.MailProcessingExtension
import org.koin.android.ext.android.inject
import org.koin.core.module.Module

class ThunderbirdApp : FeatureFlagApplication() {
    private val telemetryManager: TelemetryManager by inject()
    private val generalSettings: net.thunderbird.core.preference.GeneralSettingsManager by inject()
    private val featureTheme: net.thunderbird.core.ui.theme.api.FeatureThemeProvider by inject()
    private val accountManager: LegacyAccountDtoManager by inject()
    private val mailProcessing: MailProcessingExtension by inject()
    private val aliases: net.thunderbird.feature.account.settings.alias.AliasService by inject()

    override fun provideAppModule(): Module = appModule
    override val appName: String = "thunderbird"
    override val appVersion: String = BuildConfig.VERSION_NAME

    override fun onCreate() {
        super.onCreate()

        initializeTelemetry()
        if (BuildConfig.DEBUG && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            addDiagnosticLogSink(MotionDiagnosticSink(this))
            registerActivityLifecycleCallbacks(MotionDiagnostics(logger))
        }
        registerActivityLifecycleCallbacks(
            net.thunderbird.core.ui.theme.manager.GlassActivityCallbacks(
                generalSettings,
                featureTheme,
            ),
        )
        accountManager.addAccountRemovedListener { id ->
            mailProcessing.clearAccount(id.toString())
            aliases.clearAccount(id.toString())
        }
    }

    private fun initializeTelemetry() {
        telemetryManager.init(
            uploadEnabled = K9.isTelemetryEnabled,
            releaseChannel = BuildConfig.GLEAN_RELEASE_CHANNEL,
            versionCode = BuildConfig.VERSION_CODE,
            versionName = BuildConfig.VERSION_NAME,
        )
    }
}
