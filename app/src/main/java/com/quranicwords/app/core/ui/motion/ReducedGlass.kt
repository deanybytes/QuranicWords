package com.quranicwords.app.core.ui.motion

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/** Set once at the app root (MainActivity) from [UserPreferencesDataStore.reduceGlassEffectsFlow][
 * com.quranicwords.app.core.data.datastore.UserPreferencesDataStore.reduceGlassEffectsFlow] -
 * the in-app "Reduce glossy effects" toggle (Settings screen). Defaults to false so composables
 * outside the provided subtree (e.g. previews) still render the full glass/sheen treatment. */
val LocalReduceGlassPreference = compositionLocalOf { false }

/**
 * True when the glass/sheen treatment (translucent [com.quranicwords.app.core.ui.components.GlassSurface]
 * fills, border highlights, animated sheens) should fall back to flat/opaque surfaces instead -
 * unlike [rememberReducedMotion], there's no OS-level signal to combine with here, so this is
 * purely the in-app preference. Every glass/sheen effect added in this pass must read this and
 * degrade to the plain pre-existing treatment rather than skipping content.
 */
@Composable
fun rememberReducedGlass(): Boolean = LocalReduceGlassPreference.current
