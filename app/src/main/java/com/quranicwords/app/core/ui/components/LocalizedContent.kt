package com.quranicwords.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** Whether Bangla is the user's chosen app language, for localizing dynamic (non-string-resource)
 * content such as bundled lesson/exercise text. */
@Composable
fun rememberIsBanglaSelected(): Boolean {
    val locales = LocalConfiguration.current.locales
    return locales.get(0)?.language == "bn"
}
