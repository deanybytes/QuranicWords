package com.quranicwords.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageTest {

    @Test
    fun `the app offers exactly the eight supported languages`() {
        assertEquals(listOf("en", "bn", "ur", "hi", "in", "tr", "fa", "fr"), Language.entries.map { it.tag })
    }

    @Test
    fun `withdrawn Malay, Hausa and Swahili tags map to English`() {
        for (tag in listOf("ms", "msa", "may", "ha", "hau", "sw", "swa", "MS", "Sw")) {
            assertEquals("tag $tag", Language.ENGLISH, Language.fromTag(tag))
            assertTrue("tag $tag", Language.isRetiredTag(tag))
        }
    }

    @Test
    fun `current tags and aliases still resolve, unknown tags do not`() {
        Language.entries.forEach { assertEquals(it, Language.fromTag(it.tag)) }
        assertEquals(Language.INDONESIAN, Language.fromTag("id"))
        assertEquals(Language.PERSIAN, Language.fromTag("per"))
        assertFalse(Language.isRetiredTag("en"))
        assertFalse(Language.isRetiredTag(null))
        assertNull(Language.fromTag("xx"))
        assertNull(Language.fromTag(null))
    }
}
