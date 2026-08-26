package com.quranicwords.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class QuranFontStyleTest {

    @Test
    fun `entries have distinct fontKey mappings for all 5 genuine fonts`() {
        val keys = QuranFontStyle.entries.map { it.fontKey }.toSet()
        assertEquals(5, keys.size)
        assert(keys.contains("amiri"))
        assert(keys.contains("lateef"))
        assert(keys.contains("scheherazade"))
        assert(keys.contains("noto_naskh"))
        assert(keys.contains("noto_nastaliq_urdu"))
    }

    @Test
    fun `fromName maps legacy names backward-compatibly`() {
        assertEquals(QuranFontStyle.UTHMANIC_HAFS, QuranFontStyle.fromName("CLASSIC_MADANI_1405"))
        assertEquals(QuranFontStyle.UTHMANIC_HAFS, QuranFontStyle.fromName("MADANI_TAJWEED"))
        assertEquals(QuranFontStyle.UTHMANIC_HAFS, QuranFontStyle.fromName("SHEMERLY_AMIRIYA"))
        assertEquals(QuranFontStyle.INDOPAK_NASKH, QuranFontStyle.fromName("NURANI_HAFEZI"))
        assertEquals(QuranFontStyle.INDOPAK_NASKH, QuranFontStyle.fromName("INDOPAK"))
        assertEquals(QuranFontStyle.SCHEHERAZADE_NASKH, QuranFontStyle.fromName("MODERN_MADANI_1440"))
        assertEquals(QuranFontStyle.SCHEHERAZADE_NASKH, QuranFontStyle.fromName("MUSHAF_WARSH"))
        assertEquals(QuranFontStyle.SCHEHERAZADE_NASKH, QuranFontStyle.fromName("MUSHAF_QALOON"))
        assertEquals(QuranFontStyle.MUSHAF_UNICODE, QuranFontStyle.fromName("NOTO_NASKH"))
        assertEquals(QuranFontStyle.INDOPAK_NASTALEEQ, QuranFontStyle.fromName("INDOPAK_NASTALEEQ"))
        assertEquals(QuranFontStyle.DEFAULT, QuranFontStyle.fromName("UNKNOWN_STYLE"))
        assertEquals(QuranFontStyle.DEFAULT, QuranFontStyle.fromName(null))
    }
}
