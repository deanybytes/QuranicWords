package com.quranicwords.app.core.util

import kotlinx.serialization.json.Json

/** Shared kotlinx.serialization config: lenient and unknown-key-tolerant for bundled/seed JSON. */
val AppJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    encodeDefaults = true
}
