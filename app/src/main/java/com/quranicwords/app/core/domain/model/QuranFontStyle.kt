package com.quranicwords.app.core.domain.model

/**
 * The ~10 Quran script styles most requested/recognized by readers (mirrors what mainstream
 * Quran apps offer). [fontKey] points at a bundled OFL-licensed font when [isBundled] is true;
 * otherwise the style is selectable but renders with the system default Arabic font until the
 * real licensed font file is sourced - see the plan's font-picker addendum for why the true
 * IndoPak/Nurani/Taha/Al-Qalam/KFGQPC binaries aren't fabricated or silently substituted.
 *
 * [displayName] only has `en`/`bn` entries authored so far - other languages fall back to English
 * via [LocalizedText.get]'s fallback, same as any other not-yet-translated field. Font style
 * names are often kept close to their original form across languages anyway (proper nouns/script
 * names), so this isn't a priority gap the way UI strings or vocabulary content are.
 */
enum class QuranFontStyle(
    val displayName: LocalizedText,
    val fontKey: String?,
    val isBundled: Boolean
) {
    UTHMANI_AMIRI(mapOf("en" to "Uthmani (Amiri)", "bn" to "উসমানী (আমিরি)"), "amiri", true),
    SCHEHERAZADE(mapOf("en" to "Scheherazade Naskh", "bn" to "শেহেরজাদ নাসখ"), "scheherazade", true),
    NOTO_NASKH(mapOf("en" to "Simple Naskh (Noto)", "bn" to "সাধারণ নাসখ (নোটো)"), "noto_naskh", true),
    INDOPAK(mapOf("en" to "IndoPak", "bn" to "ইন্দো-পাক"), null, false),
    INDOPAK_NASTALEEQ(mapOf("en" to "IndoPak Nastaleeq", "bn" to "ইন্দো-পাক নাসতা'লীক"), null, false),
    NURANI(mapOf("en" to "Nurani", "bn" to "নূরানী"), null, false),
    TAHA(mapOf("en" to "Taha Naskh", "bn" to "তাহা নাসখ"), null, false),
    AL_QALAM(mapOf("en" to "Al-Qalam Quran Majeed", "bn" to "আল-ক্বলম কুরআন মাজীদ"), null, false),
    KFGQPC_UTHMANIC(mapOf("en" to "KFGQPC Uthmanic (Madinah Mushaf)", "bn" to "কেএফজিকিউপিসি উসমানিক (মদীনা মুসহাফ)"), null, false),
    MADANI_SIMPLE(mapOf("en" to "Madani Simple", "bn" to "মাদানী সিম্পল"), null, false);

    companion object {
        val DEFAULT = UTHMANI_AMIRI
        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: DEFAULT
    }
}
