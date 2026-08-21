package com.quranicwords.app.core.domain.model

/**
 * Language-tag-keyed text (keys match [Language.tag]), replacing the old flat `*En`/`*Bn` field
 * pairs that couldn't scale past 2 languages. A plain `Map<String, String>` typealias rather than
 * a wrapper class - no serialization/Room-converter edge cases to work around, kotlinx.serialization
 * and Room both already know how to handle `Map<String, String>` natively.
 */
typealias LocalizedText = Map<String, String>

/** Looks up [language], falling back to [fallback] (English by default), then to any entry at
 * all, then to empty string - never throws, mirrors the old `*En ?: ""`-style safety. */
fun LocalizedText.get(language: Language, fallback: Language = Language.ENGLISH): String =
    this[language.tag] ?: this[fallback.tag] ?: this.values.firstOrNull().orEmpty()

/** Same lookup as [get] but returns null instead of falling back to an empty string - for
 * optional fields (e.g. highlight spans) where "not present" must stay distinguishable from
 * "present but empty". */
fun LocalizedText.getOrNull(language: Language, fallback: Language = Language.ENGLISH): String? =
    this[language.tag] ?: this[fallback.tag]
