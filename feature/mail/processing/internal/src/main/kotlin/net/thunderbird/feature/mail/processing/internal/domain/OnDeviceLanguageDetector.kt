package net.thunderbird.feature.mail.processing.internal.domain

import android.content.Context
import android.os.Build
import android.view.textclassifier.TextClassificationManager
import android.view.textclassifier.TextLanguage

internal fun interface ProcessingLanguageDetector {
    fun detect(text: String): String?
}

internal class OnDeviceLanguageDetector(private val context: Context) : ProcessingLanguageDetector {
    override fun detect(text: String): String? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || text.isBlank()) return null
        return try {
            val classifier = context.getSystemService(TextClassificationManager::class.java).textClassifier
            val result = classifier.detectLanguage(
                TextLanguage.Request.Builder(text.take(DETECTION_CHARACTER_LIMIT)).build(),
            )
            if (result.localeHypothesisCount == 0) {
                null
            } else {
                result.getLocale(0).takeIf { result.getConfidenceScore(it) >= MINIMUM_CONFIDENCE }?.toLanguageTag()
            }
        } catch (_: Exception) {
            null
        }
    }
    private companion object {
        const val DETECTION_CHARACTER_LIMIT = 20_000
        const val MINIMUM_CONFIDENCE = 0.65f
    }
}
