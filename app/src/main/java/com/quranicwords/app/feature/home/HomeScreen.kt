package com.quranicwords.app.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonEntity
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.CelebrationBurst
import com.quranicwords.app.core.ui.components.CelebrationIntensity
import com.quranicwords.app.core.ui.components.DailyGoalBadge
import com.quranicwords.app.core.ui.components.GeometricPatternBackground
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.StarfieldMotif
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.StreakLockedChip
import com.quranicwords.app.core.ui.components.StreakLockedDialog
import com.quranicwords.app.core.ui.components.statusContainerColor
import com.quranicwords.app.core.ui.components.statusDefaultIconAndTint
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.unlockRevealShimmer
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.core.util.formatDuration
import com.quranicwords.app.core.util.formatPercent
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.roundToInt
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
    onOpenChapterIntro: (String) -> Unit,
    onOpenSectionIntro: (String) -> Unit,
    onOpenWordBrowse: (String) -> Unit,
    onOpenRoadmap: () -> Unit,
    onOpenOpenPractice: () -> Unit,
    onOpenStreakRecovery: () -> Unit,
    /** Hoisted to QwBottomNavShell (not `remember`ed here) so a tab switch away and back doesn't
     * lose the learner's manual collapse/expand choices - see that composable's doc comment. */
    expandedChapterIds: Set<String>?,
    onExpandedChapterIdsChange: (Set<String>?) -> Unit,
    expandedSectionIds: Set<String>?,
    onExpandedSectionIdsChange: (Set<String>?) -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Which entry of uiState.completedHistory (most-recent-first) the swipeable history card is
    // currently showing - rememberSaveable so it survives tab-switch-away-and-back like the
    // expand state above, not reset every recomposition. Clamped at read time (not written back)
    // so a newly-completed lesson elsewhere doesn't yank the learner's browse position.
    var historyIndex by rememberSaveable { mutableIntStateOf(0) }
    val clampedHistoryIndex = historyIndex.coerceIn(0, (uiState.completedHistory.size - 1).coerceAtLeast(0))

    // Dialog auto-shows once per lock episode (reset by the recovery flow, since a successful
    // recovery flips isStreakLocked back to false) - rememberSaveable so a config change or
    // tab-switch-away-and-back while it's dismissed doesn't pop it right back up.
    var streakLockedDialogDismissed by rememberSaveable(uiState.isStreakLocked) { mutableStateOf(false) }
    if (uiState.isStreakLocked && !streakLockedDialogDismissed) {
        StreakLockedDialog(
            inactivityDuration = uiState.streakInactivityDuration,
            onTakeTest = {
                streakLockedDialogDismissed = true
                onOpenStreakRecovery()
            },
            onDismiss = { streakLockedDialogDismissed = true }
        )
    }

    // Seeds the expand state from the learner's real current position exactly once, the first
    // time it becomes available - never re-forces expansion afterward, so a manual collapse by
    // the learner sticks even as progress keeps changing underneath.
    LaunchedEffect(uiState.initiallyExpandedChapterId) {
        if (expandedChapterIds == null && uiState.initiallyExpandedChapterId != null) {
            onExpandedChapterIdsChange(setOfNotNull(uiState.initiallyExpandedChapterId))
            onExpandedSectionIdsChange(setOfNotNull(uiState.initiallyExpandedSectionId))
        }
    }
    val currentExpandedChapterIds = expandedChapterIds ?: emptySet()
    val currentExpandedSectionIds = expandedSectionIds ?: emptySet()

    // "Continue Learning" FAB: re-expands (only) the chapter/section containing the learner's
    // current lesson and scrolls to that chapter's card - for when they've collapsed/scrolled
    // elsewhere and want back to "where I am" without hunting through the tree. Scrolls to the
    // chapter card, not the exact lesson node - the chapter auto-expands its current section too,
    // which is short enough (~10 lessons) to spot the highlighted current lesson without further
    // scrolling, without needing to compute an exact lesson-level LazyColumn index.
    fun jumpToCurrentPosition() {
        val currentChapterId = uiState.initiallyExpandedChapterId ?: return
        onExpandedChapterIdsChange(setOf(currentChapterId))
        onExpandedSectionIdsChange(setOfNotNull(uiState.initiallyExpandedSectionId))
        val chapterIndex = uiState.chapters.indexOfFirst { it.chapter.id == currentChapterId }
        if (chapterIndex < 0) return
        val itemIndex = chapterIndex + (if (uiState.hasReviewableItems) 1 else 0)
        coroutineScope.launch { listState.animateScrollToItem(itemIndex) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenRoadmap) {
                        Icon(Icons.Filled.Map, contentDescription = stringResource(R.string.roadmap_title))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.initiallyExpandedChapterId != null) {
                ExtendedFloatingActionButton(
                    onClick = ::jumpToCurrentPosition,
                    icon = { Icon(Icons.Filled.MyLocation, contentDescription = null) },
                    text = { Text(stringResource(R.string.home_continue_learning)) }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            // Raised from the original 0.03f/status-strip-only treatment - the old combination
            // read as a flat, empty page. Both layers stay low-alpha and purely ambient (never
            // competing with card/path content), now drawn in the brand palette from Color.kt.
            GeometricPatternBackground(modifier = Modifier.fillMaxSize(), alpha = 0.08f)
            StarfieldMotif(
                modifier = Modifier.fillMaxSize(),
                starCount = 24,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.04f)
            )

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
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PointsBadge(uiState.totalPoints)
                        if (uiState.isStreakLocked) {
                            StreakLockedChip(questionCount = uiState.streakRecoveryQuestionCount, onClick = onOpenStreakRecovery)
                        } else {
                            StreakBadge(uiState.currentStreak)
                        }
                        DailyGoalBadge(visible = uiState.isDailyGoalMetToday)
                    }
                }

                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    // Extra bottom padding when the "Continue Learning" FAB is showing, so it
                    // doesn't permanently cover the last visible card.
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = if (uiState.initiallyExpandedChapterId != null) 96.dp else 16.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.isCurriculumComplete) {
                        item(key = "curriculum_complete") {
                            CurriculumCompleteCard(onClick = onOpenOpenPractice)
                        }
                    }

                    if (uiState.hasReviewableItems) {
                        item(key = "review_entry") {
                            ReviewEntryCard(onClick = onOpenReview)
                        }
                    }

                    if (uiState.completedHistory.isNotEmpty()) {
                        item(key = "history_card") {
                            val (lesson, progress) = uiState.completedHistory[clampedHistoryIndex]
                            HistoryCard(
                                lesson = lesson,
                                progress = progress,
                                language = language,
                                position = clampedHistoryIndex,
                                total = uiState.completedHistory.size,
                                onPrevious = {
                                    historyIndex = stepHistoryIndex(clampedHistoryIndex, uiState.completedHistory.size, -1)
                                },
                                onNext = {
                                    historyIndex = stepHistoryIndex(clampedHistoryIndex, uiState.completedHistory.size, 1)
                                }
                            )
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
                                progress = progressFraction(chapterLessonIds, uiState.progressByLessonId),
                                ownCoveragePercent = chapter.quranOccurrencePercent,
                                cumulativeCoveragePercent = uiState.cumulativeCoveragePercentByChapter[chapter.id] ?: 0.0,
                                isCurrent = chapter.id == uiState.initiallyExpandedChapterId,
                                expanded = chapterExpanded,
                                onToggle = {
                                    onExpandedChapterIdsChange(currentExpandedChapterIds.toggled(chapter.id))
                                },
                                onOpenIntro = { onOpenChapterIntro(chapter.id) }
                            )
                        }

                        if (chapterExpanded) {
                            chapterWithSections.sections.forEach { sectionWithLessons ->
                                val section = sectionWithLessons.section
                                val sectionLessonIds = sectionWithLessons.lessons.map { it.id }
                                val sectionStatus = aggregateStatus(sectionLessonIds, uiState.progressByLessonId)
                                val sectionExpanded = section.id in currentExpandedSectionIds

                                item(key = "section_${section.id}") {
                                    SectionSummaryNode(
                                        title = section.title.get(language),
                                        status = sectionStatus,
                                        progress = progressFraction(sectionLessonIds, uiState.progressByLessonId),
                                        isCurrent = section.id == uiState.initiallyExpandedSectionId,
                                        expanded = sectionExpanded,
                                        onToggle = {
                                            onExpandedSectionIdsChange(currentExpandedSectionIds.toggled(section.id))
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

private val historyDateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

private fun formatCompletedDate(epochMillis: Long): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    return historyDateFormatter.format(date)
}

/** Collapsed-by-default chapter row - tap the row to expand/collapse, tap the title specifically
 * to open the chapter intro (same split affordance the old flat list used for chapter/section
 * headers). Reuses [statusContainerColor]/[statusDefaultIconAndTint]/[unlockRevealShimmer] so this
 * reads as the same visual family as a lesson node, just one tier up the tree. [isCurrent] marks
 * the chapter containing the learner's actual next lesson with a tertiary (gold) outline - "you
 * are here" at a glance among six-plus collapsed chapter rows. [progress] (0f-1f, completed/total
 * lessons) draws a thin pill bar along the card's bottom edge - skipped while still LOCKED, since
 * "0% of a chapter you can't start yet" isn't a useful signal.
 *
 * [ownCoveragePercent]/[cumulativeCoveragePercent] show "this chapter's share of the Qur'an ·
 * running total through this chapter" - not word count, since every chapter has the same word
 * count (460) in this curriculum, so repeating that constant on all eight cards would carry no
 * information (see [HomeUiState.cumulativeCoveragePercentByChapter]'s doc comment). */
@Composable
private fun ChapterSummaryNode(
    title: String,
    status: LessonStatus,
    progress: Float,
    ownCoveragePercent: Double,
    cumulativeCoveragePercent: Double,
    isCurrent: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenIntro: () -> Unit
) {
    val (icon, iconTint) = statusDefaultIconAndTint(status)
    val shape = MaterialTheme.shapes.medium
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle)
            .then(
                if (isCurrent) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, shape)
                } else {
                    Modifier
                }
            ),
        shape = shape,
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
            Icon(icon, contentDescription = null, tint = iconTint)
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenIntro)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(
                        R.string.home_chapter_coverage_stat,
                        formatPercent(ownCoveragePercent),
                        formatPercent(cumulativeCoveragePercent)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
        }
        if (status != LessonStatus.LOCKED) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 10.dp)
                    .clip(RoundedCornerShape(50))
                    .height(4.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

/** Collapsed-by-default section row, one indentation tier below its chapter. Same status-icon/
 * current-outline/progress-bar treatment as [ChapterSummaryNode], one visual step down. */
@Composable
private fun SectionSummaryNode(
    title: String,
    status: LessonStatus,
    progress: Float,
    isCurrent: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenIntro: () -> Unit,
    onOpenWordBrowse: () -> Unit
) {
    val (icon, iconTint) = statusDefaultIconAndTint(status)
    Column(
        modifier = if (isCurrent) {
            Modifier.border(2.dp, MaterialTheme.colorScheme.tertiary, RoundedCornerShape(12.dp))
        } else {
            Modifier
        }
    ) {
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
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleLarge,
                color = if (status == LessonStatus.LOCKED) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 8.dp).clickable(onClick = onOpenIntro)
            )
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null
            )
            IconButton(onClick = onOpenWordBrowse) {
                Icon(Icons.Filled.Style, contentDescription = stringResource(R.string.word_browse_title))
            }
        }
        if (status != LessonStatus.LOCKED) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 16.dp)
                    .padding(bottom = 6.dp)
                    .clip(RoundedCornerShape(50))
                    .height(3.dp),
                color = MaterialTheme.colorScheme.tertiary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
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

