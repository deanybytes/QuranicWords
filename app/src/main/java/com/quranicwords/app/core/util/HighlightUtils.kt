package com.quranicwords.app.core.util

import java.text.Normalizer

object HighlightUtils {
    /**
     * Normalizes text by decomposing diacritics / combining characters (e.g. macron vowels).
     */
    private fun normalize(text: String): String {
        return Normalizer.normalize(text, Normalizer.Form.NFKD)
            .replace("\\p{M}+".toRegex(), "")
    }

    /**
     * Finds the start and end (exclusive) character range in [verseTranslation] that corresponds
     * to the taught meaning.
     *
     * 1. If [meaningHighlight] is provided, tries case-insensitive substring match.
     * 2. If not found, cleans [meaning] (removes parentheticals, splits comma/slash/semicolon alternatives)
     *    and searches for phrases or key content words bounded by word boundaries.
     */
    fun findMeaningHighlightRange(
        verseTranslation: String?,
        meaningHighlight: String?,
        meaning: String?
    ): Pair<Int, Int>? {
        if (verseTranslation.isNullOrBlank()) return null

        // Pass 1: Direct meaningHighlight substring search (case-insensitive)
        if (!meaningHighlight.isNullOrBlank()) {
            val idx = verseTranslation.indexOf(meaningHighlight, ignoreCase = true)
            if (idx >= 0) {
                return Pair(idx, idx + meaningHighlight.length)
            }
            // Try normalized search for meaningHighlight (e.g. macron characters)
            val normTrans = normalize(verseTranslation)
            val normHl = normalize(meaningHighlight)
            val normIdx = normTrans.indexOf(normHl, ignoreCase = true)
            if (normIdx >= 0) {
                val end = (normIdx + meaningHighlight.length).coerceAtMost(verseTranslation.length)
                return Pair(normIdx, end)
            }
        }

        if (meaning.isNullOrBlank()) return null

        // Pass 2: Clean meaning, extract candidate phrases and words
        val cleaned = meaning
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .trim()
        if (cleaned.isEmpty()) return null

        val normTrans = normalize(verseTranslation)

        // Split alternatives: "sign, verse" -> ["sign", "verse"], "Allah / God" -> ["Allah", "God"]
        val rawParts = cleaned.split(Regex("[,;/|]|\\bor\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.length >= 2 }

        val candidates = (listOf(cleaned) + rawParts)
            .distinct()
            .sortedByDescending { it.length }

        // Try candidate phrases
        for (cand in candidates) {
            val normCand = normalize(cand)
            if (normCand.isBlank()) continue
            val regex = Regex("\\b${Regex.escape(normCand)}\\b", RegexOption.IGNORE_CASE)
            val match = regex.find(normTrans)
            if (match != null) {
                val start = match.range.first.coerceIn(0, verseTranslation.length)
                val end = (match.range.last + 1).coerceIn(start, verseTranslation.length)
                return Pair(start, end)
            }
        }

        // Try individual words from candidates
        val words = candidates.flatMap { cand ->
            cand.split(Regex("[^\\p{L}\\p{N}']+"))
                .filter { it.length >= 2 }
        }.distinct().sortedByDescending { it.length }

        for (word in words) {
            val normWord = normalize(word)
            if (normWord.isBlank()) continue
            val regex = Regex("\\b${Regex.escape(normWord)}\\b", RegexOption.IGNORE_CASE)
            val match = regex.find(normTrans)
            if (match != null) {
                val start = match.range.first.coerceIn(0, verseTranslation.length)
                val end = (match.range.last + 1).coerceIn(start, verseTranslation.length)
                return Pair(start, end)
            }
        }

        return null
    }
}
