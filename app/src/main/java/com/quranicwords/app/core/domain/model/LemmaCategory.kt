package com.quranicwords.app.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
enum class LemmaCategory {
    NOUN,
    VERB,
    PARTICLE,
    MIXED
}
