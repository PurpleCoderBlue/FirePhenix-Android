package net.thunderbird.feature.mail.processing.internal.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import net.thunderbird.feature.mail.processing.internal.R
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure

@Composable
internal fun processingErrorText(error: ProcessingFailure): String = stringResource(
    when (error) {
        ProcessingFailure.NOT_CONFIGURED -> R.string.processing_error_not_configured
        ProcessingFailure.INVALID_CONFIGURATION -> R.string.processing_error_invalid_configuration
        ProcessingFailure.AUTHENTICATION -> R.string.processing_error_authentication
        ProcessingFailure.BILLING -> R.string.processing_error_billing
        ProcessingFailure.RATE_LIMIT -> R.string.processing_error_rate_limit
        ProcessingFailure.TIMEOUT -> R.string.processing_error_timeout
        ProcessingFailure.NETWORK -> R.string.processing_error_network
        ProcessingFailure.DNS -> R.string.processing_error_dns
        ProcessingFailure.TLS -> R.string.processing_error_tls
        ProcessingFailure.ENDPOINT -> R.string.processing_error_endpoint
        ProcessingFailure.SERVER -> R.string.processing_error_server
        ProcessingFailure.OUTPUT -> R.string.processing_error_output
        ProcessingFailure.INCOMPLETE -> R.string.processing_error_incomplete
        ProcessingFailure.STORAGE -> R.string.processing_error_storage
    },
)
