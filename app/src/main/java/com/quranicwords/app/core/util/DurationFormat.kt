package com.quranicwords.app.core.util

/** "3m 40s" / "45s" style duration for the Home history card and any future completed-unit
 * duration display - [millis] is nullable since [com.quranicwords.app.core.data.local.entity
 * .UserProgressEntity.durationMillis] defaults to null for rows written before that field existed
 * and for the schema default; returns an em dash for those rather than a misleading "0s". */
fun formatDuration(millis: Long?): String {
    if (millis == null || millis < 0) return "—"
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return if (minutes > 0) "${minutes}m ${seconds}s" else "${seconds}s"
}
