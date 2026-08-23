package com.quranicwords.app

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.OvershootInterpolator
import kotlin.math.min
import kotlin.math.sin
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.navigation.QwNavHost
import com.quranicwords.app.core.ui.motion.LocalReduceGlassPreference
import com.quranicwords.app.core.ui.motion.LocalReduceMotionPreference
import com.quranicwords.app.core.ui.theme.QuranicWordsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // The platform SplashScreen API only animates windowSplashScreenAnimatedIcon when it's an
        // AnimatedVectorDrawable/AnimationDrawable; ic_qw_mark is a static PNG, so without this
        // listener the mark never actually animates. This plays a 3D coin-flip on the real icon
        // View as the splash exits: a rotationY spin (with an enlarged cameraDistance so it reads
        // as a genuine perspective flip rather than a flat horizontal squash) plus an overshoot
        // scale pop, landing face-up before cross-fading into the Compose content underneath.
        // Respects the OS "Remove animations" accessibility setting (same check as
        // core/ui/motion/ReducedMotion.kt's rememberReducedMotion) since the in-app "Reduce
        // motion" DataStore preference isn't reliably loaded yet this early.
        val systemReducedMotion = runCatching {
            Settings.Global.getFloat(contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            if (systemReducedMotion) {
                splashScreenView.remove()
                return@setOnExitAnimationListener
            }
            val icon = splashScreenView.iconView
            val cameraDistance = 12000f * resources.displayMetrics.density
            icon.cameraDistance = cameraDistance

            // A rim glow (ring-shaped radial gradient - transparent at the center, so it never
            // washes over the icon's own face) plus a screen-blended glossy sheen sweeping across
            // it, clipped to the icon's own circle - the coin/glass highlight that a plain
            // ImageView (all windowSplashScreenAnimatedIcon can be here) has no way to render on
            // its own. Sized and positioned to exactly match iconView's laid-out bounds, and
            // added as its sibling so it inherits the same parent-alpha fade-out below.
            val glowSheenOverlay = GlowSheenOverlayView(this).apply {
                layoutParams = ViewGroup.LayoutParams(icon.width, icon.height)
                x = icon.x
                y = icon.y
                this.cameraDistance = cameraDistance
            }
            (splashScreenView.view as? ViewGroup)?.addView(glowSheenOverlay)

            val flip = ObjectAnimator.ofFloat(icon, View.ROTATION_Y, 0f, 720f).apply {
                duration = 900
                interpolator = AccelerateDecelerateInterpolator()
            }
            val overlayFlip = ObjectAnimator.ofFloat(glowSheenOverlay, View.ROTATION_Y, 0f, 720f).apply {
                duration = 900
                interpolator = AccelerateDecelerateInterpolator()
            }
            val pop = ObjectAnimator.ofPropertyValuesHolder(
                icon,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f, 1f)
            ).apply {
                duration = 900
                interpolator = OvershootInterpolator()
            }
            val overlayPop = ObjectAnimator.ofPropertyValuesHolder(
                glowSheenOverlay,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.18f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.18f, 1f)
            ).apply {
                duration = 900
                interpolator = OvershootInterpolator()
            }
            // Glow pulses up then back down across the flip (peaking mid-turn); the sheen sweeps
            // once across the badge in the same window, so both read as tied to the coin turning
            // rather than looping independently of it.
            val glowSheenProgress = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 900
                addUpdateListener {
                    val t = it.animatedValue as Float
                    glowSheenOverlay.glowAlpha = sin(t * Math.PI).toFloat().coerceIn(0f, 1f)
                    glowSheenOverlay.sheenProgress = t
                    glowSheenOverlay.invalidate()
                }
            }
            val fadeOut = ObjectAnimator.ofFloat(splashScreenView.view, View.ALPHA, 1f, 0f).apply {
                duration = 300
            }
            AnimatorSet().apply {
                playTogether(flip, overlayFlip, pop, overlayPop, glowSheenProgress)
                play(fadeOut).after(flip)
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        splashScreenView.remove()
                    }
                })
                start()
            }
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
            val reduceGlassEffects by viewModel.reduceGlassEffects.collectAsStateWithLifecycle()
            val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()

            // AppCompatDelegate.setApplicationLocales() updates the process-wide Configuration
            // (natively via LocaleManager on API 33+, via AppCompatActivity's own compat shim
            // below that) - but neither path reliably re-resolves resource-qualifier lookups
            // (values-bn/, values-fr/, ...) inside an already-composed Compose tree without an
            // explicit recreate(). Verified on a real API 37 device: the OS-level app locale did
            // change (confirmed via `cmd locale get-app-locales`), but stringResource() calls kept
            // resolving English until the Activity was recreated - so recreate() is called
            // unconditionally here, not just below API 33 as an earlier version of this code
            // assumed. The equality check avoids a recreate loop, since this effect re-fires with
            // the same `language` right after recreate() runs.
            //
            // `language` is null before onboarding's LanguageSelect step has ever run - falling
            // back to English (rather than no-op, which would leave resource resolution to follow
            // the device's raw system locale) is what makes first-setup screens always start in
            // English regardless of device locale, per the product requirement. This never writes
            // to DataStore itself - see MainViewModel.init's guard for why persisting it here would
            // wrongly short-circuit the LanguageSelect step.
            LaunchedEffect(language) {
                val targetTag = language?.tag ?: Language.ENGLISH.tag
                val targetLocales = LocaleListCompat.forLanguageTags(targetTag)
                if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                    recreate()
                }
            }

            val scaledDensity = LocalDensity.current.let { base ->
                Density(density = base.density, fontScale = base.fontScale * fontScale.multiplier)
            }

            CompositionLocalProvider(
                LocalReduceMotionPreference provides reduceMotion,
                LocalReduceGlassPreference provides reduceGlassEffects,
                LocalDensity provides scaledDensity
            ) {
                QuranicWordsTheme(themeMode = themeMode) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        QwNavHost()
                    }
                }
            }
        }
    }
}

