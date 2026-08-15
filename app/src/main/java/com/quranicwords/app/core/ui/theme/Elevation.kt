package com.quranicwords.app.core.ui.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Named elevation scale for the app's "3D box" visual language - depth conveyed through
 * shadow/elevation and press-depth motion (see [com.quranicwords.app.core.ui.motion.pressDepth])
 * rather than literal 3D geometry, since Compose has no true 3D primitive. Use these tokens
 * instead of ad hoc `dp` literals so depth reads consistently across the whole app: a card at
 * rest is always [raised], the same card mid-press is always [pressed], and only genuinely
 * hero/floating elements (flip-cards, celebratory badges) reach [floating].
 */
object Elevation {
    val flat: Dp = 0.dp
    val pressed: Dp = 1.dp
    val raised: Dp = 4.dp
    val floating: Dp = 12.dp
}
