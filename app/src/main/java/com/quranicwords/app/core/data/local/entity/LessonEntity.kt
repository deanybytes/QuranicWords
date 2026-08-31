package com.quranicwords.app.core.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LocalizedText
import kotlinx.serialization.Serializable

/**
 * What a lesson row actually is - most are [REGULAR] teach-then-quiz content, but exams and
 * flashback reviews are modeled as lessons too (reusing the whole exercise/attempt/scoring
 * pipeline) rather than as separate entities. See [com.quranicwords.app.core.data.repository
 * .ProgressRepositoryImpl] for the unlock-gating rules per kind - every kind except [REGULAR]
 * requires a passing score to unlock what comes next.
 *
 * Flashback kinds each pull questions from *earlier siblings under the same parent only* (never
 * the unit just finished, never a later one): [LESSON_FLASHBACK] from earlier lessons in the same
 * section, [SECTION_FLASHBACK] from earlier sections in the same chapter, [CHAPTER_FLASHBACK] from
 * earlier chapters. Each is skipped for the first sibling (lesson 1 of a section, section 1 of a
 * chapter, chapter 1) since there's nothing earlier to flash back to.
 */
@Serializable
enum class LessonKind { REGULAR, LESSON_FLASHBACK, SECTION_EXAM, SECTION_FLASHBACK, CHAPTER_EXAM, CHAPTER_FLASHBACK }

/**
 * [chapterId] is always set; [sectionId] is set for every kind except [LessonKind.CHAPTER_EXAM]/
 * [LessonKind.CHAPTER_FLASHBACK], which belong to the whole chapter rather than one section.
 * [sortOrder] is scoped to whichever parent the lesson belongs to - within [sectionId] for
 * section-scoped lessons, within [chapterId] for the two chapter-scoped kinds - see
 * `ProgressRepositoryImpl.unlockNextLesson`'s three-tier fallback (next lesson in section -> next
 * section in chapter -> next chapter) for how that scoping is walked.
 */
@Serializable
@Entity(
    tableName = "lessons",
    foreignKeys = [
        ForeignKey(
            entity = ChapterEntity::class,
            parentColumns = ["id"],
            childColumns = ["chapterId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = SectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("chapterId"), Index("sectionId")]
)
data class LessonEntity(
    @PrimaryKey val id: String,
    val chapterId: String,
    val sectionId: String?,
    val title: LocalizedText,
    val sortOrder: Int,
    val kind: LessonKind = LessonKind.REGULAR,
    val category: LemmaCategory = LemmaCategory.NOUN
)
