package com.quranicwords.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class QuranFontStyleTest {

    @Test
    fun `entries are seven distinct genuine fonts named in all eight languages`() {
        val keys = QuranFontStyle.entries.map { it.fontKey }
        assertEquals(listOf("kfgqpc_hafs", "amiri", "scheherazade", "noto_naskh", "lateef", "noorehuda", "noorehira"), keys)
        for (style in QuranFontStyle.entries) {
            assertEquals(style.name, setOf("en", "bn", "ur", "hi", "in", "tr", "fa", "fr"), style.displayName.keys)
        }
    }

    @Test
    fun `fromName maps legacy and removed styles backward-compatibly`() {
        assertEquals(QuranFontStyle.MADANI_KFGQPC, QuranFontStyle.fromName("UTHMANIC_HAFS"))
        assertEquals(QuranFontStyle.MADANI_KFGQPC, QuranFontStyle.fromName("CLASSIC_MADANI_1405"))
        assertEquals(QuranFontStyle.MADANI_KFGQPC, QuranFontStyle.fromName("KFGQPC_WARSH"))
        assertEquals(QuranFontStyle.MADANI_KFGQPC, QuranFontStyle.fromName("KFGQPC_QALOUN"))
        assertEquals(QuranFontStyle.NOORANI, QuranFontStyle.fromName("INDOPAK_NASKH"))
        assertEquals(QuranFontStyle.NOORANI, QuranFontStyle.fromName("NURANI_HAFEZI"))
        assertEquals(QuranFontStyle.HAFEZI, QuranFontStyle.fromName("INDOPAK_NASTALEEQ"))
        assertEquals(QuranFontStyle.SCHEHERAZADE_NASKH, QuranFontStyle.fromName("KITAB_QURAN"))
        assertEquals(QuranFontStyle.NOTO_NASKH, QuranFontStyle.fromName("MUSHAF_UNICODE"))
        assertEquals(QuranFontStyle.AMIRI_CLASSIC, QuranFontStyle.fromName("AMIRI"))
        assertEquals(QuranFontStyle.MADANI_KFGQPC, QuranFontStyle.fromName("nonsense"))
    }

    @Test
    fun `Indo-Pak fonts get the Indo-Pak marks, one character for one`() {
        // لَكَنُودٞ (open tanween), ٱلۡإِنسَٰنَ (waṣla + Uthmani sukun), مُّبَيِّنَةٖ, خَمۡرٗا
        val uthmani = "\u0671\u0644\u06E1\u0625\u0646\u0633\u064E\u0670\u0646\u064E \u062F\u065E \u0629\u0656 \u0631\u0657"
        val indoPak = QuranFontStyle.NOORANI.script(uthmani)
        assertEquals(uthmani.length, indoPak.length)
        assertEquals("\u0627\u0644\u0652\u0625\u0646\u0633\u064E\u0670\u0646\u064E \u062F\u064C \u0629\u064D \u0631\u064B", indoPak)
        assertEquals(indoPak, QuranFontStyle.HAFEZI.script(uthmani))
        for (style in QuranFontStyle.entries.filter { !it.indoPak }) assertEquals(uthmani, style.script(uthmani))
    }
}
