package com.quranicwords.app.core.data.local

/** Comfortably under SQLite's default 999 bound-parameter limit (SQLITE_MAX_VARIABLE_NUMBER on
 * the platform SQLite of API <= 30), leaving headroom for a query's other parameters. */
const val SQLITE_IN_CHUNK_SIZE = 900

/** Runs a Room `IN (:ids)` [query] over [ids] in [SQLITE_IN_CHUNK_SIZE]-sized slices and
 * concatenates the results - a single unbounded list crashes with "too many SQL variables" once
 * it passes 999 ids (e.g. a learner with a long mistake history). Deduplicates [ids] first. */
suspend fun <T> chunkedInQuery(ids: Collection<String>, query: suspend (List<String>) -> List<T>): List<T> {
    if (ids.isEmpty()) return emptyList()
    return ids.distinct().chunked(SQLITE_IN_CHUNK_SIZE).flatMap { query(it) }
}
