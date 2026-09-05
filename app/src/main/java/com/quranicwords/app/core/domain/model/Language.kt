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

    companion object {
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

