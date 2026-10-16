package net.thunderbird.core.android.account

/** A local label only: explicit nicknames always win over the provider suggestion. */
internal fun resolveAccountDisplayName(name: String?, email: String, incomingHost: String?): String {
    name?.takeIf { it.isNotBlank() && !it.equals(email, ignoreCase = true) }?.let { return it }
    val domain = email.substringAfterLast('@', missingDelimiterValue = "").lowercase()
    val host = incomingHost?.lowercase()?.removeSuffix(".")
    return when {
        domain in GMAIL_DOMAINS || host in GMAIL_HOSTS -> "Gmail"
        domain in OUTLOOK_DOMAINS || host in OUTLOOK_HOSTS -> "Outlook"
        else -> email
    }
}

private val GMAIL_DOMAINS = setOf("gmail.com", "googlemail.com")
private val GMAIL_HOSTS = setOf("imap.gmail.com", "imap.googlemail.com", "pop.gmail.com", "pop.googlemail.com")
private val OUTLOOK_DOMAINS = setOf(
    "outlook.com", "outlook.cn", "hotmail.com", "hotmail.co.uk", "hotmail.fr", "hotmail.de",
    "live.com", "live.cn", "live.co.uk", "msn.com",
)
private val OUTLOOK_HOSTS = setOf(
    "outlook.office365.com",
    "outlook.office.com",
    "imap-mail.outlook.com",
    "pop-mail.outlook.com",
)
