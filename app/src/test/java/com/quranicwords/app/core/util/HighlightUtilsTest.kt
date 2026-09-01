package com.quranicwords.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class HighlightUtilsTest {

    @Test
    fun `findMeaningHighlightRange returns exact span when meaningHighlight matches`() {
        val translation = "In the name of Allah, the Entirely Merciful, the Especially Merciful."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "name",
            meaning = "name"
        )
        assertNotNull(range)
        assertEquals("name", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches case-insensitively with meaningHighlight`() {
        val translation = "In the name of Allah, the Entirely Merciful."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "in",
            meaning = "in"
        )
        assertNotNull(range)
        assertEquals(0, range!!.first)
        assertEquals(2, range.second)
        assertEquals("In", translation.substring(range.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange falls back to meaning when meaningHighlight is null`() {
        val translation = "We do not abrogate a verse or cause it to be forgotten..."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = null,
            meaning = "sign, verse"
        )
        assertNotNull(range)
        assertEquals("verse", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange strips parentheticals from meaning`() {
        val translation = "Their example is that of one who kindled a fire in darkness..."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = null,
            meaning = "fire (noun, feminine)"
        )
        assertNotNull(range)
        assertEquals("fire", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches diacritic-normalized words like Allah and Allah with macron`() {
        val translation = "In the name of Allāh, the Entirely Merciful."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = null,
            meaning = "Allah, the one true God"
        )
        assertNotNull(range)
        assertEquals("Allāh", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange expands to full word when suffix or prefix is attached in Bengali`() {
        val translation = "নিশ্চয় কাফেরদের জন্য রয়েছে কঠিন শাস্তি।"
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "কাফের",
            meaning = "কাফের"
        )
        assertNotNull(range)
        assertEquals("কাফেরদের", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange expands to full word with suffix in English`() {
        val translation = "Indeed, the believers are successful."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "believer",
            meaning = "believer"
        )
        assertNotNull(range)
        assertEquals("believers", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange does not expand past punctuation`() {
        val translation = "বলো: তিনি আল্লাহ, এক।"
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "আল্লাহ",
            meaning = "আল্লাহ"
        )
        assertNotNull(range)
        assertEquals("আল্লাহ", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange returns null for blank or unmatched input`() {
        assertNull(HighlightUtils.findMeaningHighlightRange(null, null, null))
        assertNull(HighlightUtils.findMeaningHighlightRange("", "", ""))
        assertNull(HighlightUtils.findMeaningHighlightRange("Hello world", "xyz", "completely unrelated"))
    }
}
