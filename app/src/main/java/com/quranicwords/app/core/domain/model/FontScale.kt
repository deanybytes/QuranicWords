package com.quranicwords.app.core.domain.model

/** User-chosen text-size multiplier (Settings screen), applied app-wide via
 * `CompositionLocalProvider(LocalDensity provides ...)` in `MainActivity` - a fixed set of steps
 * rather than a free slider, matching this project's other enum-based settings
 * ([ThemeMode], [QuranFontStyle]): simpler to reason about, and every value is guaranteed to
 * have been visually checked rather than an arbitrary continuous value nobody tested. */
enum class FontScale(val multiplier: Float) {
    SMALL(0.85f),
    DEFAULT(1.0f),
    LARGE(1.15f),
    EXTRA_LARGE(1.3f);

    companion object {
        fun fromName(name: String?): FontScale = entries.find { it.name == name } ?: DEFAULT
    }
}
