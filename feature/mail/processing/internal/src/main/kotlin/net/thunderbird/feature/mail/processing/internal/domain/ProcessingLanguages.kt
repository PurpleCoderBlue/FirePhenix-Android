package net.thunderbird.feature.mail.processing.internal.domain

/** Local language gates; an unknown language must not be silently treated as Chinese. */
internal fun String?.isChineseLanguage(): Boolean = this?.substringBefore('-')?.substringBefore('_')
    ?.lowercase() in setOf("zh", "cmn", "zho", "yue")

/** Script-aware fallback for short Chinese prose, excluding Japanese kana and Korean hangul. */
internal fun chineseSampleLanguage(text: String): String? {
    val sample = text.take(LANGUAGE_SAMPLE_LIMIT).replace(Regex("https?://\\S+|[\\w.+-]+@[\\w.-]+"), " ")
    var han = 0
    var letters = 0
    var otherCjk = false
    var index = 0
    while (index < sample.length) {
        val code = Character.codePointAt(sample, index)
        index += Character.charCount(code)
        if (Character.isLetter(code)) letters++
        if (code in HAN_A_START..HAN_A_END || code in HAN_START..HAN_END || code in HAN_EXT_START..HAN_EXT_END) han++
        if (code in KANA_START..KANA_END || code in HANGUL_START..HANGUL_END ||
            code in JAMO_START..JAMO_END
        ) {
            otherCjk = true
        }
    }
    return "zh".takeIf { !otherCjk && han >= 2 && han >= letters * MINIMUM_HAN_FRACTION }
}

private const val LANGUAGE_SAMPLE_LIMIT = 3_000
private const val MINIMUM_HAN_FRACTION = 0.4
private const val HAN_A_START = 0x3400
private const val HAN_A_END = 0x4DBF
private const val HAN_START = 0x4E00
private const val HAN_END = 0x9FFF
private const val HAN_EXT_START = 0x20000
private const val HAN_EXT_END = 0x3134F
private const val KANA_START = 0x3040
private const val KANA_END = 0x30FF
private const val HANGUL_START = 0xAC00
private const val HANGUL_END = 0xD7AF
private const val JAMO_START = 0x1100
private const val JAMO_END = 0x11FF
