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
     * Checks if a character is part of a word (excluding whitespace and sentence punctuation).
     */
    fun isWordChar(c: Char): Boolean {
        if (c.isWhitespace()) return false
        if (c in "।.,!?;:\"'()[]{}<>-—–/\\»«“”‘’`…") return false
        return true
    }

    /**
     * Expands a character range outwards to full whitespace / punctuation word boundaries
     * to avoid breaking agglutinative words, grammatical suffixes, or complex Indic conjuncts.
     */
    fun expandToWordBoundaries(text: String, start: Int, end: Int): Pair<Int, Int> {
        if (text.isEmpty() || start !in 0..text.length || end !in start..text.length) {
            return Pair(start.coerceIn(0, text.length), end.coerceIn(start.coerceIn(0, text.length), text.length))
        }

        var s = start
        while (s > 0 && isWordChar(text[s - 1])) {
            s--
        }

        var e = end
        while (e < text.length && isWordChar(text[e])) {
            e++
        }

        return Pair(s, e)
    }

    /**
     * Finds the start and end (exclusive) character range in [verseTranslation] that corresponds
     * to the taught meaning, expanding to full word boundaries to ensure complete unbroken words.
     *
     * 1. If [meaningHighlight] is provided, tries case-insensitive substring search.
     * 2. If not found, cleans [meaning] (removes parentheticals, splits comma/slash/semicolon alternatives)
     *    and searches for phrases or key content words.
     * 3. Expands the resolved range to full word boundaries.
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
                return expandToWordBoundaries(verseTranslation, idx, idx + meaningHighlight.length)
            }
            // Try normalized search for meaningHighlight (e.g. macron characters)
            val normTrans = normalize(verseTranslation)
            val normHl = normalize(meaningHighlight)
            val normIdx = normTrans.indexOf(normHl, ignoreCase = true)
            if (normIdx >= 0) {
                val end = (normIdx + meaningHighlight.length).coerceAtMost(verseTranslation.length)
                return expandToWordBoundaries(verseTranslation, normIdx, end)
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
                return expandToWordBoundaries(verseTranslation, start, end)
            }
            // Fallback substring search within words if regex boundary fails for Indic/accented
            val subIdx = normTrans.indexOf(normCand, ignoreCase = true)
            if (subIdx >= 0) {
                val end = (subIdx + normCand.length).coerceAtMost(verseTranslation.length)
                return expandToWordBoundaries(verseTranslation, subIdx, end)
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
                return expandToWordBoundaries(verseTranslation, start, end)
            }
            val subIdx = normTrans.indexOf(normWord, ignoreCase = true)
            if (subIdx >= 0) {
                val end = (subIdx + normWord.length).coerceAtMost(verseTranslation.length)
                return expandToWordBoundaries(verseTranslation, subIdx, end)
            }
        }

        return null
    }

    /**
     * Strips Arabic diacritics, normalization forms, and dagger alif for fuzzy Quranic token matching.
     */
    fun stripArabicTashkeel(text: String?): String {
        if (text.isNullOrBlank()) return ""
        return text
            .replace('\u0670', 'ا')
            .replace("[\u064B-\u065F\u06D6-\u06ED\uFEFF]".toRegex(), "")
            .replace("[إأآٱ]".toRegex(), "ا")
            .replace('ة', 'ه')
            .replace('ى', 'ي')
            .trim()
    }

    private val QURANIC_PREFIXES = listOf("وال", "فال", "بال", "كال", "لل", "ال", "و", "ف", "ب", "ل", "ك", "س", "ي", "ت", "ن", "ا")

    private fun stripPrefixes(norm: String): String {
        for (p in QURANIC_PREFIXES) {
            if (norm.startsWith(p) && norm.length - p.length >= 2) {
                return norm.substring(p.length)
            }
        }
        return norm
    }

    /**
     * Finds the exact character range (start, end) of an Arabic word in a full verse.
     */
    fun findArabicSpanInVerse(arabicWord: String?, verseArabic: String?): Pair<Int, Int>? {
        if (arabicWord.isNullOrBlank() || verseArabic.isNullOrBlank()) return null

        val exactIdx = verseArabic.indexOf(arabicWord)
        if (exactIdx >= 0) {
            return Pair(exactIdx, exactIdx + arabicWord.length)
        }

        val normTarget = stripArabicTashkeel(arabicWord)
        if (normTarget.isEmpty()) return null

        val tokenMatches = "\\S+".toRegex().findAll(verseArabic).toList()

        // 1. Exact stripped token
        for (m in tokenMatches) {
            if (stripArabicTashkeel(m.value) == normTarget) {
                return Pair(m.range.first, m.range.last + 1)
            }
        }

        // 2. Token without Quranic prefix
        for (m in tokenMatches) {
            val tokNorm = stripArabicTashkeel(m.value)
            if (stripPrefixes(tokNorm) == normTarget || stripPrefixes(tokNorm) == stripPrefixes(normTarget)) {
                return Pair(m.range.first, m.range.last + 1)
            }
        }

        // 3. Substring within token
        for (m in tokenMatches) {
            val tokNorm = stripArabicTashkeel(m.value)
            if (normTarget in tokNorm) {
                return Pair(m.range.first, m.range.last + 1)
            }
        }

        return null
    }
}

