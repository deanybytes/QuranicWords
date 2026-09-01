package com.quranicwords.app

import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Bundle
import java.util.Locale
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.os.LocaleListCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.navigation.QwNavHost
import com.quranicwords.app.core.ui.components.LocalAppLanguage
import com.quranicwords.app.core.ui.motion.LocalReduceGlassPreference
import com.quranicwords.app.core.ui.motion.LocalReduceMotionPreference
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.LocalQuranFontStyle
import com.quranicwords.app.core.ui.theme.QuranicWordsTheme
import com.quranicwords.app.core.ui.theme.toFontFamily
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
            val reduceGlassEffects by viewModel.reduceGlassEffects.collectAsStateWithLifecycle()
            val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
            val fontStyle by viewModel.fontStyle.collectAsStateWithLifecycle()

            val selectedLanguage = language ?: Language.ENGLISH
            val currentContext = LocalContext.current

            LaunchedEffect(language) {
                val currentLang = language
                if (currentLang != null) {
                    val targetTag = currentLang.tag
                    val currentAppLocales = AppCompatDelegate.getApplicationLocales()
                    if (currentAppLocales.toLanguageTags() != targetTag) {
                        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(targetTag))
                    }
                }
            }

            val targetLocale = remember(selectedLanguage) {
                Locale.forLanguageTag(selectedLanguage.tag)
            }

            val configuration = remember(targetLocale) {
                Configuration(currentContext.resources.configuration).apply {
                    setLocale(targetLocale)
                    setLayoutDirection(targetLocale)
                }
            }

            val localizedContext = remember(currentContext, configuration) {
                val configContext = currentContext.createConfigurationContext(configuration)
                object : ContextWrapper(currentContext) {
                    override fun getResources(): Resources = configContext.resources
                    override fun getAssets(): AssetManager = configContext.assets
                }
            }

            val scaledDensity = LocalDensity.current.let { base ->
                Density(density = base.density, fontScale = base.fontScale * fontScale.multiplier)
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides configuration,
                LocalAppLanguage provides selectedLanguage,
                LocalReduceMotionPreference provides reduceMotion,
                LocalReduceGlassPreference provides reduceGlassEffects,
                LocalDensity provides scaledDensity,
                LocalQuranFontFamily provides fontStyle.toFontFamily(),
                LocalQuranFontStyle provides fontStyle
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
