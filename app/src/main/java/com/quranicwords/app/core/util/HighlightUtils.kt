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
     * 1. If [meaningHighlight] is provided, matches against full words or token boundaries.
     * 2. If not found, splits [meaning] alternatives and matches against exact tokens or stem matches.
     * 3. Prevents matching short syllables inside unrelated adjacent words.
     */
    fun findMeaningHighlightRange(
        verseTranslation: String?,
        meaningHighlight: String?,
        meaning: String?
    ): Pair<Int, Int>? {
        if (verseTranslation.isNullOrBlank()) return null

        val tokenRegex = Regex("[^\\s,.;:!?।()\\[\\]{}\"'`«»„“”/\\\\-]+")
        val tokens = tokenRegex.findAll(verseTranslation).map { match ->
            Triple(match.range.first, match.range.last + 1, match.value)
        }.toList()

        // Pass 1: Direct meaningHighlight matching
        if (!meaningHighlight.isNullOrBlank()) {
            val hl = meaningHighlight.trim()
            val normHl = normalize(hl)
            // Exact token match
            for ((start, end, tok) in tokens) {
                if (tok.equals(hl, ignoreCase = true) || normalize(tok).equals(normHl, ignoreCase = true)) {
                    return Pair(start, end)
                }
            }
            // Multi-word phrase search with word boundaries
            val idx = verseTranslation.indexOf(hl, ignoreCase = true)
            if (idx >= 0) {
                val beforeOk = idx == 0 || !isWordChar(verseTranslation[idx - 1])
                val afterOk = (idx + hl.length == verseTranslation.length) || !isWordChar(verseTranslation[idx + hl.length])
                if (beforeOk && afterOk) {
                    return expandToWordBoundaries(verseTranslation, idx, idx + hl.length)
                }
            }
            // Token stem / prefix match
            if (hl.length >= 3) {
                for ((start, end, tok) in tokens) {
                    val normTok = normalize(tok)
                    if (tok.length >= 3 && (normTok.startsWith(normHl, ignoreCase = true) || normHl.startsWith(normTok, ignoreCase = true))) {
                        return Pair(start, end)
                    }
                }
            }
        }

        if (meaning.isNullOrBlank()) return null

        // Pass 2: Clean meaning, extract candidate alternatives
        val cleaned = meaning
            .replace(Regex("\\(.*?\\)|\\[.*?\\]"), "")
            .trim()
        if (cleaned.isEmpty()) return null

        // Split alternatives: "sign, verse" -> ["sign", "verse"], "Allah / God" -> ["Allah", "God"]
        val rawParts = cleaned.split(Regex("[,;/|]|\\bor\\b", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.length >= 2 }

        val candidates = (listOf(cleaned) + rawParts)
            .distinct()
            .sortedByDescending { it.length }

        // Match exact token
        for (cand in candidates) {
            val normCand = normalize(cand)
            for ((start, end, tok) in tokens) {
                if (tok.equals(cand, ignoreCase = true) || normalize(tok).equals(normCand, ignoreCase = true)) {
                    return Pair(start, end)
                }
            }
        }

        // Match multi-word candidate phrase
        for (cand in candidates) {
            if (" " in cand) {
                val idx = verseTranslation.indexOf(cand, ignoreCase = true)
                if (idx >= 0) {
                    val beforeOk = idx == 0 || !isWordChar(verseTranslation[idx - 1])
                    val afterOk = (idx + cand.length == verseTranslation.length) || !isWordChar(verseTranslation[idx + cand.length])
                    if (beforeOk && afterOk) {
                        return expandToWordBoundaries(verseTranslation, idx, idx + cand.length)
                    }
                }
            }
        }

        // Match token stem / prefix (min length 3 to prevent false positive short matches)
        for (cand in candidates) {
            val normCand = normalize(cand)
            if (normCand.length >= 3) {
                for ((start, end, tok) in tokens) {
                    val normTok = normalize(tok)
                    if (normTok.length >= 3 && (normTok.startsWith(normCand, ignoreCase = true) || normCand.startsWith(normTok, ignoreCase = true))) {
                        return Pair(start, end)
                    }
                }
            }
        }

        // Match individual words from multi-word candidates
        val subWords = candidates.flatMap { cand ->
            cand.split(Regex("[^\\p{L}\\p{N}']+"))
                .filter { it.length >= 3 }
        }.distinct().sortedByDescending { it.length }

        for (word in subWords) {
            val normWord = normalize(word)
            for ((start, end, tok) in tokens) {
                val normTok = normalize(tok)
                if (normTok.equals(normWord, ignoreCase = true)) {
                    return Pair(start, end)
                }
                if (normTok.length >= 3 && (normTok.startsWith(normWord, ignoreCase = true) || normWord.startsWith(normTok, ignoreCase = true))) {
                    return Pair(start, end)
                }
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

