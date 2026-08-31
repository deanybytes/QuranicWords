package com.quranicwords.app.core.domain.model

/**
 * Complete identification & specification of 10 authentic Quranic Arabic script styles offered in the app.
 *
 * Each style maps directly to a genuine, verified OpenType typeface bundled in `res/font`.
 */
enum class QuranFontStyle(
    val displayName: LocalizedText,
    val scriptFamily: LocalizedText,
    val originRegion: LocalizedText,
    val keyFeatures: List<String>,
    val fontKey: String,
    val isBundled: Boolean = true
) {
    MADANI_KFGQPC(
        displayName = mapOf(
            "en" to "Madinah Mushaf (KFGQPC Hafs)",
            "bn" to "মদিনা মুসহাফ (কিং ফাহাদ কমপ্লেক্স)"
        ),
        scriptFamily = mapOf(
            "en" to "Official Medina Mushaf (Hafs)",
            "bn" to "অফিসিয়াল মদিনা মুসহাফ (হাফস)"
        ),
        originRegion = mapOf(
            "en" to "King Fahd Glorious Quran Complex, Medina",
            "bn" to "কিং ফাহাদ কুরআন প্রিন্টিং কমপ্লেক্স, মদিনা"
        ),
        keyFeatures = listOf(
            "Official King Fahd Complex Madinah typography",
            "Open-head Kha Jazm ( ۡ ) and subtle Maddah marks",
            "Classic, universally recognized Uthman Taha hand",
            "Gold standard across international Quran prints"
        ),
        fontKey = "kfgqpc_hafs"
    ),

    INDOPAK_NASKH(
        displayName = mapOf(
            "en" to "Indo-Pak / Noorani Naskh (Lateef)",
            "bn" to "ইন্দো-পাক / নূরানী নাসখ (লতীফ)"
        ),
        scriptFamily = mapOf(
            "en" to "Subcontinental Noorani Naskh",
            "bn" to "উপমহাদেশীয় নূরানী ও হাফেজী নাসখ"
        ),
        originRegion = mapOf(
            "en" to "South Asia (Bangladesh, India, Pakistan)",
            "bn" to "দক্ষিণ এশিয়া (বাংলাদেশ, ভারত, পাকিস্তান)"
        ),
        keyFeatures = listOf(
            "Bold, rounded letters for effortless reading",
            "Prominent Tashdeed and clear vocalization marks",
            "Traditional layout used in South Asian Quran prints",
            "Specially optimized for easy recitation"
        ),
        fontKey = "lateef"
    ),

    INDOPAK_NASTALEEQ(
        displayName = mapOf(
            "en" to "Quranic Nastaliq (KFGQPC)",
            "bn" to "কুরআনিক নাসতালীক (কিং ফাহাদ কমপ্লেক্স)"
        ),
        scriptFamily = mapOf(
            "en" to "Authentic Quranic Nastaliq (Hafs)",
            "bn" to "শাস্ত্রীয় কুরআনিক নাসতালীক (হাফস)"
        ),
        originRegion = mapOf(
            "en" to "King Fahd Complex / South Asian Tradition",
            "bn" to "উপমহাদেশ ও কিং ফাহাদ কমপ্লেক্স"
        ),
        keyFeatures = listOf(
            "Authentic King Fahd Complex Quranic Nastaliq",
            "Graceful cascading baselines with complete Quranic diacritics",
            "Traditional Urdu/Persian Quran calligraphy style",
            "Full Tashkeel and Quranic pause symbols"
        ),
        fontKey = "hafs_nastaleeq"
    ),

    AMIRI_CLASSIC(
        displayName = mapOf(
            "en" to "Classic Amiri Quran",
            "bn" to "ক্লাসিক আমিরী কুরআন"
        ),
        scriptFamily = mapOf(
            "en" to "Royal Amiri / Bulaq Naskh",
            "bn" to "ঐতিহ্যবাহী রাজকীয় আমিরী নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Egypt & Greater Middle East",
            "bn" to "মিশর ও আরব বিশ্ব"
        ),
        keyFeatures = listOf(
            "Prestigious classical Cairo 1924 Amiri typography",
            "Exquisite calligraphic balance and proportions",
            "Historic standard for Islamic scholarship",
            "Crisp rendering with traditional Quranic ligatures"
        ),
        fontKey = "amiri"
    ),

    SCHEHERAZADE_NASKH(
        displayName = mapOf(
            "en" to "Scheherazade Traditional Naskh",
            "bn" to "শেহেরেযাদ ঐতিহ্যবাহী নাসখ"
        ),
        scriptFamily = mapOf(
            "en" to "Traditional Calligraphic Naskh",
            "bn" to "ঐতিহ্যবাহী ক্যালিগ্রাফিক নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Middle East & International",
            "bn" to "মধ্যপ্রাচ্য ও আন্তর্জাতিক"
        ),
        keyFeatures = listOf(
            "Graceful ligatures and extended baseline flow",
            "Carefully proportioned diacritics and signs",
            "SIL OpenType optimization for high-DPI screens",
            "Exceptional clarity across all font sizes"
        ),
        fontKey = "scheherazade"
    ),

    KITAB_QURAN(
        displayName = mapOf(
            "en" to "Kitab Clear Quran",
            "bn" to "কিতাব স্বচ্ছ কুরআন"
        ),
        scriptFamily = mapOf(
            "en" to "High-Legibility Modern Quranic Naskh",
            "bn" to "উচ্চ স্পষ্টতার আধুনিক কুরআনিক নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Modern Digital Quran Standard",
            "bn" to "আধুনিক ডিজিটাল স্বচ্ছ লিপি"
        ),
        keyFeatures = listOf(
            "High contrast and wide letter spacing",
            "Distinct diacritic marks that never collide",
            "Optimized specifically for mobile app reading",
            "Modern, crisp vector strokes"
        ),
        fontKey = "kitab"
    ),

    KFGQPC_WARSH(
        displayName = mapOf(
            "en" to "Warsh Mushaf (KFGQPC)",
            "bn" to "ওয়ারশ মুসহাফ (কিং ফাহাদ কমপ্লেক্স)"
        ),
        scriptFamily = mapOf(
            "en" to "Authentic Uthmanic Warsh an-Nafi'",
            "bn" to "উসমানী ওয়ারশ আন-নাফে'"
        ),
        originRegion = mapOf(
            "en" to "North Africa (Morocco, Algeria, Tunisia)",
            "bn" to "উত্তর আফ্রিকা ও মাগরিব"
        ),
        keyFeatures = listOf(
            "Official King Fahd Complex Warsh calligraphy",
            "Distinct Maghrebi dotting and vowel placements",
            "Traditional North African Mushaf aesthetic",
            "Full support for Riwayah Warsh vocalization"
        ),
        fontKey = "kfgqpc_warsh"
    ),

    KFGQPC_QALOUN(
        displayName = mapOf(
            "en" to "Qaloun Mushaf (KFGQPC)",
            "bn" to "কালূন মুসহাফ (কিং ফাহাদ কমপ্লেক্স)"
        ),
        scriptFamily = mapOf(
            "en" to "Authentic Uthmanic Qaloun an-Nafi'",
            "bn" to "উসমানী কালূন আন-নাফে'"
        ),
        originRegion = mapOf(
            "en" to "Libya, Tunisia & West Africa",
            "bn" to "লিবিয়া, তিউনিসিয়া ও পশ্চিম আফ্রিকা"
        ),
        keyFeatures = listOf(
            "Official King Fahd Complex Qaloun calligraphy",
            "Distinct Qaloun recitation diacritical conventions",
            "High authenticity for Mediterranean/African readers",
            "Refined Quranic typographic rules"
        ),
        fontKey = "kfgqpc_qaloun"
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
            "en" to "Universal Digital / Web & Mobile",
            "bn" to "সার্বজনীন ডিজিটাল স্ট্যান্ডার্ড"
        ),
        keyFeatures = listOf(
            "Google Noto harmonized geometric proportions",
            "Clean, uniform stroke weights on all screens",
            "Full cross-platform HarfBuzz rendering",
            "Ultra-sharp readability on compact devices"
        ),
        fontKey = "noto_naskh"
    ),

    NOTO_NASTALEEQ(
        displayName = mapOf(
            "en" to "Noto Nastaliq (Urdu / Persian)",
            "bn" to "নোটো নাসতালীক (উর্দু / ফার্সি)"
        ),
        scriptFamily = mapOf(
            "en" to "Flowing Persian & Urdu Nastaliq",
            "bn" to "প্রবাহিত ফার্সি ও উর্দু নাসতালীক"
        ),
        originRegion = mapOf(
            "en" to "Pakistan, India, Iran, Afghanistan",
            "bn" to "পাকিস্তান, ভারত ও ইরান"
        ),
        keyFeatures = listOf(
            "Elegant diagonal flowing calligraphic script",
            "Beloved by Urdu, Persian, and Punjabi readers",
            "Broad stroke modulation and poetic curves",
            "Traditional subcontinental translation typography"
        ),
        fontKey = "noto_nastaliq_urdu"
    );

    companion object {
        val DEFAULT = MADANI_KFGQPC

        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: when (name) {
            "UTHMANIC_HAFS", "CLASSIC_MADANI_1405", "MADANI_TAJWEED", "SHEMERLY_AMIRIYA", "UTHMANI_AMIRI" -> MADANI_KFGQPC
            "NURANI_HAFEZI", "INDOPAK" -> INDOPAK_NASKH
            "MODERN_MADANI_1440", "SCHEHERAZADE" -> SCHEHERAZADE_NASKH
            "MUSHAF_WARSH" -> KFGQPC_WARSH
            "MUSHAF_QALOON" -> KFGQPC_QALOUN
            "NOTO_NASKH" -> MUSHAF_UNICODE
            "AMIRI" -> AMIRI_CLASSIC
            "KITAB" -> KITAB_QURAN
            else -> DEFAULT
        }
    }
}
