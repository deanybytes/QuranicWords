package com.quranicwords.app.core.domain.model

/**
 * The ~10 Quran script styles most requested/recognized by readers (mirrors what mainstream
 * Quran apps offer). [fontKey] points at a bundled OFL-licensed font when [isBundled] is true;
 * otherwise the style is selectable but renders with the system default Arabic font until the
 * real licensed font file is sourced - see the plan's font-picker addendum for why the true
 * IndoPak/Nurani/Taha/Al-Qalam/KFGQPC binaries aren't fabricated or silently substituted.
 */
enum class QuranFontStyle(
    val displayNameEn: String,
    val displayNameBn: String,
    val fontKey: String?,
    val isBundled: Boolean
) {
    UTHMANI_AMIRI("Uthmani (Amiri)", "উসমানী (আমিরি)", "amiri", true),
    SCHEHERAZADE("Scheherazade Naskh", "শেহেরজাদ নাসখ", "scheherazade", true),
    NOTO_NASKH("Simple Naskh (Noto)", "সাধারণ নাসখ (নোটো)", "noto_naskh", true),
    INDOPAK("IndoPak", "ইন্দো-পাক", null, false),
    INDOPAK_NASTALEEQ("IndoPak Nastaleeq", "ইন্দো-পাক নাসতা'লীক", null, false),
    NURANI("Nurani", "নূরানী", null, false),
    TAHA("Taha Naskh", "তাহা নাসখ", null, false),
    AL_QALAM("Al-Qalam Quran Majeed", "আল-ক্বলম কুরআন মাজীদ", null, false),
    KFGQPC_UTHMANIC("KFGQPC Uthmanic (Madinah Mushaf)", "কেএফজিকিউপিসি উসমানিক (মদীনা মুসহাফ)", null, false),
    MADANI_SIMPLE("Madani Simple", "মাদানী সিম্পল", null, false);

    companion object {
        val DEFAULT = UTHMANI_AMIRI
        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: DEFAULT
    }
}
