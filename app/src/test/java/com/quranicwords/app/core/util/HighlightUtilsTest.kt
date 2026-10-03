package com.quranicwords.app.core.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class HighlightUtilsTest {

    @Test
    fun `Uthmani and dictionary spellings of the same word read the same`() {
        assertEquals(HighlightUtils.arabicReadingKey("مَنْ"), HighlightUtils.arabicReadingKey("مَن"))
        assertEquals(HighlightUtils.arabicReadingKey("مِنْ"), HighlightUtils.arabicReadingKey("مِنۡ"))
        assertEquals(HighlightUtils.arabicReadingKey("ٱلَّذِينَ"), HighlightUtils.arabicReadingKey("الَّذِينَ"))
    }

    @Test
    fun `words that differ only in their vowels stay different`() {
        assertNotEquals(HighlightUtils.arabicReadingKey("مَنْ"), HighlightUtils.arabicReadingKey("مِنْ"))
        assertEquals("", HighlightUtils.arabicReadingKey(null))
        assertEquals("", HighlightUtils.arabicReadingKey("  "))
    }
}
