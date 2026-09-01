package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.Language

/** "3m 40s" / "45s" style duration for the Home history card and any future completed-unit
 * duration display - [millis] is nullable since [com.quranicwords.app.core.data.local.entity
 * .UserProgressEntity.durationMillis] defaults to null for rows written before that field existed
 * and for the schema default; returns an em dash for those rather than a misleading "0s". */
fun formatDuration(millis: Long?, language: Language = Language.ENGLISH): String {
    if (millis == null || millis < 0) return "—"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val formattedMin = VerseReferenceFormatter.formatDigits(minutes.toString(), language)
    val formattedSec = VerseReferenceFormatter.formatDigits(seconds.toString(), language)
    val minUnit = when (language) {
        Language.BANGLA -> "মি"
        Language.URDU -> "منٹ"
        else -> "m"
    }
    val secUnit = when (language) {
        Language.BANGLA -> "সে"
        Language.URDU -> "سیکنڈ"
        else -> "s"
    }
    return if (minutes > 0) "$formattedMin$minUnit $formattedSec$secUnit" else "$formattedSec$secUnit"
}

