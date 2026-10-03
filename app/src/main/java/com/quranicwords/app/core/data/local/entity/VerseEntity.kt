package com.quranicwords.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.LocalizedText

/**
 * One complete ayah, seeded from `content/verses.json` (tools/pipeline output) - the single copy
 * of every verse a word's senses point at (see `ExerciseContent.SenseRef.verse`). [key] is
 * "surah:ayah". [wordByWord] and [translation] are keyed by content-language tag; every sense's
 * spans index into exactly these strings, so they are stored verbatim and never re-normalized.
 */
@Entity(tableName = "verses")
data class VerseEntity(
    @PrimaryKey val key: String,
    /** The pipeline's English-style citation, e.g. "Al-Baqarah 2:8" - formatted for display by
     * `VerseReferenceFormatter`. */
    val reference: String,
    /** The full ayah in Uthmani script. */
    val arabic: String,
    @ColumnInfo(name = "wbwJson") val wordByWord: LocalizedText,
    @ColumnInfo(name = "translationJson") val translation: LocalizedText
)
