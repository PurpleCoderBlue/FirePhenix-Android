package com.fsck.k9.ui.settings.general

import net.thunderbird.feature.mail.processing.api.R as ProcessingR
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.CreateDocument
import androidx.core.view.MenuProvider
import androidx.lifecycle.Lifecycle
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceScreen
import androidx.work.WorkInfo
import app.k9mail.feature.telemetry.api.TelemetryManager
import com.fsck.k9.job.K9JobManager
import com.fsck.k9.ui.BuildConfig
import com.fsck.k9.ui.R
import com.fsck.k9.ui.base.extensions.withArguments
import com.fsck.k9.ui.observe
import com.fsck.k9.ui.settings.notificationactions.NotificationActionsSettingsActivity
import com.fsck.k9.ui.settings.remove
import com.google.android.material.snackbar.Snackbar
import com.takisoft.preferencex.PreferenceFragmentCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import net.thunderbird.core.featureflag.FeatureFlagProvider
import net.thunderbird.core.featureflag.keys.GeneratedFeatureFlagKey
import net.thunderbird.feature.mail.processing.api.MailProcessingExtension
import org.koin.android.ext.android.inject
import org.koin.android.ext.android.getKoin
import org.koin.androidx.viewmodel.ext.android.viewModel

class GeneralSettingsFragment : PreferenceFragmentCompat() {
    private val viewModel: GeneralSettingsViewModel by viewModel()
    private val dataStore: GeneralSettingsDataStore by inject()
    private val telemetryManager: TelemetryManager by inject()
    private val featureFlagProvider: FeatureFlagProvider by inject()
    private val jobManager: K9JobManager by inject()

    private var rootKey: String? = null
    private var currentUiState: GeneralSettingsUiState? = null
    private var snackbar: Snackbar? = null

