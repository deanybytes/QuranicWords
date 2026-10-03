package com.quranicwords.app.core.domain.model

enum class Language(val tag: String, val nativeName: String) {
    ENGLISH("en", "English"),
    BANGLA("bn", "বাংলা"),
    URDU("ur", "اردو"),
    HINDI("hi", "हिन्दी"),
    INDONESIAN("in", "Bahasa Indonesia"),
    TURKISH("tr", "Türkçe"),
    PERSIAN("fa", "فارسی"),
    FRENCH("fr", "Français");

    val locale: java.util.Locale
        get() = if (this == INDONESIAN) java.util.Locale.forLanguageTag("in") else java.util.Locale.forLanguageTag(tag)

    companion object {
        /** Tags of languages the app used to offer (Malay, Hausa, Swahili) and has since withdrawn.
         * A learner who had picked one of them (stored tag in DataStore, or in a backup file) is
         * moved to English - see [isRetiredTag] and `UserPreferencesDataStore.migrateRetiredLanguage`. */
        private val RETIRED_TAGS = setOf("ms", "msa", "may", "ha", "hau", "sw", "swa")

        /** Whether [tag] names a withdrawn language that [fromTag] silently maps to English. */
        fun isRetiredTag(tag: String?): Boolean = tag != null && tag.lowercase() in RETIRED_TAGS

        fun fromTag(tag: String?): Language? = entries.find { it.tag.equals(tag, ignoreCase = true) } ?: when (tag?.lowercase()) {
            "id" -> INDONESIAN
            "ben" -> BANGLA
            "urd" -> URDU
            "hin" -> HINDI
            "fas", "per" -> PERSIAN
            "fra" -> FRENCH
            "tur" -> TURKISH
            "eng" -> ENGLISH
            else -> if (isRetiredTag(tag)) ENGLISH else null
        }
    }
}
