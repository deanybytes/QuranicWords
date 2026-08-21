package com.quranicwords.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/** The shared decorative-motif vocabulary for this app's custom-drawn (never externally
 * licensed) illustration language - crescent moon, starfield, mosque silhouette, abstract book,
 * in that priority order. Every new motif use elsewhere in the app should go through this
 * dispatcher rather than reaching for one of the individual Composables directly, so the
 * vocabulary stays a single, deliberate set instead of accumulating near-duplicate icons per
 * screen. See docs/UI_GUIDELINES.md's "Motif vocabulary" section. */
enum class MotifKind { CRESCENT, STARFIELD, MOSQUE, BOOK }

@Composable
fun IslamicMotif(kind: MotifKind, modifier: Modifier = Modifier, color: Color = Color.Unspecified) {
    when (kind) {
        MotifKind.CRESCENT -> CrescentMoonMotif(modifier, color)
        MotifKind.STARFIELD -> StarfieldMotif(modifier = modifier, color = color)
        MotifKind.MOSQUE -> MosqueSilhouetteMotif(modifier, color)
        MotifKind.BOOK -> AbstractBookMotif(modifier, color)
    }
}
