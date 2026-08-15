package com.quranicwords.app

import android.os.Build
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

            // AppCompatDelegate.setApplicationLocales() only actually changes the process-wide
            // Configuration on API 33+ (native LocaleManager). On API 24-32 it relies on
            // AppCompatActivity's own compat shim, which requires an explicit recreate() to take
            // effect - without it, resource-qualifier resolution (values-bn/) and
            // LocalConfiguration never update. The equality check avoids a recreate loop, since
            // this effect re-fires with the same `language` right after recreate() runs.
            LaunchedEffect(language) {
                val target = language ?: return@LaunchedEffect
                val targetLocales = LocaleListCompat.forLanguageTags(target.tag)
                if (AppCompatDelegate.getApplicationLocales() != targetLocales) {
                    AppCompatDelegate.setApplicationLocales(targetLocales)
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        recreate()
                    }
                }
            }

            CompositionLocalProvider(LocalReduceMotionPreference provides reduceMotion) {
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
