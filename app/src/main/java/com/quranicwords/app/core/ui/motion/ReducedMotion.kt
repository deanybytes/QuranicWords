package com.quranicwords.app.core.ui.motion

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/** Set once at the app root (MainActivity) from [UserPreferencesDataStore.reduceMotionFlow][
 * com.quranicwords.app.core.data.datastore.UserPreferencesDataStore.reduceMotionFlow] -
 * the in-app "Reduce motion" toggle (Settings screen). Defaults to false so composables outside
 * the provided subtree (e.g. previews) still render normally. */
val LocalReduceMotionPreference = compositionLocalOf { false }

/**
 * True when motion should be minimized: either the user enabled the in-app "Reduce motion"
 * setting, or the OS-level "Remove animations" accessibility setting is on
 * ([Settings.Global.ANIMATOR_DURATION_SCALE] == 0). Every new animated component added in this
 * pass (flip cards, Rive flame, Lottie one-shots, celebration bursts, nav transitions) must read
 * this and degrade to an instant/static end-state rather than skipping content or crashing.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val inAppPreference = LocalReduceMotionPreference.current
    val context = LocalContext.current
    val systemReducedMotion = remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    return inAppPreference || systemReducedMotion
}
