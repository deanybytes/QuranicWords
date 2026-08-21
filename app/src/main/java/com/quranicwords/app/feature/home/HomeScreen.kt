package com.quranicwords.app.feature.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.StarfieldMotif
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.statusContainerColor
import com.quranicwords.app.core.ui.components.statusDefaultIconAndTint
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.unlockRevealShimmer
import com.quranicwords.app.core.ui.theme.Elevation
import kotlin.math.sin

/**
 * Collapse/expand branching tree (QW-22), replacing the previous always-flat chapter/section/
 * lesson list. Chapters and sections render as collapsed summary nodes until tapped; only the
 * expanded chapter's sections and the expanded section's lessons are ever inserted as real
 * `LazyColumn` items, so the composed item count at any time stays around 30 (a few chapter/
 * section summaries + one section's ~10 lessons) rather than the full 887-lesson curriculum -
 * `LazyColumn` was already lazy per-item before this change, but a genuinely *connected* graph
 * line across hundreds of items would defeat that; indentation-based nesting (this) keeps it
 * intact with no new windowing code. Auto-expands on load to the chapter/section containing the
 * learner's actual current lesson (see [HomeUiState.initiallyExpandedChapterId]/
 * [initiallyExpandedSectionId]) so a first-time visit doesn't require a tap to find "where was I".
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenLesson: (String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenChapterIntro: (String) -> Unit,
    onOpenSectionIntro: (String) -> Unit,
    onOpenWordBrowse: (String) -> Unit,
    onOpenAchievements: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()

    var expandedChapterIds by remember { mutableStateOf<Set<String>?>(null) }
    var expandedSectionIds by remember { mutableStateOf<Set<String>?>(null) }
    // Seeds the expand state from the learner's real current position exactly once, the first
    // time it becomes available - never re-forces expansion afterward, so a manual collapse by
    // the learner sticks even as progress keeps changing underneath.
    LaunchedEffect(uiState.initiallyExpandedChapterId) {
        if (expandedChapterIds == null && uiState.initiallyExpandedChapterId != null) {
            expandedChapterIds = setOfNotNull(uiState.initiallyExpandedChapterId)
            expandedSectionIds = setOfNotNull(uiState.initiallyExpandedSectionId)
        }
    }
    val currentExpandedChapterIds = expandedChapterIds ?: emptySet()
    val currentExpandedSectionIds = expandedSectionIds ?: emptySet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenAchievements) {
                        Icon(Icons.Filled.EmojiEvents, contentDescription = stringResource(R.string.home_open_achievements))
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_open_settings))
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            GeometricPatternBackground(modifier = Modifier.fillMaxSize(), alpha = 0.03f)

            Column(modifier = Modifier.fillMaxSize()) {
                Box {
                    // Very low-alpha starfield behind the status strip - matches the LazyColumn's
                    // own 12dp rhythm below rather than the odd 8dp this row previously used
                    // alone, and reads as a distinct "status strip" above the plain background.
                    StarfieldMotif(
                        modifier = Modifier.fillMaxWidth().height(64.dp),
                        starCount = 8,
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.05f)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PointsBadge(uiState.totalPoints)
                        StreakBadge(uiState.currentStreak)
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.hasReviewableItems) {
                        item(key = "review_entry") {
                            ReviewEntryCard(onClick = onOpenReview)
                        }
                    }

                    uiState.chapters.forEach { chapterWithSections ->
                        val chapter = chapterWithSections.chapter
                        val chapterLessonIds = chapterWithSections.sections.flatMap { s -> s.lessons.map { it.id } } +
                            chapterWithSections.chapterLevelLessons.map { it.id }
                        val chapterStatus = aggregateStatus(chapterLessonIds, uiState.progressByLessonId)
                        val chapterExpanded = chapter.id in currentExpandedChapterIds

                        item(key = "chapter_${chapter.id}") {
                            ChapterSummaryNode(
                                title = chapter.title.get(language),
                                status = chapterStatus,
                                expanded = chapterExpanded,
                                onToggle = {
                                    expandedChapterIds = currentExpandedChapterIds.toggled(chapter.id)
                                },
                                onOpenIntro = { onOpenChapterIntro(chapter.id) }
                            )
                        }

                        if (chapterExpanded) {
                            chapterWithSections.sections.forEach { sectionWithLessons ->
                                val section = sectionWithLessons.section
                                val sectionStatus = aggregateStatus(
                                    sectionWithLessons.lessons.map { it.id },
                                    uiState.progressByLessonId
                                )
                                val sectionExpanded = section.id in currentExpandedSectionIds

                                item(key = "section_${section.id}") {
                                    SectionSummaryNode(
                                        title = section.title.get(language),
                                        status = sectionStatus,
                                        expanded = sectionExpanded,
                                        onToggle = {
                                            expandedSectionIds = currentExpandedSectionIds.toggled(section.id)
                                        },
                                        onOpenIntro = { onOpenSectionIntro(section.id) },
                                        onOpenWordBrowse = { onOpenWordBrowse(section.id) }
                                    )
                                }

                                if (sectionExpanded) {
                                    itemsIndexed(
                                        sectionWithLessons.lessons,
                                        key = { _, lesson -> lesson.id }
                                    ) { index, lesson ->
                                        LessonPathNode(
                                            index = index,
                                            title = lesson.title.get(language),
                                            kind = lesson.kind,
                                            progress = uiState.progressByLessonId[lesson.id],
                                            onClick = { onOpenLesson(lesson.id) },
                                            indent = 24.dp
                                        )
                                    }
                                }
                            }

                            if (chapterWithSections.chapterLevelLessons.isNotEmpty()) {
                                itemsIndexed(
                                    chapterWithSections.chapterLevelLessons,
                                    key = { _, lesson -> lesson.id }
                                ) { index, lesson ->
                                    LessonPathNode(
                                        index = index,
                                        title = lesson.title.get(language),
                                        kind = lesson.kind,
                                        progress = uiState.progressByLessonId[lesson.id],
                                        onClick = { onOpenLesson(lesson.id) },
                                        indent = 0.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id

/** Collapsed-by-default chapter row - tap the row to expand/collapse, tap the title specifically
 * to open the chapter intro (same split affordance the old flat list used for chapter/section
 * headers). Reuses [statusContainerColor]/[unlockRevealShimmer] so this reads as the same visual
 * family as a lesson node, just one tier up the tree. */
