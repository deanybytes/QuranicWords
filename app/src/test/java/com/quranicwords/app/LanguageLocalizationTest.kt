package com.quranicwords.app

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import androidx.test.core.app.ApplicationProvider
import com.quranicwords.app.core.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class LanguageLocalizationTest {

    private fun getLocaleForLanguage(lang: Language): Locale {
        return when (lang) {
            Language.INDONESIAN -> Locale.forLanguageTag("in")
            else -> Locale.forLanguageTag(lang.tag)
        }
    }

    @Test
    fun testAllLanguagesLoadNonEnglishStrings() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        val enString = appContext.getString(R.string.settings_title)
        assertEquals("Settings", enString)

        for (lang in Language.entries) {
            if (lang == Language.ENGLISH) continue

            val targetLocale = getLocaleForLanguage(lang)
            val config = Configuration(appContext.resources.configuration).apply {
                setLocale(targetLocale)
                setLocales(LocaleList(targetLocale))
                setLayoutDirection(targetLocale)
            }
            val localizedContext = appContext.createConfigurationContext(config)
            val localizedString = localizedContext.resources.getString(R.string.settings_title)

            println("Language: ${lang.name} (${lang.tag}) -> settings_title: '$localizedString'")
            assertNotEquals("Language ${lang.name} should not have English 'Settings'", "Settings", localizedString)
        }
    }

    @Test
    fun testExitConfirmationDialogStringsLocalized() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        for (lang in Language.entries) {
            if (lang == Language.ENGLISH) continue

            val targetLocale = getLocaleForLanguage(lang)
            val config = Configuration(appContext.resources.configuration).apply {
                setLocale(targetLocale)
                setLocales(LocaleList(targetLocale))
                setLayoutDirection(targetLocale)
            }
            val localizedContext = appContext.createConfigurationContext(config)
            val exitTitle = localizedContext.resources.getString(R.string.app_exit_confirm_title)
            val exitDesc = localizedContext.resources.getString(R.string.app_exit_confirm_message)

            println("Language: ${lang.name} -> exitTitle: '$exitTitle', exitDesc: '$exitDesc'")
            assertNotEquals("Language ${lang.name} exitTitle should be localized", "Exit App", exitTitle)
        }
    }

    @Test
    fun testWalkthroughStringsLocalized() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        for (lang in Language.entries) {
            if (lang == Language.ENGLISH) continue

            val targetLocale = getLocaleForLanguage(lang)
            val config = Configuration(appContext.resources.configuration).apply {
                setLocale(targetLocale)
                setLocales(LocaleList(targetLocale))
                setLayoutDirection(targetLocale)
            }
            val localizedContext = appContext.createConfigurationContext(config)
            val title1 = localizedContext.resources.getString(R.string.walkthrough_page1_title)
            val nextBtn = localizedContext.resources.getString(R.string.walkthrough_next)

            println("Language: ${lang.name} -> walkthrough_page1_title: '$title1', next: '$nextBtn'")
            assertNotEquals("Language ${lang.name} walkthrough_page1_title should be localized", "Welcome to Quranic Words", title1)
        }
    }

    @Test
    fun testNotificationStringsLocalized() {
        val appContext = ApplicationProvider.getApplicationContext<Context>()
        for (lang in Language.entries) {
            if (lang == Language.ENGLISH) continue

            val targetLocale = getLocaleForLanguage(lang)
            val config = Configuration(appContext.resources.configuration).apply {
                setLocale(targetLocale)
                setLocales(LocaleList(targetLocale))
                setLayoutDirection(targetLocale)
            }
            val localizedContext = appContext.createConfigurationContext(config)
            val notifTitle = localizedContext.resources.getString(R.string.notification_streak_title)

            println("Language: ${lang.name} -> notifTitle: '$notifTitle'")
            assertNotEquals("Language ${lang.name} notification title should be localized", "Your streak is waiting", notifTitle)
        }
    }
}