/**
 * Shown once the whole curriculum is finished (see [HomeUiState.isCurriculumComplete]) - the "no
 * dead end" answer: instead of the chapter list just running out with nothing new to do, a
 * celebratory card offers to keep practicing via `Route.OpenPractice`'s unbounded random-word
 * quiz. Reuses the same "3D box" hero treatment and [CelebrationBurst] vocabulary as a passed
 * lesson/exam, since finishing the entire curriculum is a bigger milestone than either of those.
 * The chapter list still renders below this unchanged - this is additive, not a replacement, so
 * a completed learner can still revisit any chapter via Roadmap. */
@Composable
private fun CurriculumCompleteCard(onClick: () -> Unit) {
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
            modifier = Modifier.fillMaxWidth().pressDepth(interactionSource),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = Elevation.floating),
            onClick = onClick,
            interactionSource = interactionSource
        ) {
            Box {
                CelebrationBurst(intensity = CelebrationIntensity.PERFECT, modifier = Modifier.fillMaxSize())
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Column {
                        Text(
                            stringResource(R.string.home_curriculum_complete_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            stringResource(R.string.home_curriculum_complete_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

/**
 * A lightweight "history peek" card (Task 5.3) - horizontally draggable to browse the learner's
 * own completed lessons, most-recently-completed first, without leaving Home. Deliberately not
 * free navigation (that's [com.quranicwords.app.feature.roadmap.RoadmapScreen]'s job): it can
 * only page through lessons the learner has actually already finished, one at a time, and never
 * navigates anywhere on tap.
 *
 * Drag tracked via a plain [Animatable] offset rather than [uiState] itself, so the finger-follow
 * feels immediate; [onPrevious]/[onNext] only fire once a full [historyStepThreshold] has been
 * dragged, then the offset springs back to zero with [MotionSpecs.snappy]. The chevron buttons are
 * the same step, exposed for anyone who can't perform the drag gesture (accessibility, and simply
 * easier to discover than an undiscoverable swipe-only affordance).
 */
@Composable
private fun HistoryCard(
    lesson: LessonEntity,
    progress: UserProgressEntity,
    language: Language,
    position: Int,
    total: Int,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val thresholdPx = with(density) { historyStepThreshold.toPx() }
    val canGoPrevious = position > 0
    val canGoNext = position < total - 1

    // Snaps the drag offset back to zero whenever the displayed lesson changes (including via the
    // chevron buttons, not just a drag) so a fast tap-tap-tap never leaves a stale offset behind.
    LaunchedEffect(lesson.id) { offsetX.snapTo(0f) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    coroutineScope.launch { offsetX.snapTo(offsetX.value + delta) }
                },
                onDragStopped = {
                    val dragged = offsetX.value
                    if (dragged <= -thresholdPx && canGoNext) {
                        onNext()
                    } else if (dragged >= thresholdPx && canGoPrevious) {
                        onPrevious()
                    } else {
                        coroutineScope.launch { offsetX.animateTo(0f, animationSpec = MotionSpecs.snappy()) }
                    }
                }
            ),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onPrevious, enabled = canGoPrevious) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = stringResource(R.string.home_history_previous))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.home_history_title),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(lesson.title.get(language), style = MaterialTheme.typography.titleMedium, maxLines = 1)
                Text(
                    stringResource(
                        R.string.home_history_summary,
                        formatCompletedDate(progress.completedAtEpochMillis!!),
                        progress.bestScorePercent,
                        formatDuration(progress.durationMillis)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(R.string.home_history_position, position + 1, total),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onNext, enabled = canGoNext) {
                Icon(Icons.Filled.ChevronRight, contentDescription = stringResource(R.string.home_history_next))
            }
        }
    }
}

private val historyStepThreshold = 56.dp
