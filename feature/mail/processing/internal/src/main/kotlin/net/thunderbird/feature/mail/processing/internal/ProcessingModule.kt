package net.thunderbird.feature.mail.processing.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import net.thunderbird.feature.mail.processing.api.MailProcessingExtension
import net.thunderbird.feature.mail.processing.internal.data.EncryptedProcessingStorage
import net.thunderbird.feature.mail.processing.internal.data.ProcessingHttpClient
import net.thunderbird.feature.mail.processing.internal.data.ProcessingProtocol
import net.thunderbird.feature.mail.processing.internal.data.ProcessingService
import net.thunderbird.feature.mail.processing.internal.data.ProcessingStorage
import net.thunderbird.feature.mail.processing.internal.domain.MailProcessingManager
import net.thunderbird.feature.mail.processing.internal.domain.OnDeviceLanguageDetector
import net.thunderbird.feature.mail.processing.internal.domain.ProcessingLanguageDetector
import net.thunderbird.feature.mail.processing.internal.ui.ProcessingReaderViewModel
import net.thunderbird.feature.mail.processing.internal.ui.ProcessingSettingsViewModel
import net.thunderbird.feature.mail.processing.internal.ui.RemoteImageSettingsViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val mailProcessingModule = module {
    single<ProcessingStorage> { EncryptedProcessingStorage(androidContext()) }
    single { ProcessingProtocol() }
    single {
        val logger = get<net.thunderbird.core.logging.Logger>()
        ProcessingHttpClient(get(), diagnostic = { event -> logger.info(tag = "FP-AI") { event } })
    }
    single<ProcessingService> { get<ProcessingHttpClient>() }
    single<net.thunderbird.feature.mail.processing.internal.data.ProcessingConnectionTester> {
        get<ProcessingHttpClient>()
    }
    single<ProcessingLanguageDetector> { OnDeviceLanguageDetector(androidContext()) }
    single { MailProcessingManager(get(), get(), CoroutineScope(SupervisorJob() + Dispatchers.Default)) }
    single<MailProcessingExtension> { DefaultMailProcessingExtension(get(), get()) }
    viewModelOf(::ProcessingReaderViewModel)
    viewModel { ProcessingSettingsViewModel(get(), get(), get(), get()) }
    viewModelOf(::RemoteImageSettingsViewModel)
}
