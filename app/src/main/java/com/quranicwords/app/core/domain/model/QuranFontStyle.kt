package com.quranicwords.app.core.domain.model

/**
 * Quran script styles offered in the font picker. [fontKey] points at a bundled OFL-licensed font
 * when [isBundled] is true; [isBundled] is currently always true - every entry here has a real,
 * verified-license font file backing it (`app/src/main/res/font/`, license text under
 * `app/src/main/assets/font_licenses/`). The flag is kept (rather than dropped) since new styles
 * may be added later without a sourced font yet, same pattern as before.
 *
 * [INDOPAK] and [INDOPAK_NASTALEEQ] are bundled via genuinely open (SIL OFL) substitutes in the
 * same script family (Lateef, Noto Nastaliq Urdu) rather than the exact named commercial
 * typefaces - [displayName] says so honestly, same discipline as [NOTO_NASKH].
 *
 * A prior version of this enum also offered Nurani, Taha Naskh, Al-Qalam Quran Majeed, KFGQPC
 * Uthmanic, and Madani Simple. All five were removed after a real license check found none of
 * them clears the bar this app holds every other bundled asset to: the KFGQPC-published fonts
 * (Taha, KFGQPC Uthmanic, Madani Simple) ship under a restrictive EULA that explicitly disallows
 * modification, is ambiguous about redistribution ("cannot be ... Reproduced" alongside a grant to
 * "Distribute"), and multiple sources describe commercial use as requiring separate permission
 * from the publisher - not compatible with bundling in a GPL-3.0 open-source app. Al-Qalam Quran
 * Majeed has no published license at all ("No License Available" per the font repositories that
 * host it). A genuinely OFL-licensed IndoPak-Mushaf-style font was found for Nurani's slot
 * (DigitalKhatt/Tarteel's `indopakfont`), but it's a variable OpenType-CFF2 font that `fonttools`
 * couldn't safely reduce to a static instance (a real `varLib.instancer` bug on this specific
 * file's CFF2 charstrings), and bundling a variable CFF2 font whose rendering on minSdk 24-25
 * devices can't be confirmed without physical device testing isn't a risk worth taking for a
 * font. See `docs/CONTENT_SOURCES.md`'s "Quran script fonts" section for the full writeup -
 * rather than leaving these five selectable-but-unbundled indefinitely, removing them outright
 * was the more honest outcome once no safe path to bundling them was found.
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
    INDOPAK_NASTALEEQ(mapOf("en" to "Nastaliq (Noto)", "bn" to "নাসতা'লীক (নোটো)"), "noto_nastaliq_urdu", true);

    companion object {
        val DEFAULT = UTHMANI_AMIRI
        fun fromName(name: String?): QuranFontStyle = entries.find { it.name == name } ?: DEFAULT
    }
}
