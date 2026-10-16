package net.thunderbird.feature.mail.processing.internal.ui

/** Public, checked-in fixture only. No message or account fields are accepted by the test path. */
internal object ProcessingModelTestFixture {
    val translation: String = listOf(
        "Hello. This demonstration contains public sample text and does not include email, " +
            "accounts, or private information.",
        "The reader keeps the original layout, links, tables, and images while translating the " +
            "text inside each paragraph.",
        "A translation request should preserve the order and number of its input paragraphs, " +
            "including short headings.",
        "The user can read the translated message and switch back to the original without " +
            "changing the saved email.",
        "A document can contain several sections. Each section has a heading followed by a " +
            "short explanatory paragraph.",
        "Background tasks should stop when the user cancels them and must not replace a newer " +
            "result with an older one.",
        "The demonstration meeting starts tomorrow morning. No action is required today and no " +
            "invitation will be sent.",
        "A sample checklist includes reading the instructions, reviewing the example, and " +
            "checking the final result.",
        "The application should show a clear error if a service times out instead of silently " +
            "using another provider.",
        "This is the last sample paragraph. Please translate these public sentences without " +
            "adding any new information.",
    ).joinToString("\n\n")
}
