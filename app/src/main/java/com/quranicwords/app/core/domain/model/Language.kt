package com.quranicwords.app.core.domain.model

enum class Language(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা"),
    URDU("ur", "اردو"),
    INDONESIAN("in", "Bahasa Indonesia"),
    TURKISH("tr", "Türkçe"),
    FRENCH("fr", "Français");

    companion object {
        fun fromTag(tag: String?): Language? = entries.find { it.tag.equals(tag, ignoreCase = true) } ?: when (tag?.lowercase()) {
            "id" -> INDONESIAN
            "ben" -> BANGLA
            "urd" -> URDU
            "fra" -> FRENCH
            "tur" -> TURKISH
            "eng" -> ENGLISH
            else -> null
        }
    }
}

