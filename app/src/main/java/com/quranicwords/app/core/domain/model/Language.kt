package com.quranicwords.app.core.domain.model

enum class Language(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা"),
    URDU("ur", "اردو"),
    HINDI("hi", "हिन्दी"),
    INDONESIAN("in", "Bahasa Indonesia"),
    MALAY("ms", "Bahasa Melayu"),
    TURKISH("tr", "Türkçe"),
    PERSIAN("fa", "فارسی"),
    HAUSA("ha", "Hausa"),
    SWAHILI("sw", "Kiswahili"),
    FRENCH("fr", "Français");

    /** Whether word meanings exist in this language. Malay, Hausa and Swahili keep their
     * interface strings, but have no source-verified vocabulary data, so their learners see
     * English meanings (LocalizedText falls back) and new learners can't pick them. */
    val isContentLanguage: Boolean
        get() = this != MALAY && this != HAUSA && this != SWAHILI

    val locale: java.util.Locale
        get() = if (this == INDONESIAN) java.util.Locale.forLanguageTag("in") else java.util.Locale.forLanguageTag(tag)

    companion object {
        /** Languages offered in the pickers. */
        val selectable: List<Language> get() = entries.filter { it.isContentLanguage }

        fun fromTag(tag: String?): Language? = entries.find { it.tag.equals(tag, ignoreCase = true) } ?: when (tag?.lowercase()) {
            "id" -> INDONESIAN
            "ben" -> BANGLA
            "urd" -> URDU
            "hin" -> HINDI
            "msa", "may" -> MALAY
            "fas", "per" -> PERSIAN
            "hau" -> HAUSA
            "swa" -> SWAHILI
            "fra" -> FRENCH
            "tur" -> TURKISH
            "eng" -> ENGLISH
            else -> null
        }
    }
}