@Composable
private fun ChapterSummaryNode(
    title: String,
    status: LessonStatus,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenIntro: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle),
        colors = CardDefaults.cardColors(containerColor = statusContainerColor(status).copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.raised)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .unlockRevealShimmer(status, MaterialTheme.colorScheme.tertiary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f).clickable(onClick = onOpenIntro)
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }
    }
}

/** Collapsed-by-default section row, one indentation tier below its chapter. */
@Composable
private fun SectionSummaryNode(
    title: String,
    status: LessonStatus,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenIntro: () -> Unit,
    onOpenWordBrowse: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
                .clickable(onClick = onToggle)
                .unlockRevealShimmer(status, MaterialTheme.colorScheme.tertiary)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = if (status == LessonStatus.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).clickable(onClick = onOpenIntro)
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
            IconButton(onClick = onOpenWordBrowse) {
                Icon(Icons.Filled.Style, contentDescription = stringResource(R.string.word_browse_title))
            }
        }
        HorizontalDivider(
            modifier = Modifier.padding(start = 20.dp),
            color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
        )
    }
}

/**
 * One node of the winding skill path - offset left/right in a sine wave, with
 * a short dashed spine segment behind it that chains into a continuous-looking line as
 * consecutive nodes scroll into view. Each node is a self-contained lazy list item (see
 * [HomeScreen]) rather than part of one eagerly-composed path. [indent] shifts the whole node
 * right to sit visually "under" its parent section/chapter in the collapse/expand tree.
 */
