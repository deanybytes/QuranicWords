package com.quranicwords.app.feature.roadmap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.QwIconButton
import com.quranicwords.app.core.ui.components.statusContainerColor
import com.quranicwords.app.core.ui.components.statusDefaultIconAndTint
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.Elevation

/**
 * Full-curriculum timeline (all chapters/sections/lessons, flat - no collapse/expand, since the
 * whole point here is a bird's-eye view rather than progressive reveal) for jumping straight to
 * any *completed* unit for review. Locked/not-yet-completed rows are visibly greyed and inert -
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
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            uiState.chapters.forEach { chapterWithSections ->
                item(key = "chapter_${chapterWithSections.chapter.id}") {
                    RoadmapHeaderRow(chapterWithSections.chapter.title.get(language), MaterialTheme.typography.headlineSmall)
                }
                chapterWithSections.sections.forEach { sectionWithLessons ->
                    item(key = "section_${sectionWithLessons.section.id}") {
                        RoadmapHeaderRow(sectionWithLessons.section.title.get(language), MaterialTheme.typography.titleMedium, indent = 16.dp)
                    }
                    itemsIndexed(sectionWithLessons.lessons, key = { _, lesson -> lesson.id }) { _, lesson ->
                        val status = uiState.progressByLessonId[lesson.id]?.status ?: LessonStatus.LOCKED
                        RoadmapLessonRow(
                            title = lesson.title.get(language),
                            status = status,
                            onClick = { if (isRoadmapReachable(status)) onOpenLesson(lesson.id) },
                            indent = 32.dp
                        )
                    }
                }
                itemsIndexed(chapterWithSections.chapterLevelLessons, key = { _, lesson -> lesson.id }) { _, lesson ->
                    val status = uiState.progressByLessonId[lesson.id]?.status ?: LessonStatus.LOCKED
                    RoadmapLessonRow(
                        title = lesson.title.get(language),
                        status = status,
                        onClick = { if (isRoadmapReachable(status)) onOpenLesson(lesson.id) },
                        indent = 16.dp
                    )
                }
            }
        }
    }
}

@Composable
private fun RoadmapHeaderRow(title: String, style: androidx.compose.ui.text.TextStyle, indent: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        title,
        style = style,
        modifier = Modifier.fillMaxWidth().padding(start = indent, top = 12.dp, bottom = 4.dp)
    )
}

@Composable
private fun RoadmapLessonRow(
    title: String,
    status: LessonStatus,
    onClick: () -> Unit,
    indent: androidx.compose.ui.unit.Dp
) {
    val reachable = isRoadmapReachable(status)
    val (icon, iconTint) = statusDefaultIconAndTint(status)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .clickable(enabled = reachable, onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (reachable) statusContainerColor(status) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (reachable) MaterialTheme.colorScheme.surface.copy(alpha = 0.25f) else androidx.compose.ui.graphics.Color.Transparent)
                    .padding(4.dp)
            ) {
                Icon(icon, contentDescription = null, tint = if (reachable) iconTint else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
            }
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (reachable) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}