    private val exportLogsResultContract = registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
        if (contentUri != null) {
            viewModel.exportLogs(contentUri)
        }
    }

    private val exportSyncDebugLogsResultContract =
        registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
            if (contentUri != null) {
                viewModel.fileExport(contentUri.toString())
            }
        }
    private val choseFileSyncDebugLogsResultContract =
        registerForActivityResult(CreateDocument("text/plain")) { contentUri ->
            if (contentUri != null) {
                context?.contentResolver?.takePersistableUriPermission(
                    contentUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
                )
                jobManager.scheduleDebugLogLimit(contentUri.toString()).observe(
                    this,
                ) { workInfo: WorkInfo? ->
                    if (workInfo != null) {
                        if (workInfo.state == WorkInfo.State.SUCCEEDED) {
                            viewModel.showExportSnackbar(true)
                        } else if (workInfo.state == WorkInfo.State.FAILED) {
                            viewModel.showExportSnackbar(false)
                        }
                    }
                }
            }
        }

    override fun onCreatePreferencesFix(savedInstanceState: Bundle?, rootKey: String?) {
        preferenceManager.preferenceDataStore = dataStore
        this.rootKey = rootKey
        setPreferencesFromResource(R.xml.general_settings, rootKey)
        addFirePhenixPreferences()

        findPreference<Preference>("notification_actions_settings")?.onPreferenceClickListener =
            Preference.OnPreferenceClickListener {
                context?.let { NotificationActionsSettingsActivity.start(it) }
                true
            }

        val listener = Preference.OnPreferenceChangeListener { _, newValue ->
            if (!(newValue as Boolean)) {
                jobManager.cancelDebugLogLimit()
                exportSyncDebugLogsResultContract.launch(formatFileExportUriString())
            } else {
                choseFileSyncDebugLogsResultContract.launch(formatFileExportUriString())
            }
            true
        }
        findPreference<Preference>("sync_debug_logging")?.onPreferenceChangeListener = listener
        featureFlagProvider.provide(GeneratedFeatureFlagKey.DISABLE_FONT_SIZE_CONFIG)
            .onEnabled {
                val parentPreference = findPreference<PreferenceCategory>("global_preferences")
                val fontSizePreferenceScreen = findPreference<PreferenceScreen>("font_size")

                if (parentPreference != null && fontSizePreferenceScreen != null) {
                    parentPreference.removePreference(fontSizePreferenceScreen)
                }
            }

        findPreference<Preference>("debug_secret_debug_screen")?.apply {
            if (!BuildConfig.DEBUG) {
                remove()
                onPreferenceClickListener = null
            } else {
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    viewModel.onOpenSecretDebugScreen(requireContext())

                    true
                }
            }
        }

        initializeDataCollection()

        viewModel.uiState.observe(this) { uiState ->
            updateUiState(uiState)
        }
    }

    override fun onDisplayPreferenceDialog(preference: Preference) {
        if (preference.key == "language" && preference is androidx.preference.ListPreference) {
            val tag = "firephenix-language-dialog"
            if (parentFragmentManager.findFragmentByTag(tag) == null) {
                CompactLanguagePreferenceDialog().apply {
                    arguments = Bundle().apply { putString("key", preference.key) }
                    @Suppress("DEPRECATION")
                    setTargetFragment(this@GeneralSettingsFragment, 0)
                }.show(parentFragmentManager, tag)
            }
            return
        }
        super.onDisplayPreferenceDialog(preference)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        dismissSnackbar()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.title = preferenceScreen.title

        val menuHost = requireActivity()
        menuHost.addMenuProvider(
            object : MenuProvider {
                override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                    if (rootKey == PREFERENCE_SCREEN_DEBUGGING) {
                        menuInflater.inflate(R.menu.debug_settings_option, menu)
                    }
                }

                override fun onPrepareMenu(menu: Menu) {
                    if (rootKey == PREFERENCE_SCREEN_DEBUGGING) {
                        currentUiState?.let { uiState ->
                            menu.findItem(R.id.exportLogs).isEnabled = uiState.isExportLogsMenuEnabled
                        }
                    }
                }

                override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                    return when (menuItem.itemId) {
                        R.id.exportLogs -> {
                            exportLogsResultContract.launch(GeneralSettingsViewModel.DEFAULT_FILENAME)
                            true
                        }

                        R.id.exportSyncLogs -> {
                            exportSyncDebugLogsResultContract.launch(formatFileExportUriString())
                            true
                        }

                        else -> false
                    }
                }
            },
            viewLifecycleOwner,
            Lifecycle.State.RESUMED,
        )
    }

    private fun initializeDataCollection() {
        if (!telemetryManager.isTelemetryFeatureIncluded()) {
            findPreference<Preference>(PREFERENCE_DATA_COLLECTION)?.remove()
        }
    }

    private fun updateUiState(uiState: GeneralSettingsUiState) {
        val oldUiState = currentUiState
        currentUiState = uiState

        if (oldUiState?.isExportLogsMenuEnabled != uiState.isExportLogsMenuEnabled) {
            setExportLogsMenuEnabled()
        }

        if (oldUiState?.snackbarState != uiState.snackbarState) {
            setSnackbarState(uiState.snackbarState)
        }
    }

    private fun setExportLogsMenuEnabled() {
        requireActivity().invalidateOptionsMenu()
    }

    private fun setSnackbarState(snackbarState: SnackbarState) {
        when (snackbarState) {
            SnackbarState.Hidden -> dismissSnackbar()
            SnackbarState.ExportLogSuccess -> showSnackbar(R.string.debug_export_logs_success)
            SnackbarState.ExportLogFailure -> showSnackbar(R.string.debug_export_logs_failure)
        }
    }

    private fun dismissSnackbar() {
        snackbar?.dismiss()
        snackbar = null
    }

    private fun showSnackbar(message: Int) {
        Snackbar.make(requireView(), message, Snackbar.LENGTH_INDEFINITE)
            .also { snackbar = it }
            .show()
    }

    private fun formatFileExportUriString(): String {
        val now = Calendar.getInstance()
        return String.format(
            Locale.US,
            "%s_%s.txt",
            DEFAULT_SYNC_FILENAME,
            SimpleDateFormat("yyyy-MM-dd", Locale.US).format(now.time),
        )
    }

    companion object {
        private const val PREFERENCE_SCREEN_DEBUGGING = "debug_preferences"
        private const val PREFERENCE_DATA_COLLECTION = "data_collection"
        const val DEFAULT_SYNC_FILENAME = "thunderbird-sync-logs"

        fun create(rootKey: String? = null) = GeneralSettingsFragment().withArguments(ARG_PREFERENCE_ROOT to rootKey)
    }
}

private fun GeneralSettingsFragment.addFirePhenixPreferences() {
        getKoin().getOrNull<net.thunderbird.core.ui.theme.api.AppearanceSettings>()?.let { appearance ->
            findPreference<Preference>("firephenix_appearance")?.apply {
                title = appearance.title
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    appearance.open(requireContext())
                    true
                }
            }
            // The shared appearance editor includes the existing light/dark setting.
            findPreference<Preference>("theme")?.isVisible = false
        }
        getKoin().getOrNull<MailProcessingExtension>()?.let { extension ->
            findPreference<Preference>("firephenix_remote_images")?.onPreferenceClickListener =
                Preference.OnPreferenceClickListener {
                    extension.openRemoteImageSettings(requireContext())
                    true
                }
            findPreference<Preference>("firephenix_mail_processing")?.apply {
                title = getString(ProcessingR.string.mail_processing_settings_title)
                onPreferenceClickListener = Preference.OnPreferenceClickListener {
                    extension.openSettings(requireContext())
                    true
                }
            }
        }
    }
