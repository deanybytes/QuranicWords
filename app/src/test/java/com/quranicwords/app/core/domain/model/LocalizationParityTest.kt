package com.quranicwords.app.core.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

class LocalizationParityTest {

    private fun findRepoRoot(): File {
        var dir: File? = File(".").canonicalFile
        while (dir != null) {
            if (File(dir, "app/src/main/res/values/strings.xml").exists()) {
                return dir
            }
            dir = dir.parentFile
        }
        error("Could not find repository root")
    }

    private fun loadStringKeys(xmlFile: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(xmlFile)
        val strings = doc.getElementsByTagName("string")
        val map = mutableMapOf<String, String>()
        for (i in 0 until strings.length) {
            val item = strings.item(i) as Element
            val name = item.getAttribute("name")
            val text = item.textContent
            map[name] = text
        }
        return map
    }

    @Test
    fun `all 11 languages have exact 100 percent string key parity with zero missing or extra keys`() {
        val root = findRepoRoot()
        val baseXml = File(root, "app/src/main/res/values/strings.xml")
        assertTrue("Base strings.xml must exist", baseXml.exists())
        val baseKeys = loadStringKeys(baseXml)
        assertEquals("Base strings count", 321, baseKeys.size)

        for (lang in Language.entries) {
            val resDirName = if (lang == Language.ENGLISH) "values" else "values-${lang.tag}"
            val langXml = File(root, "app/src/main/res/$resDirName/strings.xml")
            assertTrue("strings.xml must exist for language: ${lang.name} (${lang.tag}) at ${langXml.path}", langXml.exists())

            val langKeys = loadStringKeys(langXml)
            val missing = baseKeys.keys - langKeys.keys
            val extra = langKeys.keys - baseKeys.keys

            assertTrue("Language ${lang.name} has missing keys: $missing", missing.isEmpty())
            assertTrue("Language ${lang.name} has extra keys: $extra", extra.isEmpty())
            assertEquals("Language ${lang.name} must have identical string count", baseKeys.size, langKeys.size)
        }
    }

    @Test
    fun `all 11 languages are declared in locales_config xml`() {
        val root = findRepoRoot()
        val configXml = File(root, "app/src/main/res/xml/locales_config.xml")
        assertTrue("locales_config.xml must exist", configXml.exists())

        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(configXml)
        val localeNodes = doc.getElementsByTagName("locale")

        val declaredLocales = mutableSetOf<String>()
        for (i in 0 until localeNodes.length) {
            val el = localeNodes.item(i) as Element
            val name = el.getAttributeNS("http://schemas.android.com/apk/res/android", "name")
                .ifBlank { el.getAttribute("android:name") }
            declaredLocales.add(name)
        }

        val expected = Language.entries.map { it.tag }.toSet()
        assertEquals("Declared locales must match all supported languages", expected, declaredLocales)
    }

    @Test
    fun `format specifiers match between base and all localized strings`() {
        val root = findRepoRoot()
        val baseXml = File(root, "app/src/main/res/values/strings.xml")
        val baseKeys = loadStringKeys(baseXml)

        val specifierPattern = Pattern.compile("%(?:\\d+\\$)?[sdf%]|%.2f%%")
        val positionalPattern = Pattern.compile("%(\\d+)\\$")

        for (lang in Language.entries) {
            if (lang == Language.ENGLISH) continue
            val langXml = File(root, "app/src/main/res/values-${lang.tag}/strings.xml")
            val langKeys = loadStringKeys(langXml)

            for ((key, baseVal) in baseKeys) {
                val transVal = langKeys[key] ?: continue
                val baseMatcher = specifierPattern.matcher(baseVal)
                var baseCount = 0
                while (baseMatcher.find()) baseCount++

                val transMatcher = specifierPattern.matcher(transVal)
                var transCount = 0
                while (transMatcher.find()) transCount++

                assertEquals(
                    "Format specifier count mismatch for key '$key' in ${lang.name}: '$baseVal' vs '$transVal'",
                    baseCount,
                    transCount
                )

                // Verify positional argument indices match
                val basePosMatcher = positionalPattern.matcher(baseVal)
                val basePositions = mutableListOf<String>()
                while (basePosMatcher.find()) {
                    basePosMatcher.group(1)?.let { basePositions.add(it) }
                }

                val transPosMatcher = positionalPattern.matcher(transVal)
                val transPositions = mutableListOf<String>()
                while (transPosMatcher.find()) {
                    transPosMatcher.group(1)?.let { transPositions.add(it) }
                }

                assertEquals(
                    "Format positional indices set mismatch for key '$key' in ${lang.name}: '$baseVal' vs '$transVal'",
                    basePositions.toSet(),
                    transPositions.toSet()
                )
            }
        }
    }
}
