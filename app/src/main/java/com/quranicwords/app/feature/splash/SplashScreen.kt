package com.quranicwords.app.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.CrescentMoonMotif
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.StarfieldMotif
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberReducedMotion

/**
 * The in-app decision screen shown after the platform SplashScreen API's brief cold-start
 * window - runs first-launch content seeding and routes to wherever onboarding left off.
 *
 * Plays [OpeningInvocationSequence] only when [destination] resolves straight to [Route.Home] -
 * i.e. every later cold start for an already-onboarded learner, not a first-time user still
 * headed into onboarding. A first-time user gets the invocation later instead, at the true end
 * of onboarding (`Route.OnboardingInvocation`, from `DailyGoalSelect`) - playing it here for that
 * case would render it before [Route.LanguageSelect] has ever run, in the device's raw system
 * locale rather than the learner's own chosen one. See that route's doc comment.
 *
 * After the invocation finishes (or is tapped through early), the ceremonial "opening" moment
 * (redesign plan): an ambient low-opacity geometric lattice behind a spring-scaled logo reveal -
 * abstract/stylized only, never a literal Mushaf/page graphic, per the plan's guardrail against
 * rendering scripture as decoration.
 */
@Composable
fun SplashScreen(
    onNavigateTo: (Route) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()
    val reducedMotion = rememberReducedMotion()
    var invocationFinished by remember { mutableStateOf(false) }
    val playsInvocationHere = destination == Route.Home

    LaunchedEffect(destination, invocationFinished) {
        val dest = destination ?: return@LaunchedEffect
        if (dest != Route.Home || invocationFinished) onNavigateTo(dest)
    }

    if (playsInvocationHere && !invocationFinished) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            OpeningInvocationSequence(
                reducedMotion = reducedMotion,
                onFinished = { invocationFinished = true }
            )
        }
        return
    }
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    // Wider start delta than before (0.4f, was 0.6f) - MotionSpecs.celebratory()'s bouncy spring
    // overshoots past 1f proportionally to how far it travels, so this alone makes the reveal
    // read as a genuine pop rather than a gentle settle, without hand-tuning a new spring.
    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.4f,
        animationSpec = if (reducedMotion) tween(durationMillis = 0) else MotionSpecs.celebratory(),
        label = "splashLogoScale"
    )
    // A slight unfurl-from-rotated-start on the mark itself, on top of QwLogo's own idle glow -
    // settles to upright as the scale-in spring finishes, echoing an opening/unfolding motion
    // without literally depicting a book or page (see this file's own guardrail below).
    val logoRotation by animateFloatAsState(
        targetValue = if (revealed) 0f else -22f,
        animationSpec = if (reducedMotion) tween(durationMillis = 0) else MotionSpecs.celebratory(),
        label = "splashLogoRotation"
    )
    val patternAlpha by animateFloatAsState(
        targetValue = if (revealed) 0.08f else 0f,
        animationSpec = if (reducedMotion) tween(durationMillis = 0) else tween(durationMillis = 900),
        label = "splashPatternAlpha"
    )

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize()) {
            GeometricPatternBackground(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                alpha = patternAlpha
            )
            // Low-alpha companions to the geometric lattice, same reveal timing - a small
            // crescent tucked in the corner and a scattered starfield behind the logo, both
            // custom-drawn (see docs/UI_GUIDELINES.md's "Motif vocabulary" section).
            CrescentMoonMotif(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(24.dp)
                    .size(40.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = (patternAlpha * 1.5f).coerceIn(0f, 1f))
            )
            StarfieldMotif(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = patternAlpha)
            )
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QwLogo(
                    modifier = Modifier.graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        rotationZ = logoRotation
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
