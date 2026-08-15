package com.quranicwords.app.core.ui.motion

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

/**
 * Shared animation-spec vocabulary so every screen pulls from one tuned set instead of the ad hoc
 * per-file `tween(600)`-style values the codebase had before this pass.
 */
object MotionSpecs {
    /** Quick, responsive - press states, selection, option-card taps. */
    fun <T> snappy(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = Spring.StiffnessMedium
    )

    /** Soft, unhurried - fades, reveals, non-interactive transitions. */
    fun <T> gentle(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioLowBouncy,
        stiffness = Spring.StiffnessLow
    )

    /** Bouncy, attention-grabbing - lesson-complete badges, streak milestones, unlock reveals. */
    fun <T> celebratory(): FiniteAnimationSpec<T> = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMediumLow
    )

    /** Count-up numbers (points/streak badges) - a fixed duration reads better than a spring for
     * a rapidly-changing integer display. */
    val countUp = tween<Int>(durationMillis = 600)
}
