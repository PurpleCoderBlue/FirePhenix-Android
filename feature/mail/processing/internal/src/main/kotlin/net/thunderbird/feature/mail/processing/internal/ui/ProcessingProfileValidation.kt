package net.thunderbird.feature.mail.processing.internal.ui

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import net.thunderbird.feature.mail.processing.api.ProcessingProvider
import net.thunderbird.feature.mail.processing.api.ServiceProfile
import net.thunderbird.feature.mail.processing.internal.data.ProcessingFailure
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal fun normalizedProfile(draft: ServiceProfile, parameters: String): ServiceProfile {
    val values = try {
        Json.parseToJsonElement(parameters.ifBlank { "{}" }).jsonObject.mapValues { it.value.toString() }
    } catch (_: IllegalArgumentException) {
        throw ProfileValidationException(ProfileField.PARAMETERS, ProcessingFailure.INVALID_CONFIGURATION)
    }
    return draft.copy(
        name = draft.name.trim().ifBlank { draft.provider.label },
        endpoint = draft.endpoint.trim(),
        model = draft.model.trim(),
        region = draft.region.trim(),
        parameters = values,
    )
}

internal fun validateProfile(profile: ServiceProfile) {
    val endpoint = profile.endpoint.toHttpUrlOrNull()
    val userInfo = endpoint?.let { it.username.isNotEmpty() || it.password.isNotEmpty() } ?: false
    val invalidEndpoint = endpoint == null || endpoint.isHttps.not()
    if (invalidEndpoint || userInfo || endpoint?.fragment != null) {
        throw ProfileValidationException(ProfileField.ENDPOINT, ProcessingFailure.INVALID_CONFIGURATION)
    }
    val traditional = profile.provider in setOf(ProcessingProvider.MICROSOFT, ProcessingProvider.GOOGLE)
    if (!traditional && profile.model.isBlank()) {
        throw ProfileValidationException(ProfileField.MODEL, ProcessingFailure.NOT_CONFIGURED)
    }
}
