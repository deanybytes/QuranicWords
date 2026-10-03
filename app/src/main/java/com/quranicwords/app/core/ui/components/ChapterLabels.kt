package com.quranicwords.app.core.ui.components

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * A "Chapter N"-style label, fully resource-driven: languages that name early chapters by ordinal
 * word ("১ম অধ্যায়", "پہلا باب", "فصل اول") supply `R.array.chapter_ordinals` and get
 * [ordinalRes]; everyone else (and any chapter past the array) gets [numericRes] with the number
 * in the language's own digits.
 */
@Composable
fun chapterLabel(
    number: Int,
    language: Language,
    @StringRes numericRes: Int = R.string.chapter_label,
    @StringRes ordinalRes: Int = R.string.chapter_label_ordinal
): String {
    val ordinal = stringArrayResource(R.array.chapter_ordinals).getOrNull(number - 1)?.takeIf { it.isNotBlank() }
    return if (ordinal != null) stringResource(ordinalRes, ordinal)
    else stringResource(numericRes, VerseReferenceFormatter.formatNumber(number, language))
}
