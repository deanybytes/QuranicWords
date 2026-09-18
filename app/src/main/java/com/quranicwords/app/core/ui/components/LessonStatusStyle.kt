package com.quranicwords.app.core.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.quranicwords.app.core.data.local.entity.LessonStatus

/** Shared [LessonStatus] -> container color mapping, extracted so chapter/section summary nodes
 * (QW-22's collapse/expand tree) and the existing per-lesson node use the exact same color
 * language rather than three independently-drifting copies. */
@Composable
fun statusContainerColor(status: LessonStatus): Color = when (status) {
    LessonStatus.COMPLETED -> MaterialTheme.colorScheme.primary
    LessonStatus.UNLOCKED -> MaterialTheme.colorScheme.primaryContainer
    LessonStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
fun statusDefaultIconAndTint(status: LessonStatus, isCurrent: Boolean = false): Pair<ImageVector, Color> = when (status) {
    LessonStatus.COMPLETED -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.onPrimary
    LessonStatus.UNLOCKED -> if (isCurrent) {
        Icons.Filled.PlayArrow to MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        Icons.AutoMirrored.Filled.MenuBook to MaterialTheme.colorScheme.onPrimaryContainer
    }
    LessonStatus.LOCKED -> Icons.Filled.Lock to MaterialTheme.colorScheme.onSurfaceVariant
}
