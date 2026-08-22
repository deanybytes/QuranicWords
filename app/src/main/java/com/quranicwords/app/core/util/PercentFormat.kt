package com.quranicwords.app.core.util

import kotlin.math.roundToInt

/** Rounds to one decimal place, dropping a trailing ".0" (e.g. 84.8412 -> "84.8", 6.0 -> "6") -
 * shared between [com.quranicwords.app.feature.intro.IntroScreen] and
 * [com.quranicwords.app.feature.home.HomeScreen], both of which show
 * [com.quranicwords.app.core.data.local.entity.ChapterEntity.quranOccurrencePercent]-derived
 * values to the learner. */
fun formatPercent(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded == rounded.toInt().toDouble()) rounded.toInt().toString() else rounded.toString()
}
