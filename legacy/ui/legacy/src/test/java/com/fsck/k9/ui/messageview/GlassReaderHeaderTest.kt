package com.fsck.k9.ui.messageview

import android.app.Activity
import android.app.Application
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.view.ContextThemeWrapper
import androidx.constraintlayout.widget.ConstraintLayout
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import com.fsck.k9.ui.R
import com.google.android.material.button.MaterialButton
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28], application = Application::class)
class GlassReaderHeaderTest {
    @Test
    fun `Original retains layout while glass reuses the same download and image consent actions`() {
        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val context =
            ContextThemeWrapper(
                activity,
                com.google.android.material.R.style.Theme_MaterialComponents_Light_NoActionBar,
            )
        val root = FrameLayout(context)
        val subjectBar = ConstraintLayout(context).apply {
            addView(TextView(context).apply { id = R.id.subject }, ConstraintLayout.LayoutParams(100, 48))
            addView(View(context).apply { id = R.id.flagged }, ConstraintLayout.LayoutParams(48, 48))
            addView(View(context).apply { id = R.id.mail_processing_actions }, ConstraintLayout.LayoutParams(48, 48))
        }
        val participants = ConstraintLayout(context).apply {
            id = R.id.participants_container
            listOf(R.id.date, R.id.recipients, R.id.menu_primary_action).forEach { target ->
                addView(View(context).apply { id = target }, ConstraintLayout.LayoutParams(48, 48))
            }
        }
        var downloaded = 0
        val download = MaterialButton(context).apply {
            id = R.id.download_remainder
            text = "Download full message"
            visibility = View.GONE
            setOnClickListener { downloaded++ }
        }
        val images = MaterialButton(context).apply {
            id = R.id.show_pictures
            text = "Show remote images"
        }
        val remote = FrameLayout(context).apply {
            id = R.id.extra_header_container
            visibility = View.GONE
            addView(images)
        }
        listOf(subjectBar, participants, download, remote).forEach(root::addView)

        GlassReaderHeader.apply(root, false)
        assertThat(download.parent).isSameInstanceAs(root)
        assertThat(download.text.toString()).isEqualTo("Download full message")
        GlassReaderHeader.apply(root, true)

        assertThat(download.parent).isSameInstanceAs(participants)
        assertThat(remote.parent).isSameInstanceAs(participants)
        assertThat(download.visibility).isEqualTo(View.GONE)
        assertThat(remote.visibility).isEqualTo(View.GONE)
        assertThat(download.contentDescription.toString()).isEqualTo("Download full message")
        assertThat(images.contentDescription.toString()).isEqualTo("Show remote images")
        download.performClick()
        assertThat(downloaded).isEqualTo(1)
    }
}
