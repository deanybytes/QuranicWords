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
    fun `findMeaningHighlightRange matches Bengali sub-words preserving matras`() {
        val translation = "তিনি পরম দয়ালু।"
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = null,
            meaning = "পরম দয়ালু"
        )
        assertNotNull(range)
        assertEquals("পরম দয়ালু", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Urdu before full stop without including punctuation`() {
        val translation = "اور وہ بڑا مہربان ہے۔"
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "مہربان",
            meaning = "مہربان"
        )
        assertNotNull(range)
        assertEquals("مہربان", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Urdu with Arabic-Persian character normalization and comma`() {
        val translation = "یہ کتاب ہے، جس میں کوئی شک نہیں۔"
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = null,
            meaning = "كتاب" // Arabic kaf
        )
        assertNotNull(range)
        assertEquals("کتاب", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Hindi with matras preserved`() {
        val translation = "यह किताब है जिसमें कोई संदेह नहीं है।"
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "किताब",
            meaning = "किताब"
        )
        assertNotNull(range)
        assertEquals("किताब", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Persian kaf and yeh`() {
        val translation = "این کتابی است که در آن هیچ شکی نیست."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "کتاب",
            meaning = "کتاب"
        )
        assertNotNull(range)
        assertEquals("کتابی", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Turkish with word boundary expansion`() {
        val translation = "Bu, kendisinde şüphe olmayan kitaptır."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "kitap",
            meaning = "kitap"
        )
        assertNotNull(range)
        assertEquals("kitaptır", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Indonesian and Malay`() {
        val translation = "Kitab ini tidak ada keraguan padanya."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "kitab",
            meaning = "kitab"
        )
        assertNotNull(range)
        assertEquals("Kitab", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches French with accents`() {
        val translation = "C'est le Livre céleste au sujet duquel il n'y a aucun doute."
        val range = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = translation,
            meaningHighlight = "livre",
            meaning = "livre"
        )
        assertNotNull(range)
        assertEquals("Livre", translation.substring(range!!.first, range.second))
    }

    @Test
    fun `findMeaningHighlightRange matches Swahili and Hausa`() {
        val swahili = "Hiki ni Kitabu kisicho na shaka."
        val swRange = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = swahili,
            meaningHighlight = "kitabu",
            meaning = "kitabu"
        )
        assertNotNull(swRange)
        assertEquals("Kitabu", swahili.substring(swRange!!.first, swRange.second))

        val hausa = "Wannan Littafi ne babu shakka."
        val haRange = HighlightUtils.findMeaningHighlightRange(
            verseTranslation = hausa,
            meaningHighlight = "littafi",
            meaning = "littafi"
        )
        assertNotNull(haRange)
        assertEquals("Littafi", hausa.substring(haRange!!.first, haRange.second))
    }

    @Test
    fun `findMeaningHighlightRange returns null for blank or unmatched input`() {
        assertNull(HighlightUtils.findMeaningHighlightRange(null, null, null))
        assertNull(HighlightUtils.findMeaningHighlightRange("", "", ""))
        assertNull(HighlightUtils.findMeaningHighlightRange("Hello world", "xyz", "completely unrelated"))
    }
}
