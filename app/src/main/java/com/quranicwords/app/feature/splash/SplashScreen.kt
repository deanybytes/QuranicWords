package com.quranicwords.app.feature.splash

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.CrescentMoonMotif
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.StarfieldMotif
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.BrandGreen
import com.quranicwords.app.core.ui.theme.BrandLightGreen

/**
 * The in-app decision screen shown after the platform SplashScreen API's cold start -
 * runs first-launch content seeding and routes to wherever onboarding left off.
 *
 * Displays an ambient geometric background, crescent moon and starfield motifs,
 * with a luminous rotating glow around the logo that serves as the dynamic loading and
 * progress indicator.
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

    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.3f,
        animationSpec = if (reducedMotion) {
            tween(durationMillis = 0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessVeryLow)
        },
        label = "splashLogoScale"
    )

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
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            GeometricPatternBackground(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primary,
                alpha = patternAlpha
            )
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

            SplashLoadingLogo(
                logoScale = logoScale,
                logoScaleX = logoScaleX,
                logoRotationY = logoRotationY,
                density = density,
                reducedMotion = reducedMotion
            )
        }
    }
}

@Composable
private fun SplashLoadingLogo(
    logoScale: Float,
    logoScaleX: Float,
    logoRotationY: Float,
    density: Density,
    reducedMotion: Boolean
) {
    val infiniteTransition = rememberInfiniteTransition(label = "splashGlowProgress")
    val orbitRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (reducedMotion) 0f else 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "orbitRotation"
    )
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue = if (reducedMotion) 0.55f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = Modifier.size(240.dp),
        contentAlignment = Alignment.Center
    ) {
        // Rotating luminous glow aura around the perimeter
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                }
        ) {
            val center = this.center
            val ringRadius = (160.dp.toPx() / 2f) + 14.dp.toPx()

            rotate(degrees = orbitRotation, pivot = center) {
                // Diffuse outer ambient glow band
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            BrandGold.copy(alpha = 0.12f * glowPulse),
                            BrandGreen.copy(alpha = 0.35f * glowPulse),
                            BrandLightGreen.copy(alpha = 0.70f * glowPulse),
                            BrandGold.copy(alpha = 0.90f * glowPulse),
                            Color.Transparent
                        ),
                        center = center
                    ),
                    radius = ringRadius + 6.dp.toPx(),
                    center = center,
                    style = Stroke(width = 18.dp.toPx(), cap = StrokeCap.Round)
                )

                // Vibrant crisp core arc circling around logo
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            Color.Transparent,
                            BrandGold.copy(alpha = 0.25f),
                            BrandGreen.copy(alpha = 0.65f * glowPulse),
                            BrandLightGreen.copy(alpha = 0.85f * glowPulse),
                            BrandGold.copy(alpha = 1f * glowPulse),
                            Color.White.copy(alpha = 0.95f * glowPulse)
                        ),
                        center = center
                    ),
                    startAngle = 0f,
                    sweepAngle = 290f,
                    useCenter = false,
                    topLeft = Offset(center.x - ringRadius, center.y - ringRadius),
                    size = Size(ringRadius * 2f, ringRadius * 2f),
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                )

                // Orbiting radiant comet head at arc tip
                val headAngleRad = Math.toRadians(290.0)
                val headX = center.x + ringRadius * Math.cos(headAngleRad).toFloat()
                val headY = center.y + ringRadius * Math.sin(headAngleRad).toFloat()
                val headPos = Offset(headX, headY)

                drawCircle(
                    color = BrandGold.copy(alpha = 0.8f * glowPulse),
                    radius = 9.dp.toPx(),
                    center = headPos
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.5.dp.toPx(),
                    center = headPos
                )
            }
        }

        // Center QwLogo with 3D reveal
        QwLogo(
            size = 160.dp,
            modifier = Modifier.graphicsLayer {
                scaleX = logoScale * logoScaleX
                scaleY = logoScale
                rotationY = logoRotationY
                cameraDistance = 12f * density.density
            }
        )
    }
}
