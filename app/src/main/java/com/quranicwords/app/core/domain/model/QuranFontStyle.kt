package com.quranicwords.app.core.domain.model

/**
 * The ~10 Quran script styles most requested/recognized by readers (mirrors what mainstream
 * Quran apps offer). [fontKey] points at a bundled OFL-licensed font when [isBundled] is true;
 * otherwise the style is selectable but renders with the system default Arabic font until a real
 * licensed font file is sourced. [INDOPAK] and [INDOPAK_NASTALEEQ] are bundled via genuinely
 * open (SIL OFL) substitutes in the same script family (Lateef, Noto Nastaliq Urdu) rather than
 * the exact named commercial typefaces - [displayName] says so honestly, same discipline as
 * [NOTO_NASKH]. [NURANI]/[TAHA]/[AL_QALAM]/[KFGQPC_UTHMANIC]/[MADANI_SIMPLE] specifically name
 * real commercial/community font projects with no confirmed open-license equivalent found yet -
 * see `docs/CONTENT_SOURCES.md` for why those five stay unbundled rather than getting a
 * lower-confidence substitute forced onto them too.
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
    INDOPAK(mapOf("en" to "IndoPak-style Naskh (Lateef)", "bn" to "ইন্দো-পাক ধাঁচের নাসখ (লতীফ)"), "lateef", true),
    INDOPAK_NASTALEEQ(mapOf("en" to "Nastaliq (Noto)", "bn" to "নাসতা'লীক (নোটো)"), "noto_nastaliq_urdu", true),
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
