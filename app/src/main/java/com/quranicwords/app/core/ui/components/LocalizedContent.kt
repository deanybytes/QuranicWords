package com.quranicwords.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import com.quranicwords.app.core.domain.model.Language

/** The user's chosen app language, for localizing dynamic (non-string-resource) content such as
 * bundled lesson/exercise text. Falls back to [Language.ENGLISH] when the active device locale
 * doesn't match any supported [Language] tag. */
@Composable
fun rememberSelectedLanguage(): Language {
    val locales = LocalConfiguration.current.locales
    return Language.fromTag(locales.get(0)?.language) ?: Language.ENGLISH
}
