package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.Language
import org.junit.Assert.assertEquals
import org.junit.Test

class VerseReferenceFormatterTest {

    @Test
    fun `formats reference in English without surah prefix`() {
        assertEquals("36:62", VerseReferenceFormatter.format("Surah 36:62", Language.ENGLISH))
        assertEquals("2:255", VerseReferenceFormatter.format("2:255", Language.ENGLISH))
    }

    @Test
    fun `formats reference in Bangla with Bengali digits`() {
        assertEquals("৩৬:৬২", VerseReferenceFormatter.format("Surah 36:62", Language.BANGLA))
        assertEquals("২:২৫৫", VerseReferenceFormatter.format("2:255", Language.BANGLA))
    }

    @Test
    fun `formats reference in Urdu with Arabic Indic digits`() {
        assertEquals("۳۶:۶۲", VerseReferenceFormatter.format("Surah 36:62", Language.URDU))
        assertEquals("۲:۲۵۵", VerseReferenceFormatter.format("2:255", Language.URDU))
    }

    @Test
    fun `handles null or blank reference safely`() {
        assertEquals("", VerseReferenceFormatter.format(null, Language.ENGLISH))
        assertEquals("", VerseReferenceFormatter.format("", Language.BANGLA))
    }
}
