package com.quranicwords.app.core.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
import com.quranicwords.app.core.ui.motion.rememberReducedMotion

/**
 * The app's "glass box" recipe - a drop-in [Card]-shaped replacement (content stacks vertically
 * via [ColumnScope], same convention as Material3's own `Card`) with a translucent tinted fill
 * (letting whatever ambient pattern sits behind it show faintly through), a soft diagonal
 * white-to-transparent border highlight (same light-catching-edge idea as [QwLogo]'s sheen, just
 * static rather than animated) for depth (deliberately no drop shadow - see the "no shadow"
 * feedback that removed one), and - when [onClick] is supplied - this app's `pressDepth` feedback
 * plus a one-shot glossy reflection sweep on every tap
 * (same clip-to-shape/screen-blend technique as [QwLogo]'s ambient sheen and the correct-answer
 * badge's sheen in `AnswerFeedbackOverlay`, just triggered by a click instead of looping/one-shot-
 * on-reveal). Reuses existing theme color roles for [tint] (no new arbitrary colors) and keeps
 * [tonalAlpha] high enough (default 0.6) that content drawn at full `on*` opacity on top keeps
 * this app's existing WCAG-checked contrast - only the *fill* is translucent, never the text/icon
 * colors.
 *
 * [onClick] fully owns the click handling (applies `Modifier.clickable` itself) - don't also chain
 * a separate `.clickable(...)` onto [modifier], or the box ends up with two independent click
 * targets. Any "accent" decoration a caller needs (e.g. a colored outline for a "current" state)
 * must go through [accentBorderColor] rather than a caller-drawn `Modifier.border(...)` layered
 * onto [modifier] - a second border drawn outside this composable's own `.clip()/.background()`
 * chain measures against a subtly different effective edge and renders as two visibly mismatched
 * nested boxes (a real bug hit while building this out). Going through
 * [accentBorderColor] keeps the accent and the glass fill on the exact same draw call.
 *
 * Falls back to a plain opaque [Surface] (the pre-existing flat-card look, still clickable, no
 * reflection) under [rememberReducedGlass] - every glass surface in the app must go through this
 * composable rather than hand-rolling the translucent-background-plus-border pattern, so that
 * single toggle reaches everywhere at once.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MaterialTheme.shapes.medium,
    tint: Color = MaterialTheme.colorScheme.surfaceContainer,
    tonalAlpha: Float = 0.6f,
    accentBorderColor: Color? = null,
    accentBorderWidth: Dp = 2.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val reducedGlass = rememberReducedGlass()
    val reducedMotion = rememberReducedMotion()
    val interactionSource = remember { MutableInteractionSource() }
    var clickCount by remember { mutableIntStateOf(0) }
    val reflectionProgress = remember { Animatable(0f) }
    LaunchedEffect(clickCount) {
        if (clickCount > 0 && !reducedMotion && !reducedGlass) {
            reflectionProgress.snapTo(0f)
            reflectionProgress.animateTo(1f, animationSpec = tween(durationMillis = 500, easing = LinearEasing))
        }
    }
    val clickModifier = if (onClick != null) {
        Modifier
            .clickable(interactionSource = interactionSource, indication = null) {
                clickCount++
                onClick()
            }
            .pressDepth(interactionSource)
    } else {
        Modifier
    }

    val accentBorderModifier = if (accentBorderColor != null) {
        Modifier.border(width = accentBorderWidth, color = accentBorderColor, shape = shape)
    } else {
        Modifier
    }

    if (reducedGlass) {
        Surface(
            modifier = modifier.then(clickModifier).then(accentBorderModifier),
            shape = shape,
            color = tint,
            shadowElevation = 0.dp
        ) {
            Column(content = content)
        }
        return
    }
    Box(
        modifier = modifier
            .then(clickModifier)
            .clip(shape)
            .background(tint.copy(alpha = tonalAlpha))
            .then(
                if (accentBorderColor != null) {
                    accentBorderModifier
                } else {
                    Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                        ),
                        shape = shape
                    )
                }
            )
    ) {
        Column(content = content)
        if (onClick != null && reflectionProgress.value > 0f) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val w = size.width
                        val h = size.height
                        val bandWidth = w * 0.4f
                        val travel = w * 1.8f
                        val bandCenter = -w * 0.4f + reflectionProgress.value * travel
                        rotate(degrees = 20f, pivot = Offset(w / 2f, h / 2f)) {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colorStops = arrayOf(
                                        0f to Color.Transparent,
                                        0.5f to Color.White.copy(alpha = 0.55f),
                                        1f to Color.Transparent
                                    ),
                                    start = Offset(bandCenter - bandWidth / 2f, 0f),
                                    end = Offset(bandCenter + bandWidth / 2f, 0f)
                                ),
                                topLeft = Offset(-w, -h),
                                size = Size(w * 3f, h * 3f),
                                blendMode = BlendMode.Screen
                            )
                        }
                    }
            )
        }
    }
}
