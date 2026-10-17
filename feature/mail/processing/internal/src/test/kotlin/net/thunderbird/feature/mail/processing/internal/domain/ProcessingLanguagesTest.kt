package net.thunderbird.feature.mail.processing.internal.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class ProcessingLanguagesTest {
    @Test
    fun `Chinese language variants share the same skip gate`() {
        listOf("zh", "zh-Hans", "zh_TW", "cmn-Hant", "yue-HK").forEach {
            assertThat(it.isChineseLanguage()).isEqualTo(true)
        }
        listOf(null, "", "und", "en", "ja", "ko").forEach {
            assertThat(it.isChineseLanguage()).isEqualTo(false)
        }
    }

    @Test
    fun `local script fallback recognizes short and mixed Chinese but not Japanese or isolated names`() {
        listOf("𠀀𠀁", "请查收", "您的 GitHub 验证码已经发送，请勿转发。", "請確認您的帳戶安全設定。").forEach {
            assertThat(chineseSampleLanguage(it)).isEqualTo("zh")
        }
        listOf("Hello", "Your receipt for 北京 is attached", "新しいメールを確認してください", "한국어 안내", "123!?").forEach {
            assertThat(chineseSampleLanguage(it)).isEqualTo(null)
        }
    }
}
