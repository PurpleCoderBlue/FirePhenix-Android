package net.thunderbird.feature.notification.api.push

/** A future provider hint, without mail content. Receiving a hint does not establish mail delivery. */
data class MailPushSignal(
    val source: MailPushSource,
    val accountId: String,
    val changeId: String? = null,
)

enum class MailPushSource { FCM, HONOR }

/**
 * Optional boundary for future SDK adapters to validate a hint and request existing account synchronization.
 * No adapter, SDK, token registration, gateway, or runtime binding is provided by this declaration.
 */
fun interface MailPushSignalHandler {
    suspend fun onSignal(signal: MailPushSignal)
}
