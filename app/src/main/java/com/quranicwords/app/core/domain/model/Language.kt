package com.quranicwords.app.core.domain.model

enum class Language(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা"),
    ALBANIAN("sq", "Shqip"),
    CHINESE("zh", "中文"),
    FARSI("fa", "فارسی"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    HINDI("hi", "हिन्दी"),
    INDONESIAN("in", "Indonesia"),
    RUSSIAN("ru", "Русский"),
    TURKISH("tr", "Türkçe"),
    URDU("ur", "اردو");

    companion object {
        fun fromTag(tag: String?): Language? = entries.find { it.tag == tag }
    }
}
