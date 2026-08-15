package com.quranicwords.app.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import kotlinx.coroutines.delay

/**
 * Wraps [content] in a fade+slide-up entrance delayed by `index * 60ms`, so a screen's list of
 * option cards reveals top-to-bottom instead of appearing all at once. Shows immediately (no
 * delay/animation) under reduced motion.
 */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val reducedMotion = rememberReducedMotion()
    var visible by remember { mutableStateOf(reducedMotion) }

    LaunchedEffect(Unit) {
        if (!reducedMotion) {
            delay(60L * index)
        }
        visible = true
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(260)) +
            slideInVertically(animationSpec = tween(260)) { it / 4 },
        modifier = modifier
    ) {
        content()
    }
}
