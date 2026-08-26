package com.quranicwords.app.core.domain.model

/**
 * Complete identification & specification of Quranic Arabic script styles offered in the app.
 *
 * Each style includes script family categorization, historical/regional origin, key orthographical
 * identifiers, and mapping to verified open-licensed (SIL OFL) typefaces.
 */
enum class QuranFontStyle(
    val displayName: LocalizedText,
    val scriptFamily: LocalizedText,
    val originRegion: LocalizedText,
    val keyFeatures: List<String>,
    val fontKey: String,
    val isBundled: Boolean = true
) {
    NURANI_HAFEZI(
        displayName = mapOf(
            "en" to "Nurani / Hafezi (15-Line Script)",
            "bn" to "নুরানি / হাফেজী (১৫ লাইন ইন্দো-পাক)"
        ),
        scriptFamily = mapOf(
            "en" to "Indo-Pak Naskh (Subcontinental Classical)",
            "bn" to "ইন্দো-পাক নাসখ (শাস্ত্রীয় সংস্করণ)"
        ),
        originRegion = mapOf(
            "en" to "Emdadia Library / Nurani Quran Board (Bangladesh, India, Pakistan)",
            "bn" to "এমদাদিয়া লাইব্রেরী / নুরানী কুরআন বোর্ড"
        ),
        keyFeatures = listOf(
            "Fixed 15-line ayah-ending page format",
            "Distinct small inverted-V / caret Jazm (^)",
            "Bold horizontal baselines & heavy pen strokes",
            "Long, fully vertical dagger Alif marks"
        ),
        fontKey = "lateef"
    ),

    INDOPAK_NASKH(
        displayName = mapOf(
            "en" to "IndoPak Naskh (General Subcontinental)",
            "bn" to "ইন্দো-পাক নাসখ (সাধারণ উপমহাদেশীয়)"
        ),
        scriptFamily = mapOf(
            "en" to "Indo-Pak Naskh",
            "bn" to "ইন্দো-পাক নাসখ"
        ),
        originRegion = mapOf(
            "en" to "South Asia & Southeast Asia (Bangladesh, India, Pakistan, Indonesia)",
            "bn" to "দক্ষিণ এশিয়া ও দক্ষিণ-পূর্ব এশিয়া"
        ),
        keyFeatures = listOf(
            "Dense, thick calligraphic curves for clear letter separation",
            "Pronounced Tashdeed (shaddah) and large rounded diacritics",
            "Prominent traditional Waqf stop marks (ۚ , ؕ , ۘ , ۙ , ؞)",
            "Classic decorative margin layout"
        ),
        fontKey = "lateef"
    ),

    CLASSIC_MADANI_1405(
        displayName = mapOf(
            "en" to "Classic Madani Mushaf (Hijri 1405)",
            "bn" to "ক্লাসিক মাদানী মুসহাফ (১৪০৫ হিজরি)"
        ),
        scriptFamily = mapOf(
            "en" to "Standard Uthmanic Naskh (Hafs 'an 'Asim)",
            "bn" to "প্রমিত উসমানী নাসখ (হাফস)"
        ),
        originRegion = mapOf(
            "en" to "King Fahd Quran Printing Complex, Medina (1405 AH / Uthman Taha)",
            "bn" to "বাদশাহ ফাহাদ কুরআন কমপ্লেক্স, মদিনা (১৪০৫ হি.)"
        ),
        keyFeatures = listOf(
            "Original master calligraphy by Uthman Taha",
            "Semicircular hook / open-head Kha Jazm ( ۡ )",
            "Slender, universally recognized Uthmanic proportions",
            "Small diagonal dagger Alif ( ٰ )"
        ),
        fontKey = "amiri"
    ),

    MODERN_MADANI_1440(
        displayName = mapOf(
            "en" to "Modern Madani Mushaf (Hijri 1440)",
            "bn" to "আধুনিক মাদানী মুসহাফ (১৪৪০ হিজরি)"
        ),
        scriptFamily = mapOf(
            "en" to "Modern Vector Uthmanic Naskh",
            "bn" to "আধুনিক ভেক্টর উসমানী নাসখ"
        ),
        originRegion = mapOf(
            "en" to "King Fahd Quran Printing Complex, Medina (1440 AH Digital Redraw)",
            "bn" to "বাদশাহ ফাহাদ কমপ্লেক্স (১৪৪০ হি. আধুনিক সংস্করণ)"
        ),
        keyFeatures = listOf(
            "High-definition digital vector redraw of Uthman Taha script",
            "Sharpened terminal edges & refined ligature curves",
            "Optimized glyph kerning for high-density mobile screens",
            "Modernized diacritical and ayah symbol spacing"
        ),
        fontKey = "scheherazade"
    ),

    MADANI_TAJWEED(
        displayName = mapOf(
            "en" to "Madani Mushaf with Tajweed (Color-Coded)",
            "bn" to "তাজবীদ কালার কোডেড মাদানী মুসহাফ"
        ),
        scriptFamily = mapOf(
            "en" to "Color-Coded Madani Naskh",
            "bn" to "কালার কোডেড মাদানী নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Dar Al-Maarifah (Damascus) / King Fahd Quran Complex",
            "bn" to "দারুল মা'আরিফাহ (দামেস্ক) / কিং ফাহাদ কমপ্লেক্স"
        ),
        keyFeatures = listOf(
            "Red / Orange: Madd (vocal prolongation - 4 to 6 counts)",
            "Green / Cyan: Ghunnah, Ikhfa, and Iqlab (nasalization)",
            "Blue / Navy: Qalqalah (echoing/bouncing consonants)",
            "Grey: Silent / merged letters (Idgham)"
        ),
        fontKey = "amiri"
    ),

    MUSHAF_WARSH(
        displayName = mapOf(
            "en" to "Mushaf Warsh (Riwayah Warsh 'an Nafi')",
            "bn" to "মুসহাফ ওয়ারশ (ওয়ারশ কিরাত)"
        ),
        scriptFamily = mapOf(
            "en" to "Maghrebi / Andalusian-influenced Naskh",
            "bn" to "মাগরেবী / আন্দালুসীয় নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Morocco, Algeria, Tunisia, Mauritania, West Africa",
            "bn" to "মরক্কো, আলজেরিয়া, তিউনিসিয়া, মৌরিতানিয়া, পশ্চিম আফ্রিকা"
        ),
        keyFeatures = listOf(
            "Authentic Maghrebi Nuqta: Fa with dot below (ڢ), Qaf with dot above (ڧ)",
            "Solid circular dots for Tashil, Imalah, and Naql vowels",
            "Warsh-specific ayah division and orthography",
            "Distinct North African manuscript styling"
        ),
        fontKey = "scheherazade"
    ),

    MUSHAF_QALOON(
        displayName = mapOf(
            "en" to "Mushaf Qaloon (Riwayah Qaloon 'an Nafi')",
            "bn" to "মুসহাফ কালূন (কালূন কিরাত)"
        ),
        scriptFamily = mapOf(
            "en" to "North African Standard Naskh (Riwayah Qaloon)",
            "bn" to "উত্তর আফ্রিকান নাসখ (কালূন)"
        ),
        originRegion = mapOf(
            "en" to "Libya, Tunisia, Chad",
            "bn" to "লিবিয়া, তিউনিসিয়া, চাদ"
        ),
        keyFeatures = listOf(
            "Customized diacritics for Qaloon recitation rules",
            "Specialized notation for Silat Mim al-Jam'",
            "Distinct Munfasil shortening and Hamzatayn marks",
            "Clean Mediterranean Arabic letterforms"
        ),
        fontKey = "scheherazade"
    ),

    SHEMERLY_AMIRIYA(
        displayName = mapOf(
            "en" to "Shemerly Script (Al-Amiriya / Egyptian)",
            "bn" to "শেমেরলী / আল-আমিরিয়া (মিশরীয় মুদ্রণ)"
        ),
        scriptFamily = mapOf(
            "en" to "Egyptian Ottoman-style Naskh",
            "bn" to "মিশরীয় উসমানীয় ধাঁচের নাসখ"
        ),
        originRegion = mapOf(
            "en" to "Dar Al-Shemerly (Cairo) & Bulaq / Amiriya Government Press",
            "bn" to "দার আশ-শেমেরলী (কায়রো) ও আমিরিয়া প্রেস"
        ),
        keyFeatures = listOf(
            "Heavier, blocky letter weight compared to Medina script",
            "Lower x-height with tightly packed words",
            "Thick descending tails on Noon (ن), Ya (ي), and Ra (ر)",
            "Historic 1924 Cairo Royal Quran typographic tradition"
        ),
        fontKey = "amiri"
    ),

    MUSHAF_UNICODE(
        displayName = mapOf(
            "en" to "Mushaf Unicode Text (System / Dynamic Reflow)",
            "bn" to "মুসহাফ ইউনিকোড টেক্সট (সিস্টেম ভেক্টর)"
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
            "Dynamic vector glyph rendering across all screen sizes",
            "Full text searchability, copying, and reflowing",
            "Clean modern digital legibility",
            "Cross-platform HarfBuzz / DirectWrite native rendering"
        ),
        fontKey = "noto_naskh"
    ),

    INDOPAK_NASTALEEQ(
        displayName = mapOf(
            "en" to "Nastaliq Script (Urdu/Persian Style)",
            "bn" to "নাসতা'লীক লিপি (উর্দু/ফার্সি শৈলী)"
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
        val DEFAULT = CLASSIC_MADANI_1405

        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: when (name) {
            "UTHMANI_AMIRI" -> SHEMERLY_AMIRIYA
            "SCHEHERAZADE" -> MODERN_MADANI_1440
            "NOTO_NASKH" -> MUSHAF_UNICODE
            "INDOPAK" -> INDOPAK_NASKH
            else -> DEFAULT
        }
    }
}
