package com.quranicwords.app.core.domain.model

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromName(name: String?): ThemeMode = entries.find { it.name == name } ?: SYSTEM
    }
}
