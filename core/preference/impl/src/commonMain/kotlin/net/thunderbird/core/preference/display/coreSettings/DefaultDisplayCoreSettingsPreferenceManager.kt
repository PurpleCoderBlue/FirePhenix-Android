package net.thunderbird.core.preference.display.coreSettings

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import net.thunderbird.core.logging.Logger
import net.thunderbird.core.preference.PreferenceChangeBroker
import net.thunderbird.core.preference.PreferenceChangeSubscriber
import net.thunderbird.core.preference.PreferenceScope
import net.thunderbird.core.preference.storage.Storage
import net.thunderbird.core.preference.storage.StorageEditor
import net.thunderbird.core.preference.storage.StoragePersister
import net.thunderbird.core.preference.storage.getEnumOrDefault
import net.thunderbird.core.preference.storage.putEnum

private const val TAG = "DefaultDisplayCoreSettingsPreferenceManager"

class DefaultDisplayCoreSettingsPreferenceManager(
    private val logger: Logger,
    private val storagePersister: StoragePersister,
    private val storageEditor: StorageEditor,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private var scope: CoroutineScope = CoroutineScope(SupervisorJob()),
    preferenceChangeBroker: PreferenceChangeBroker,
) : DisplayCoreSettingsPreferenceManager, PreferenceChangeSubscriber {

    init {
        preferenceChangeBroker.subscribe(this)
    }

    private val configState: MutableStateFlow<DisplayCoreSettings> = MutableStateFlow(value = loadConfig())
    private val mutex = Mutex()
    private val storage: Storage
        get() = storagePersister.loadValues()

    override fun getConfig(): DisplayCoreSettings = configState.value

    override fun getConfigFlow(): Flow<DisplayCoreSettings> = configState

    override fun save(config: DisplayCoreSettings) {
        logger.debug(TAG) { "save() called with: config = $config" }
        writeConfig(config)
        configState.update { config }
    }

    private fun loadConfig(): DisplayCoreSettings = DisplayCoreSettings(
        glassBlur = storage.getInt(DisplayCoreSettingKey.GlassBlur.value, 16).coerceIn(0, 32),
        glassBackground = storage.getEnumOrDefault(DisplayCoreSettingKey.GlassBackground.value, GlassBackground.BLUE),
        glassBackgroundImage = storage.getStringOrDefault(DisplayCoreSettingKey.GlassBackgroundImage.value, ""),
        glassBackgroundBlur = storage.getInt(DisplayCoreSettingKey.GlassBackgroundBlur.value, 12).coerceIn(0, 512),
        glassAutomaticColor = storage.getBoolean(DisplayCoreSettingKey.GlassAutomaticColor.value, true),
        glassThemeColor =
        storage.getInt(DisplayCoreSettingKey.GlassThemeColor.value, 0xFF3576A5.toInt()) or 0xFF000000.toInt(),
        fixedMessageViewTheme = storage.getBoolean(
            DisplayCoreSettingKey.FixedMessageViewTheme.value,
            DISPLAY_SETTINGS_DEFAULT_FIXED_MESSAGE_VIEW_THEME,
        ),
        appTheme = storage.getEnumOrDefault(DisplayCoreSettingKey.Theme.value, DISPLAY_SETTINGS_DEFAULT_APP_THEME),
        messageViewTheme = storage.getEnumOrDefault(
            DisplayCoreSettingKey.MessageViewTheme.value,
            DISPLAY_SETTINGS_DEFAULT_MESSAGE_VIEW_THEME,
        ),
        messageComposeTheme = storage.getEnumOrDefault(
            DisplayCoreSettingKey.MessageComposeTheme.value,
            DISPLAY_SETTINGS_DEFAULT_MESSAGE_COMPOSE_THEME,
        ),
        appLanguage = storage.getStringOrDefault(
            DisplayCoreSettingKey.AppLanguage.value,
            DISPLAY_SETTINGS_DEFAULT_APP_LANGUAGE,
        ),
        splitViewMode = storage.getEnumOrDefault(
            DisplayCoreSettingKey.SplitViewMode.value,
            DISPLAY_SETTINGS_DEFAULT_SPLIT_VIEW_MODE,
        ),
    )

    private fun writeConfig(config: DisplayCoreSettings) {
        logger.debug(TAG) { "writeConfig() called with: config = $config" }
        scope.launch(ioDispatcher) {
            mutex.withLock {
                storageEditor.putEnum(DisplayCoreSettingKey.Theme.value, config.appTheme)
                storageEditor.putInt(DisplayCoreSettingKey.GlassBlur.value, config.glassBlur.coerceIn(0, 32))
                storageEditor.putEnum(DisplayCoreSettingKey.GlassBackground.value, config.glassBackground)
                storageEditor.putString(DisplayCoreSettingKey.GlassBackgroundImage.value, config.glassBackgroundImage)
                storageEditor.putInt(
                    DisplayCoreSettingKey.GlassBackgroundBlur.value,
                    config.glassBackgroundBlur.coerceIn(0, 512),
                )
                storageEditor.putBoolean(DisplayCoreSettingKey.GlassAutomaticColor.value, config.glassAutomaticColor)
                storageEditor.putInt(
                    DisplayCoreSettingKey.GlassThemeColor.value,
                    config.glassThemeColor or 0xFF000000.toInt(),
                )
                storageEditor.putEnum(DisplayCoreSettingKey.MessageViewTheme.value, config.messageViewTheme)
                storageEditor.putEnum(
                    DisplayCoreSettingKey.MessageComposeTheme.value,
                    config.messageComposeTheme,
                )
                storageEditor.putBoolean(
                    DisplayCoreSettingKey.FixedMessageViewTheme.value,
                    config.fixedMessageViewTheme,
                )
                storageEditor.putString(DisplayCoreSettingKey.AppLanguage.value, config.appLanguage)
                storageEditor.putEnum(DisplayCoreSettingKey.SplitViewMode.value, config.splitViewMode)
                storageEditor.commit().also { commited ->
                    logger.verbose(TAG) { "writeConfig: storageEditor.commit() resulted in: $commited" }
                }
            }
        }
    }

    override fun receive(scope: PreferenceScope) {
        if (scope == PreferenceScope.ALL || scope == PreferenceScope.DISPLAY_CORE) {
            configState.update { loadConfig() }
        }
    }
}
