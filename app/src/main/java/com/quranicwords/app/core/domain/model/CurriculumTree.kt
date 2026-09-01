package com.quranicwords.app.core.domain.model

import com.quranicwords.app.core.data.local.entity.ChapterEntity
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.SectionEntity

data class SectionWithLessons(
    val section: SectionEntity,
    val lessons: List<LessonEntity>
)

data class ChapterWithSections(
    val chapter: ChapterEntity,
    val sections: List<SectionWithLessons>,
    val chapterLevelLessons: List<LessonEntity> = emptyList()
)
