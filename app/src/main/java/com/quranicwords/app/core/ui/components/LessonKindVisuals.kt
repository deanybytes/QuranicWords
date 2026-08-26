package com.quranicwords.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.ui.theme.BrandGold

/**
 * Visual styling definition for each [LessonKind], differentiating regular lessons from
 * lesson-level quizzes, section exams, section reviews, chapter exams, and grand reviews.
 */
data class LessonKindVisual(
    val icon: ImageVector,
    val accentColor: Color,
    val containerColor: Color,
    val onContainerColor: Color,
    val labelResId: Int,
    val isQuizOrExam: Boolean
)

/**
 * Resolves the icon, accent color, container tint, and localized badge label for a given [LessonKind].
 */
@Composable
fun rememberLessonKindVisual(kind: LessonKind): LessonKindVisual {
    val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    return when (kind) {
        LessonKind.REGULAR -> LessonKindVisual(
            icon = Icons.AutoMirrored.Filled.MenuBook,
            accentColor = MaterialTheme.colorScheme.primary,
            containerColor = if (isDark) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            } else {
                MaterialTheme.colorScheme.primaryContainer
            },
            onContainerColor = if (isDark) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            },
            labelResId = R.string.lesson_kind_regular,
            isQuizOrExam = false
        )

        LessonKind.LESSON_FLASHBACK -> {
            val cyanAccent = if (isDark) Color(0xFF26C6DA) else Color(0xFF0097A7)
            LessonKindVisual(
                icon = Icons.Filled.Quiz,
                accentColor = cyanAccent,
                containerColor = cyanAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                onContainerColor = cyanAccent,
                labelResId = R.string.lesson_kind_lesson_quiz,
                isQuizOrExam = true
            )
        }

        LessonKind.SECTION_EXAM -> {
            val goldAccent = BrandGold
            LessonKindVisual(
                icon = Icons.Filled.MilitaryTech,
                accentColor = goldAccent,
                containerColor = goldAccent.copy(alpha = if (isDark) 0.24f else 0.16f),
                onContainerColor = goldAccent,
                labelResId = R.string.lesson_kind_section_exam,
                isQuizOrExam = true
            )
        }

        LessonKind.SECTION_FLASHBACK -> {
            val purpleAccent = if (isDark) Color(0xFFBA68C8) else Color(0xFF8E24AA)
            LessonKindVisual(
                icon = Icons.Filled.HistoryEdu,
                accentColor = purpleAccent,
                containerColor = purpleAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                onContainerColor = purpleAccent,
                labelResId = R.string.lesson_kind_section_flashback,
                isQuizOrExam = true
            )
        }

        LessonKind.CHAPTER_EXAM -> {
            val rubyAccent = if (isDark) Color(0xFFFF4081) else Color(0xFFE91E63)
            LessonKindVisual(
                icon = Icons.Filled.EmojiEvents,
                accentColor = rubyAccent,
                containerColor = rubyAccent.copy(alpha = if (isDark) 0.25f else 0.16f),
                onContainerColor = rubyAccent,
                labelResId = R.string.lesson_kind_chapter_exam,
                isQuizOrExam = true
            )
        }

        LessonKind.CHAPTER_FLASHBACK -> {
            val tealAccent = if (isDark) Color(0xFF4DB6AC) else Color(0xFF00897B)
            LessonKindVisual(
                icon = Icons.Filled.AutoAwesome,
                accentColor = tealAccent,
                containerColor = tealAccent.copy(alpha = if (isDark) 0.22f else 0.14f),
                onContainerColor = tealAccent,
                labelResId = R.string.lesson_kind_chapter_flashback,
                isQuizOrExam = true
            )
        }
    }
}
