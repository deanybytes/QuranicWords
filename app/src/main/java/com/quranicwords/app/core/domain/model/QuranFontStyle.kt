package com.quranicwords.app.core.domain.model

/**
 * The Qur'anic Arabic typefaces offered in the app. Every entry is a genuine, distinct typeface
 * bundled in `res/font` that:
 * - renders every character of the app's Uthmani (Madinah) text, including the pause marks
 *   (ۖ ۗ ۚ), dagger alif, alif waṣla and open tanween (ٞ) - a missing glyph would silently fall
 *   back to another font;
 * - looks visibly different from the others (near-identical designs are not offered twice);
 * - may be redistributed in the app (KFGQPC end-user licence, SIL OFL 1.1, or the NooreHidayat
 *   licence, which allows free distribution with any application);
 * - is named for what it is - no font is presented as a script style it does not have.
 *
 * Removed after a rendering audit: KFGQPC Warsh and Qaloun (made for the Warsh/Qālūn readings, no
 * alif waṣla), Kitab (derived from Scheherazade - looked the same), KFGQPC Nastaleeq (renders as
 * plain naskh) and Noto Nastaliq (no Qur'anic pause marks). Saved choices map to the closest
 * remaining style in [fromName].
 */
enum class QuranFontStyle(
    val displayName: LocalizedText,
    val fontKey: String,
    /** Indo-Pak calligraphy (the Noorani and Hafezi prints of South Asia): the font follows the
     * Indo-Pak conventions for a handful of marks, so [script] converts them. */
    val indoPak: Boolean = false
) {
    MADANI_KFGQPC(
        displayName = mapOf(
            "en" to "Madinah Mushaf (KFGQPC Uthmanic Hafs)",
            "bn" to "মদিনা মুসহাফ (কেএফজিকিউপিসি উসমানী হাফস)",
            "ur" to "مدینہ مصحف (شاہ فہد کمپلیکس، عثمانی حفص)",
            "hi" to "मदीना मुसहफ़ (केएफ़जीक्यूपीसी उस्मानी हफ़्स)",
            "in" to "Mushaf Madinah (KFGQPC Utsmani Hafs)",
            "tr" to "Medine Mushafı (KFGQPC Osmanlı Hafs)",
            "fa" to "مصحف مدینه (مجتمع ملک فهد، عثمانی حفص)",
            "fr" to "Moushaf de Médine (KFGQPC Uthmani Hafs)"
        ),
        fontKey = "kfgqpc_hafs"
    ),

    AMIRI_CLASSIC(
        displayName = mapOf(
            "en" to "Amiri - classic calligraphic Naskh",
            "bn" to "আমিরী - ধ্রুপদী ক্যালিগ্রাফিক নাসখ",
            "ur" to "امیری - کلاسیکی خطاطی نسخ",
            "hi" to "अमीरी - शास्त्रीय सुलेख नस्ख़",
            "in" to "Amiri - Naskh kaligrafi klasik",
            "tr" to "Amiri - klasik hat Nesih",
            "fa" to "امیری - نسخ خوشنویسی کلاسیک",
            "fr" to "Amiri - Naskh calligraphique classique"
        ),
        fontKey = "amiri"
    ),

    SCHEHERAZADE_NASKH(
        displayName = mapOf(
            "en" to "Scheherazade New - traditional Naskh",
            "bn" to "শেহেরজাদ নিউ - ঐতিহ্যবাহী নাসখ",
            "ur" to "شہرزاد نیو - روایتی نسخ",
            "hi" to "शहरज़ाद न्यू - पारंपरिक नस्ख़",
            "in" to "Scheherazade New - Naskh tradisional",
            "tr" to "Scheherazade New - geleneksel Nesih",
            "fa" to "شهرزاد نو - نسخ سنتی",
            "fr" to "Scheherazade New - Naskh traditionnel"
        ),
        fontKey = "scheherazade"
    ),

    NOTO_NASKH(
        displayName = mapOf(
            "en" to "Noto Naskh Arabic - modern clean Naskh",
            "bn" to "নোটো নাসখ আরবি - আধুনিক সরল নাসখ",
            "ur" to "نوٹو نسخ عربی - جدید سادہ نسخ",
            "hi" to "नोटो नस्ख़ अरबी - आधुनिक सरल नस्ख़",
            "in" to "Noto Naskh Arabic - Naskh modern yang bersih",
            "tr" to "Noto Naskh Arabic - modern sade Nesih",
            "fa" to "نوتو نسخ عربی - نسخ ساده و امروزی",
            "fr" to "Noto Naskh Arabic - Naskh moderne épuré"
        ),
        fontKey = "noto_naskh"
    ),

    LATEEF_NASKH(
        displayName = mapOf(
            "en" to "Lateef - South Asian Naskh",
            "bn" to "লতীফ - দক্ষিণ এশীয় নাসখ",
            "ur" to "لطیف - جنوبی ایشیائی نسخ",
            "hi" to "लतीफ़ - दक्षिण एशियाई नस्ख़",
            "in" to "Lateef - Naskh Asia Selatan",
            "tr" to "Lateef - Güney Asya Nesihi",
            "fa" to "لطیف - نسخ جنوب آسیا",
            "fr" to "Lateef - Naskh d'Asie du Sud"
        ),
        fontKey = "lateef"
    ),

    NOORANI(
        displayName = mapOf(
            "en" to "Noorani Qur'an (Indo-Pak, Noore Huda)",
            "bn" to "নূরানী কুরআন (ইন্দো-পাক, নূরে হুদা)",
            "ur" to "نورانی قرآن (ہند و پاک، نورِ ہدیٰ)",
            "hi" to "नूरानी क़ुरआन (इंडो-पाक, नूरे हुदा)",
            "in" to "Al-Qur'an Nurani (Indo-Pak, Noore Huda)",
            "tr" to "Nurani Kur'an (Hint-Pak, Noore Huda)",
            "fa" to "قرآن نورانی (هند و پاک، نور هدی)",
            "fr" to "Coran Nourani (Indo-Pak, Noore Huda)"
        ),
        fontKey = "noorehuda",
        indoPak = true
    ),

    HAFEZI(
        displayName = mapOf(
            "en" to "Hafezi Qur'an (Indo-Pak 15-line, Noore Hira)",
            "bn" to "হাফেজী কুরআন (ইন্দো-পাক ১৫ লাইন, নূরে হেরা)",
            "ur" to "حافظی قرآن (ہند و پاک ۱۵ سطری، نورِ حرا)",
            "hi" to "हाफ़िज़ी क़ुरआन (इंडो-पाक 15 पंक्ति, नूरे हिरा)",
            "in" to "Al-Qur'an Hafizi (Indo-Pak 15 baris, Noore Hira)",
            "tr" to "Hafızlık Kur'anı (Hint-Pak 15 satır, Noore Hira)",
            "fa" to "قرآن حافظی (هند و پاک ۱۵ سطری، نور حرا)",
            "fr" to "Coran Hafizi (Indo-Pak 15 lignes, Noore Hira)"
        ),
        fontKey = "noorehira",
        indoPak = true
    );

    /**
     * [text] as this font must receive it. The app's text is Uthmani (Madinah); the Indo-Pak fonts
     * write five marks the Indo-Pak way: alif waṣla ٱ as a plain alif, the Uthmani sukun ۡ as the
     * round jazm ْ, and open tanween ٞ ٖ ٗ as ٌ ٍ ً. Each is one character for one, so every
     * highlight span of the content stays exact. Other fonts get [text] unchanged.
     */
    /** Indo-Pak fonts draw compact letters; verses use this scale so all fonts read at one size. */
    val verseScale: Float get() = if (indoPak) 1.25f else 1f

    fun script(text: String): String {
        if (!indoPak) return text
        val out = CharArray(text.length)
        for (i in text.indices) {
            out[i] = when (val c = text[i]) {
                '\u0671' -> '\u0627'
                '\u06E1' -> '\u0652'
                '\u065E' -> '\u064C'
                '\u0656' -> '\u064D'
                '\u0657' -> '\u064B'
                else -> c
            }
        }
        return String(out)
    }

    companion object {
        val DEFAULT = MADANI_KFGQPC

        /** Restores a saved choice, including names of styles that were renamed or removed. */
        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: when (name) {
            "UTHMANIC_HAFS", "CLASSIC_MADANI_1405", "MADANI_TAJWEED", "SHEMERLY_AMIRIYA", "UTHMANI_AMIRI",
            "MUSHAF_WARSH", "MUSHAF_QALOON", "KFGQPC_WARSH", "KFGQPC_QALOUN" -> MADANI_KFGQPC
            "NURANI_HAFEZI", "INDOPAK", "INDOPAK_NASKH" -> NOORANI
            "INDOPAK_NASTALEEQ", "NOTO_NASTALEEQ" -> HAFEZI
            "MODERN_MADANI_1440", "SCHEHERAZADE", "KITAB", "KITAB_QURAN" -> SCHEHERAZADE_NASKH
            "MUSHAF_UNICODE" -> NOTO_NASKH
            "AMIRI" -> AMIRI_CLASSIC
            else -> DEFAULT
        }
    }
}
