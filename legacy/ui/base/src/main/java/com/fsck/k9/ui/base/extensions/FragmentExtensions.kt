package com.fsck.k9.ui.base.extensions

import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentTransaction
import net.thunderbird.core.preference.GeneralSettingsManager
import net.thunderbird.core.ui.theme.manager.GlassMotion
import org.koin.android.ext.android.getKoin

inline fun FragmentActivity.fragmentTransaction(crossinline block: FragmentTransaction.() -> Unit) {
    with(supportFragmentManager.beginTransaction()) {
        configureGlassMotion(this)
        block()
        commit()
    }
}

@PublishedApi
internal fun FragmentActivity.configureGlassMotion(transaction: FragmentTransaction) {
    glassTransitions()?.let { transitions ->
        GlassMotion.trace(this, "fragment-transaction-configured")
        transaction.setCustomAnimations(transitions.enter, transitions.exit, transitions.popEnter, transitions.popExit)
    }
}

/** Shared by direct transactions and NavController actions, including their back-stack animations. */
fun FragmentActivity.glassNavOptions(): androidx.navigation.NavOptions? = glassTransitions()?.let { motion ->
    androidx.navigation.NavOptions.Builder()
        .setEnterAnim(motion.enter)
        .setExitAnim(motion.exit)
        .setPopEnterAnim(motion.popEnter)
        .setPopExitAnim(motion.popExit)
        .build()
}

private fun FragmentActivity.glassTransitions(): net.thunderbird.core.ui.theme.manager.GlassTransitions? {
    val settings = getKoin().getOrNull<GeneralSettingsManager>()?.getConfig() ?: return null
    return GlassMotion.transitions(
        this,
        settings.display.visualSettings.animationPreference,
    )
}

inline fun FragmentActivity.fragmentTransactionWithBackStack(
    name: String? = null,
    crossinline block: FragmentTransaction.() -> Unit,
) {
    fragmentTransaction {
        block()
        addToBackStack(name)
    }
}

fun <T : Fragment> T.withArguments(vararg argumentPairs: Pair<String, Any?>) = apply {
    arguments = bundleOf(*argumentPairs)
}
