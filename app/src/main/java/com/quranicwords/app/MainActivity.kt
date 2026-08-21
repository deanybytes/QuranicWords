package com.quranicwords.app

import android.os.Bundle
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
import com.quranicwords.app.core.navigation.QwNavHost
import com.quranicwords.app.core.ui.motion.LocalReduceMotionPreference
import com.quranicwords.app.core.ui.theme.QuranicWordsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
            val language by viewModel.language.collectAsStateWithLifecycle()
            val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
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
            LaunchedEffect(language) {
                val target = language ?: return@LaunchedEffect
                val targetLocales = LocaleListCompat.forLanguageTags(target.tag)
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
