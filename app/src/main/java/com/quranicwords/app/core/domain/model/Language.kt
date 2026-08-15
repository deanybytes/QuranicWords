package com.quranicwords.app.core.domain.model

enum class Language(val tag: String) {
    ENGLISH("en"),
    BANGLA("bn");

    companion object {
        fun fromTag(tag: String?): Language? = entries.find { it.tag == tag }
    }
}
