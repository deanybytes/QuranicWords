package com.quranicwords.app.feature.roadmap

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.QwIconButton
import com.quranicwords.app.core.ui.components.rememberLessonKindVisual
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.components.statusContainerColor
import com.quranicwords.app.core.ui.components.statusDefaultIconAndTint
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.core.util.VerseReferenceFormatter
import com.quranicwords.app.feature.home.aggregateStatus

/**
 * Full-curriculum timeline (all chapters/sections/lessons, flat - no collapse/expand, since the
 * whole point here is a bird's-eye view rather than progressive reveal) for jumping straight to
 * any *completed* or *unlocked* unit for learning or review. Locked rows are visibly greyed and inert -
 * see [isRoadmapReachable].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoadmapScreen(
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit,
    viewModel: RoadmapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.currentLessonId, uiState.isLoading) {
        val currentId = uiState.currentLessonId
        if (!uiState.isLoading && currentId != null) {
            var itemIndex = 0
            var found = false
            for (chapter in uiState.chapters) {
                if (found) break
                itemIndex++ // chapter header
                for (section in chapter.sections) {
                    if (found) break
                    itemIndex++ // section header
                    for (lesson in section.lessons) {
                        if (lesson.id == currentId) {
                            found = true
                            break
                        }
                        itemIndex++
                    }
                }
                if (!found) {
                    for (lesson in chapter.chapterLevelLessons) {
                        if (lesson.id == currentId) {
                            found = true
                            break
                        }
                        itemIndex++
                    }
                }
            }
            if (found) {
                listState.animateScrollToItem((itemIndex - 1).coerceAtLeast(0))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(
                    Elevation.raised,
                    RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                ),
                title = { Text(stringResource(R.string.roadmap_title)) },
                navigationIcon = {
                    QwIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            uiState.chapters.forEach { chapterWithSections ->
                val chapterLessonIds = chapterWithSections.sections.flatMap { s -> s.lessons.map { it.id } } +
                    chapterWithSections.chapterLevelLessons.map { it.id }
                val chapterStatus = aggregateStatus(chapterLessonIds, uiState.progressByLessonId)

                item(key = "chapter_${chapterWithSections.chapter.id}") {
                    RoadmapHeaderRow(
                        title = chapterWithSections.chapter.title.get(language),
                        style = MaterialTheme.typography.headlineSmall,
                        status = chapterStatus
                    )
                }
                chapterWithSections.sections.forEach { sectionWithLessons ->
                    val sectionLessonIds = sectionWithLessons.lessons.map { it.id }
                    val sectionStatus = aggregateStatus(sectionLessonIds, uiState.progressByLessonId)

                    item(key = "section_${sectionWithLessons.section.id}") {
                        RoadmapHeaderRow(
                            title = sectionWithLessons.section.title.get(language),
                            style = MaterialTheme.typography.titleMedium,
                            indent = 16.dp,
                            status = sectionStatus
                        )
                    }
                    itemsIndexed(sectionWithLessons.lessons, key = { _, lesson -> lesson.id }) { _, lesson ->
                        val progress = uiState.progressByLessonId[lesson.id]
                        val status = progress?.status ?: LessonStatus.LOCKED
                        val isCurrent = lesson.id == uiState.currentLessonId
                        RoadmapLessonRow(
                            title = lesson.title.get(language),
                            kind = lesson.kind,
                            status = status,
                            scorePercent = progress?.bestScorePercent,
                            isCurrent = isCurrent,
                            onClick = { if (isRoadmapReachable(status)) onOpenLesson(lesson.id) },
                            indent = 32.dp
                        )
                    }
                }
                itemsIndexed(chapterWithSections.chapterLevelLessons, key = { _, lesson -> lesson.id }) { _, lesson ->
                    val progress = uiState.progressByLessonId[lesson.id]
                    val status = progress?.status ?: LessonStatus.LOCKED
                    val isCurrent = lesson.id == uiState.currentLessonId
                    RoadmapLessonRow(
                        title = lesson.title.get(language),
                        kind = lesson.kind,
                        status = status,
                        scorePercent = progress?.bestScorePercent,
                        isCurrent = isCurrent,
                        onClick = { if (isRoadmapReachable(status)) onOpenLesson(lesson.id) },
                        indent = 16.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoadmapHeaderRow(
    title: String,
    style: androidx.compose.ui.text.TextStyle,
    indent: Dp = 0.dp,
    status: LessonStatus? = null
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent, top = 12.dp, bottom = 4.dp)
    ) {
        Text(
            title,
            style = style,
            modifier = Modifier.weight(1f, fill = false)
        )
        if (status == LessonStatus.COMPLETED) {
            Icon(
                Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun RoadmapLessonRow(
    title: String,
    kind: LessonKind,
    status: LessonStatus,
    scorePercent: Int?,
    onClick: () -> Unit,
    indent: Dp,
    isCurrent: Boolean = false
) {
    val language = rememberSelectedLanguage()
    val reachable = isRoadmapReachable(status)
    val kindVisual = rememberLessonKindVisual(kind)
    val (defaultIcon, defaultTint) = statusDefaultIconAndTint(status, isCurrent = isCurrent)

    val icon = when {
        status == LessonStatus.COMPLETED -> Icons.Filled.CheckCircle
        isCurrent -> Icons.Filled.PlayArrow
        status == LessonStatus.LOCKED -> if (kindVisual.isQuizOrExam) kindVisual.icon else Icons.Filled.Lock
        else -> kindVisual.icon
    }

    val iconTint = if (status == LessonStatus.COMPLETED) {
        MaterialTheme.colorScheme.onPrimary
    } else if (isCurrent) {
        MaterialTheme.colorScheme.tertiary
    } else if (reachable) {
        if (kindVisual.isQuizOrExam) kindVisual.accentColor else defaultTint
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    val containerColor = when {
        status == LessonStatus.COMPLETED -> statusContainerColor(status)
        isCurrent -> MaterialTheme.colorScheme.primaryContainer
        reachable -> statusContainerColor(status)
        kindVisual.isQuizOrExam -> kindVisual.containerColor.copy(alpha = 0.4f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    val shape = RoundedCornerShape(14.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .then(
                if (isCurrent) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, shape)
                } else if (kindVisual.isQuizOrExam) {
                    Modifier.border(
                        1.dp,
                        kindVisual.accentColor.copy(alpha = if (reachable) 0.6f else 0.25f),
                        shape
                    )
                } else Modifier
            )
            .clickable(enabled = reachable, onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        if (kindVisual.isQuizOrExam) {
                            kindVisual.accentColor.copy(alpha = 0.18f)
                        } else if (reachable) {
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.25f)
                        } else {
                            androidx.compose.ui.graphics.Color.Transparent
                        }
                    )
                    .padding(6.dp)
            ) {
                Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (kindVisual.isQuizOrExam) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(kindVisual.accentColor.copy(alpha = 0.18f))
                            .padding(horizontal = 5.dp, vertical = 1.5.dp)
                    ) {
                        Icon(
                            kindVisual.icon,
                            contentDescription = null,
                            tint = kindVisual.accentColor,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            stringResource(kindVisual.labelResId),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = kindVisual.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.size(2.dp))
                }

                Text(
                    title,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = if (isCurrent || kindVisual.isQuizOrExam) FontWeight.SemiBold else FontWeight.Normal
                    ),
                    color = if (reachable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            if (status == LessonStatus.COMPLETED && scorePercent != null) {
                Text(
                    VerseReferenceFormatter.formatDigits("$scorePercent%", language),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = if (kindVisual.isQuizOrExam) kindVisual.accentColor else MaterialTheme.colorScheme.primary
                )
            } else if (isCurrent) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
