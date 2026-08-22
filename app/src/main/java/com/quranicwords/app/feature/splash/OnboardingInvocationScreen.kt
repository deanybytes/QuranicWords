package com.quranicwords.app.feature.splash

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.quranicwords.app.core.ui.motion.rememberReducedMotion

/**
 * Plays the opening invocation ([OpeningInvocationSequence]) exactly once, at the true end of
 * onboarding (`Route.OnboardingInvocation`, navigated to from DailyGoalSelect) - not before
 * Language Select the way the old "always show at cold start" placement did, so it always renders
 * in the learner's own just-chosen language rather than the device's raw system locale. [onFinished]
 * navigates on to Home, replacing this route in the back stack.
 */
@Composable
fun OnboardingInvocationScreen(onFinished: () -> Unit) {
    val reducedMotion = rememberReducedMotion()
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        OpeningInvocationSequence(reducedMotion = reducedMotion, onFinished = onFinished)
    }
}