@Composable
private fun LessonPathNode(
    index: Int,
    title: String,
    kind: LessonKind,
    progress: UserProgressEntity?,
    onClick: () -> Unit,
    indent: Dp = 0.dp
) {
    // Tertiary (gold) accent instead of a neutral Material outline color - ties the path into the
    // "illuminated manuscript" palette rather than reading as generic Material chrome.
    val lineColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
    val bias = sin(index * Math.PI / 3.0).toFloat() * 0.55f

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = indent)
            .height(96.dp)
            .drawBehind {
                val dashEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f), 0f)
                drawLine(
                    color = lineColor,
                    start = Offset(size.width / 2f, 0f),
                    end = Offset(size.width / 2f, size.height),
                    strokeWidth = 4f,
                    pathEffect = dashEffect
                )
            }
    ) {
        LessonNode(
            title = title,
            kind = kind,
            progress = progress,
            onClick = onClick,
            modifier = Modifier.align(BiasAlignment(horizontalBias = bias, verticalBias = 0f))
        )
    }
}

/** Regular lessons use the usual play/lock/check iconography; exam and flashback kinds get a
 * distinct icon so the path visually flags "this one's a checkpoint" before the learner taps in -
 * a simple, functional cue for now, refined further in the full gamified-UI pass. */
private fun kindIcon(kind: LessonKind): ImageVector? = when (kind) {
    LessonKind.REGULAR -> null
    LessonKind.SECTION_EXAM, LessonKind.CHAPTER_EXAM -> Icons.Filled.EmojiEvents
    LessonKind.LESSON_FLASHBACK, LessonKind.SECTION_FLASHBACK, LessonKind.CHAPTER_FLASHBACK -> Icons.Filled.History
}

@Composable
private fun LessonNode(
    title: String,
    kind: LessonKind,
    progress: UserProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = progress?.status ?: LessonStatus.LOCKED
    val isUnlocked = status != LessonStatus.LOCKED

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = MotionSpecs.celebratory(),
        label = "nodeScale"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val containerColor = statusContainerColor(status)
        val (defaultIcon, tint) = statusDefaultIconAndTint(status)
        val icon = if (status == LessonStatus.LOCKED) defaultIcon else (kindIcon(kind) ?: defaultIcon)

        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .unlockRevealShimmer(status, MaterialTheme.colorScheme.tertiary)
                .clip(CircleShape)
                .background(containerColor),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, enabled = isUnlocked) {
                Icon(icon, contentDescription = title, tint = tint)
            }
        }

        Card(
            modifier = Modifier.width(160.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isUnlocked) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isUnlocked) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
                if (status == LessonStatus.COMPLETED) {
                    Text(
                        "${progress?.bestScorePercent ?: 0}%",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                } else if (status == LessonStatus.LOCKED) {
                    Text(
                        stringResource(R.string.home_module_locked),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/** Entry point into the dynamic Review session (see `LessonViewModel`'s `isReviewSession` path) -
 * only shown when [HomeUiState.hasReviewableItems] is true, i.e. the learner has at least one
 * item whose most recent attempt was wrong. The most prominent CTA on this screen, so it gets the
 * full "3D box" hero treatment (see docs/UI_GUIDELINES.md): [Elevation.floating] + press-depth +
 * a layered fake shadow. */
@Composable
private fun ReviewEntryCard(onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.medium

    Box(modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .matchParentSize()
                .offset(x = 3.dp, y = 5.dp)
                .blur(10.dp)
                .background(Color.Black.copy(alpha = 0.18f), shape)
        )
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .pressDepth(interactionSource),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating),
            onClick = onClick,
            interactionSource = interactionSource
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(Icons.Filled.Refresh, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                Column {
                    Text(
                        stringResource(R.string.home_review_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        stringResource(R.string.home_review_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}
