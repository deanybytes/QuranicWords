package com.quranicwords.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import com.quranicwords.app.core.domain.model.Language

/**
 * CompositionLocal providing the active [Language] chosen by the user.
 * Defaults to [Language.ENGLISH].
 */
val LocalAppLanguage = compositionLocalOf { Language.ENGLISH }

/**
 * The user's chosen app language, for localizing dynamic (non-string-resource) content such as
 * bundled lesson/exercise text.
 */
@Composable
fun rememberSelectedLanguage(): Language = LocalAppLanguage.current

