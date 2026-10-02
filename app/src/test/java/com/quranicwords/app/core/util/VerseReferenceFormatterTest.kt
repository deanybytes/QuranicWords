package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VerseReferenceFormatterTest {

    @Test
    fun `formats reference with localized Surah name in English`() {
        assertEquals("Surah Al-Baqarah 2:23", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.ENGLISH))
        assertEquals("Surah Ya-Sin 36:62", VerseReferenceFormatter.format("Surah 36:62", Language.ENGLISH))
        assertEquals("Surah Al-Baqarah 2:255", VerseReferenceFormatter.format("2:255", Language.ENGLISH))
    }

    @Test
    fun `formats reference with localized Surah name in Bangla`() {
        assertEquals("সূরা আল-বাকারা ২:২৩", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.BANGLA))
        assertEquals("সূরা ইয়াসীন ৩৬:৬২", VerseReferenceFormatter.format("Surah 36:62", Language.BANGLA))
        assertEquals("সূরা আল-বাকারা ২:২৫৫", VerseReferenceFormatter.format("2:255", Language.BANGLA))
    }

    @Test
    fun `formats reference with localized Surah name in Urdu`() {
        assertEquals("سورۃ البقرۃ ۲:۲۳", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.URDU))
        assertEquals("سورۃ یٰسین ۳۶:۶۲", VerseReferenceFormatter.format("Surah 36:62", Language.URDU))
        assertEquals("سورۃ البقرۃ ۲:۲۵۵", VerseReferenceFormatter.format("2:255", Language.URDU))
    }

    @Test
    fun `formats reference with localized Surah name in Turkish`() {
        assertEquals("Sure Bakara 2:23", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.TURKISH))
        assertEquals("Sure Yâsîn 36:62", VerseReferenceFormatter.format("Surah 36:62", Language.TURKISH))
    }

    @Test
    fun `formats reference with localized Surah name in French`() {
        assertEquals("Sourate Al-Baqara 2:23", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.FRENCH))
        assertEquals("Sourate Ya-Sin 36:62", VerseReferenceFormatter.format("Surah 36:62", Language.FRENCH))
    }

    @Test
    fun `formats reference with localized Surah name in Hindi`() {
        assertEquals("सूरह अल-बक़रह २:२३", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.HINDI))
        assertEquals("सूरह या-सीन ३६:६२", VerseReferenceFormatter.format("Surah 36:62", Language.HINDI))
    }

    @Test
    fun `formats reference with localized Surah name in Indonesian and Malay`() {
        assertEquals("Surah Al-Baqarah 2:23", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.INDONESIAN))
        assertEquals("Surah Al-Baqarah 2:23", VerseReferenceFormatter.format("Al-Baqarah 2:23", Language.MALAY))
    }

    @Test
    fun `every language follows the same Surah-word, name, reference pattern`() {
        assertEquals("سوره بقره ۲:۲۲", VerseReferenceFormatter.format("Al-Baqarah 2:22", Language.PERSIAN))
        for (language in Language.entries) {
            val formatted = VerseReferenceFormatter.format("Al-Baqarah 2:22", language)
            assertTrue("$language: $formatted", formatted.startsWith(VerseReferenceFormatter.surahWord(language) + " "))
        }
    }

    @Test
    fun `handles null or blank reference safely`() {
        assertEquals("", VerseReferenceFormatter.format(null, Language.ENGLISH))
        assertEquals("", VerseReferenceFormatter.format("", Language.BANGLA))
        assertEquals("", VerseReferenceFormatter.format("   ", Language.URDU))
    }

    @Test
    fun `formats digits correctly for general text`() {
        assertEquals("১২৩৪৫", VerseReferenceFormatter.formatDigits("12345", Language.BANGLA))
        assertEquals("۱۲۳۴۵", VerseReferenceFormatter.formatDigits("12345", Language.URDU))
        assertEquals("१२३४५", VerseReferenceFormatter.formatDigits("12345", Language.HINDI))
        assertEquals("12345", VerseReferenceFormatter.formatDigits("12345", Language.ENGLISH))
    }
}
