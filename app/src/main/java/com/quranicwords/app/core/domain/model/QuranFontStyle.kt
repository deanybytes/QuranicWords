package com.quranicwords.app.core.domain.model

/**
 * Complete identification & specification of Quranic Arabic script styles offered in the app.
 *
 * Each style maps directly to an authentic, verified open-licensed typeface bundled in the app.
 */
enum class QuranFontStyle(
    val displayName: LocalizedText,
    val scriptFamily: LocalizedText,
    val originRegion: LocalizedText,
    val keyFeatures: List<String>,
    val fontKey: String,
    val isBundled: Boolean = true
) {
    UTHMANIC_HAFS(
        displayName = mapOf(
            "en" to "Classic Uthmanic (Amiri / Madani)",
            "bn" to "ক্লাসিক উসমানী (আমিরী / মাদানী)"
        ),
        scriptFamily = mapOf(
            "en" to "Standard Uthmanic Naskh (Hafs)",
            "bn" to "প্রমিত উসমানী নাসখ (হাফস)"
        ),
        originRegion = mapOf(
            "en" to "Medina / King Fahd Complex (Uthman Taha style)",
            "bn" to "বাদশাহ ফাহাদ কুরআন কমপ্লেক্স, মদিনা"
        ),
        keyFeatures = listOf(
            "Slender, universally recognized Uthmanic proportions",
            "Semicircular hook / open-head Kha Jazm ( ۡ )",
            "Small diagonal dagger Alif ( ٰ )",
            "Classic master calligraphy"
        ),
        fontKey = "amiri"
    ),

    INDOPAK_NASKH(
        displayName = mapOf(
            "en" to "IndoPak Naskh (Lateef)",
            "bn" to "ইন্দো-পাক নাসখ (লতীফ)"
        ),
        scriptFamily = mapOf(
            "en" to "Indo-Pak Naskh (Subcontinental Classical)",
            "bn" to "ইন্দো-পাক নাসখ (উপমহাদেশীয় শাস্ত্রীয়)"
        ),
        originRegion = mapOf(
            "en" to "South Asia (Bangladesh, India, Pakistan)",
            "bn" to "দক্ষিণ এশিয়া (বাংলাদেশ, ভারত, পাকিস্তান)"
        ),
        keyFeatures = listOf(
            "Dense, thick calligraphic curves for clear letter separation",
            "Pronounced Tashdeed (shaddah) and large rounded diacritics",
            "Bold horizontal baselines & heavy pen strokes",
            "Traditional subcontinental Quran board layout"
        ),
        fontKey = "lateef"
    ),

    SCHEHERAZADE_NASKH(
        displayName = mapOf(
            "en" to "Scheherazade Naskh (Traditional)",
            "bn" to "শেহেরেযাদ নাসখ (ঐতিহ্যবাহী)"
        ),
        scriptFamily = mapOf(
            "en" to "Traditional Calligraphic Naskh",
            "bn" to "ঐতিহ্যবাহী ক্যালিগ্রাফিক নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Middle East / Classical Arabic Tradition",
            "bn" to "মধ্যপ্রাচ্য / শাস্ত্রীয় আরবি ঐতিহ্য"
        ),
        keyFeatures = listOf(
            "Graceful traditional ligatures & proportional diacritics",
            "Extended baseline connectivity",
            "Optimized glyph kerning for high-density mobile screens",
            "High legibility across all screen sizes"
        ),
        fontKey = "scheherazade"
    ),

    MUSHAF_UNICODE(
        displayName = mapOf(
            "en" to "Modern Digital Naskh (Noto Naskh)",
            "bn" to "আধুনিক ডিজিটাল নাসখ (নোটো নাসখ)"
        ),
        scriptFamily = mapOf(
            "en" to "Standard Digital OpenType Naskh",
            "bn" to "স্ট্যান্ডার্ড ডিজিটাল ওপেনটাইপ নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Universal Web / Mobile Standard (Unicode & OpenType)",
            "bn" to "সার্বজনীন ডিজিটাল ওপেনটাইপ স্ট্যান্ডার্ড"
        ),
        keyFeatures = listOf(
            "Clean modern digital vector legibility",
            "Harmonized proportions and uniform stroke weights",
            "Full text searchability and crisp geometric clarity",
            "Cross-platform native HarfBuzz rendering"
        ),
        fontKey = "noto_naskh"
    ),

    INDOPAK_NASTALEEQ(
        displayName = mapOf(
            "en" to "Nastaliq Script (Urdu / Persian)",
            "bn" to "নাসতা'লীক লিপি (উর্দু / ফার্সি)"
        ),
        scriptFamily = mapOf(
            "en" to "Indo-Pak / Persian Nastaliq",
            "bn" to "ইন্দো-পাক ও ফার্সি নাসতা'লীক"
        ),
        originRegion = mapOf(
            "en" to "Pakistan, India, Iran, Afghanistan",
            "bn" to "পাকিস্তান, ভারত, ইরান, আফগানিস্তান"
        ),
        keyFeatures = listOf(
            "Cascading diagonal baselines and flowing ligatures",
            "Traditional subcontinental Urdu Quran translation layout",
            "Broad pen-angle contrast and elegant swooping curves",
            "High cultural familiarity for Urdu, Persian, and Punjabi readers"
        ),
        fontKey = "noto_nastaliq_urdu"
    );

    companion object {
        val DEFAULT = UTHMANIC_HAFS

        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: when (name) {
            "CLASSIC_MADANI_1405", "MADANI_TAJWEED", "SHEMERLY_AMIRIYA", "UTHMANI_AMIRI" -> UTHMANIC_HAFS
            "NURANI_HAFEZI", "INDOPAK" -> INDOPAK_NASKH
            "MODERN_MADANI_1440", "MUSHAF_WARSH", "MUSHAF_QALOON", "SCHEHERAZADE" -> SCHEHERAZADE_NASKH
            "NOTO_NASKH" -> MUSHAF_UNICODE
            else -> DEFAULT
        }
    }
}