/**
 * Plain-Android-View companion to the splash coin-flip in [MainActivity.onCreate] - the native
 * SplashScreen icon (an ImageView the platform owns) has no way to render QwLogo's Compose-based
 * glow/gloss, so this redraws the same two effects with `android.graphics` shaders instead:
 * 1. A rim glow - a ring-shaped [RadialGradient] (transparent at the center, so it never washes
 *    over the icon's own face, peaking in a band right at its edge, fading to transparent again
 *    past it).
 * 2. A glossy sheen - a diagonal [LinearGradient] band, clipped to the icon's own circle and
 *    screen-blended (`PorterDuff.Mode.SCREEN`, which needs a software layer to composite
 *    correctly pre-API 29 - see the `setLayerType` call below) so it reads as light glinting
 *    across the coin's face rather than a flat wash.
 * [glowAlpha]/[sheenProgress] are driven frame-by-frame by the `ValueAnimator` in the exit
 * listener, synced to the same timeline as the flip itself.
 */
private class GlowSheenOverlayView(context: android.content.Context) : View(context) {
    var glowAlpha: Float = 0f
    var sheenProgress: Float = 0f

    private val glowColor = Color.parseColor("#EBC971")
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val sheenPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SCREEN)
    }
    private val clipPath = Path()

    init {
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0f || h <= 0f) return
        val cx = w / 2f
        val cy = h / 2f
        val r = min(w, h) / 2f

        val outerR = r * 1.35f
        val peak = 0.80f
        val glowArgb = Color.argb((glowAlpha * 255).toInt().coerceIn(0, 255), Color.red(glowColor), Color.green(glowColor), Color.blue(glowColor))
        glowPaint.shader = RadialGradient(
            cx, cy, outerR,
            intArrayOf(Color.TRANSPARENT, Color.TRANSPARENT, glowArgb, Color.TRANSPARENT),
            floatArrayOf(0f, peak - 0.12f, peak, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, outerR, glowPaint)

        clipPath.reset()
        clipPath.addCircle(cx, cy, r, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.rotate(25f, cx, cy)
        val diameter = r * 2f
        val bandWidth = diameter * 0.4f
        val travel = diameter * 1.8f
        val bandCenter = -diameter * 0.4f + sheenProgress * travel
        sheenPaint.shader = LinearGradient(
            bandCenter - bandWidth / 2f, 0f,
            bandCenter + bandWidth / 2f, 0f,
            intArrayOf(Color.TRANSPARENT, Color.argb(160, 255, 255, 255), Color.TRANSPARENT),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(cx - diameter * 1.5f, cy - diameter * 1.5f, cx + diameter * 1.5f, cy + diameter * 1.5f, sheenPaint)
        canvas.restore()
    }
}
