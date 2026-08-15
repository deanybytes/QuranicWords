package com.quranicwords.app.feature.splash

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
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
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberReducedMotion

/**
 * The in-app decision screen shown after the platform SplashScreen API's brief cold-start
 * window - runs first-launch content seeding and routes to wherever onboarding left off.
 *
 * The ceremonial "opening" moment (redesign plan): an ambient low-opacity geometric lattice
 * behind a spring-scaled logo reveal - abstract/stylized only, never a literal Mushaf/page
 * graphic, per the plan's guardrail against rendering scripture as decoration.
 */
@Composable
fun SplashScreen(
    onNavigateTo: (Route) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val destination by viewModel.destination.collectAsStateWithLifecycle()

    LaunchedEffect(destination) {
        destination?.let(onNavigateTo)
    }

    val reducedMotion = rememberReducedMotion()
    var revealed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { revealed = true }
    val logoScale by animateFloatAsState(
        targetValue = if (revealed) 1f else 0.6f,
        animationSpec = if (reducedMotion) tween(durationMillis = 0) else MotionSpecs.celebratory(),
        label = "splashLogoScale"
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
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                QwLogo(modifier = Modifier.graphicsLayer { scaleX = logoScale; scaleY = logoScale })
                Spacer(modifier = Modifier.height(24.dp))
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
