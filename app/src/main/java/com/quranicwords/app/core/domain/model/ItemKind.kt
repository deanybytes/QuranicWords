package com.quranicwords.app.core.domain.model

import kotlinx.serialization.Serializable

/** Which corpus a practiced item belongs to. Single-value today (vocabulary words only) but kept
 * as an enum rather than removed outright, since [com.quranicwords.app.core.data.local
 * .entity.ExerciseAttemptEntity] persists it and [com.quranicwords.app.core.domain.repository
 * .BackupRepository] round-trips it through [Serializable]. */
@Serializable
enum class ItemKind { WORD }
