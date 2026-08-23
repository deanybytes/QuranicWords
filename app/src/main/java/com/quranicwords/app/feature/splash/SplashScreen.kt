package com.quranicwords.app.feature.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.CrescentMoonMotif
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.StarfieldMotif
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
 * (redesign plan): an ambient low-opacity geometric lattice behind a big coin-flip logo reveal -
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
    val density = LocalDensity.current
    // Slow, low-stiffness spring (longer settle than MotionSpecs.celebratory()) so the pop-in
    // reads as a deliberate reveal rather than a blink-and-you-miss-it flash.
    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.3f,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow)
        },
        label = "splashLogoScale"
    )
    // A tossed-coin spin around the Y axis (three full turns, decelerating to a stop face-on) -
    // same rotationY/cameraDistance mechanics as Qw3DFlipCard, just a longer throw. Reads as a
    // coin landing rather than a page unfurling, but stays abstract/geometric - no literal
    // book/page depicted, per this file's guardrail below.
    val logoRotationY by animateFloatAsState(
        targetValue = if (revealed) 0f else -1080f,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        },
        label = "splashLogoRotationY"
    )
    val logoMirrorFix = ((logoRotationY % 360f) + 360f) % 360f
    val logoScaleX = if (logoMirrorFix in 90f..270f) -1f else 1f
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
                    size = 160.dp,
                    modifier = Modifier.graphicsLayer {
                        scaleX = logoScale * logoScaleX
                        scaleY = logoScale
                        rotationY = logoRotationY
                        cameraDistance = 12f * density.density
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
