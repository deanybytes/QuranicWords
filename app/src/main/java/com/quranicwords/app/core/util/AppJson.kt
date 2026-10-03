package com.quranicwords.app.core.util

import com.quranicwords.app.core.domain.model.ExerciseContent
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/** Shared kotlinx.serialization config: lenient and unknown-key-tolerant for bundled/seed JSON. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}

/**
 * Decodes one stored exercise's content JSON, or null when it can't be decoded - e.g. a row of a
 * withdrawn exercise type (the old listening exercises) left by a stale seed before a reseed, or a
 * malformed blob. Callers drop such an exercise rather than crash the whole lesson.
 */
fun decodeExerciseContentOrNull(json: String): ExerciseContent? = try {
    AppJson.decodeFromString(ExerciseContent.serializer(), json)
} catch (_: SerializationException) {
    null
} catch (_: IllegalArgumentException) {
    null
}
