package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import nl.dionsegijn.konfetti.compose.KonfettiView
import nl.dionsegijn.konfetti.core.Party
import nl.dionsegijn.konfetti.core.Position
import nl.dionsegijn.konfetti.core.emitter.Emitter
import java.util.concurrent.TimeUnit

/** How much a lesson moment deserves celebrating - scales particle count/spread. */
enum class CelebrationIntensity(internal val particleCount: Int, internal val spread: Int) {
    PASSED(particleCount = 40, spread = 70),
    PERFECT(particleCount = 120, spread = 180)
}

/**
 * Lightweight, non-scripture celebration burst for points/badge/streak UI. Per the redesign
 * plan's guardrail against treating scripture as decoration, this must never be invoked over
 * verse/Arabic-content text - only over gamification chrome (lesson summary badges, streak
 * milestones). No-ops entirely under reduced motion, since this is pure decoration with no
 * informational content to preserve.
 */
@Composable
fun CelebrationBurst(intensity: CelebrationIntensity, modifier: Modifier = Modifier) {
    if (rememberReducedMotion()) return

    // Brand gold/emerald/mint family, matching the theme's tertiary (gold) and primary (emerald)
    // roles - see core/ui/theme/Color.kt. Konfetti's public examples pass colors without an alpha
    // prefix (opaque RGB); matched here rather than introducing an untested 0xFF-prefixed form.
    val colors = remember { listOf(0xD4AF37, 0x0B6E4F, 0xF9A825, 0xB7F2D5) }
    val party = remember(intensity) {
        Party(
            speed = 8f,
            maxSpeed = 22f,
            damping = 0.9f,
            spread = intensity.spread,
            colors = colors,
            emitter = Emitter(duration = 150, TimeUnit.MILLISECONDS).max(intensity.particleCount),
            position = Position.Relative(0.5, 0.3)
        )
    }
    KonfettiView(modifier = modifier.fillMaxSize(), parties = listOf(party))
}
