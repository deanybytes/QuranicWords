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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
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
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Style
import com.quranicwords.app.core.domain.model.LemmaCategory
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
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
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.QuranStarfieldMotif
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.StreakLockedChip
import com.quranicwords.app.core.ui.components.StreakLockedDialog
import com.quranicwords.app.core.ui.components.charts.DonutChart
import com.quranicwords.app.core.ui.components.charts.Home30DayActivityTrendChart
import com.quranicwords.app.core.ui.components.statusContainerColor
import com.quranicwords.app.core.ui.components.statusDefaultIconAndTint
import com.quranicwords.app.core.ui.components.rememberLessonKindVisual
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.pressDepth
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
import com.quranicwords.app.core.ui.motion.unlockRevealShimmer
import com.quranicwords.app.core.ui.theme.BrandGold
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
    onOpenLearnedWords: () -> Unit = {},
    /** Hoisted to QwBottomNavShell (not `remember`ed here) so a tab switch away and back doesn't
     * lose the learner's manual collapse/expand choices - see that composable's doc comment. */
    expandedChapterIds: Set<String>?,
    onExpandedChapterIdsChange: (Set<String>?) -> Unit,
    expandedSectionIds: Set<String>?,
    onExpandedSectionIdsChange: (Set<String>?) -> Unit,
    /** Reports the "Continue Learning" target up to [com.quranicwords.app.core.navigation
     * .QwBottomNavShell], whose own `Scaffold` now hosts that FAB centered over the bottom nav
     * bar - a Home-tab-specific action, but the bar itself is shared shell chrome, so the shell
     * needs to know when to show it rather than Home rendering its own floating button. */
    onContinueLearningLessonIdChange: (String?) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()
    val listState = rememberLazyListState()

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

    LaunchedEffect(uiState.currentLessonId) {
        onContinueLearningLessonIdChange(uiState.currentLessonId)
    }

    Scaffold(
        // No topBar - the hero header below is a custom panel (gradient/shadow/rounded corners),
        // not a Material TopAppBar, so it can look like a genuine hero surface instead of a flat
        // system bar. contentWindowInsets zeroed out - the outer QwBottomNavShell Scaffold (this
        // screen's only caller) already reserves the status/nav bar insets around this whole tab;
        // without zeroing this Scaffold's own default (`WindowInsets.safeDrawing`) it would reserve
        // the status bar height a *second* time, leaving a blank gap above the header.
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Same gold lattice as the header panel below - brought back per feedback (liked there,
            // wanted here too), plus the Quran scatter unchanged on top.
            GeometricPatternBackground(modifier = Modifier.fillMaxSize(), alpha = 0.1f)
            QuranStarfieldMotif(
                modifier = Modifier.fillMaxSize(),
                count = 26,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
            )

            Column(modifier = Modifier.fillMaxSize()) {
                HomeHeroHeader(
                    totalPoints = uiState.totalPoints,
                    currentStreak = uiState.currentStreak,
                    isStreakLocked = uiState.isStreakLocked,
                    streakRecoveryQuestionCount = uiState.streakRecoveryQuestionCount,
                    isDailyGoalMetToday = uiState.isDailyGoalMetToday,
                    quranCoveragePercent = uiState.quranCoveragePercent,
                    last30DaysMinutes = uiState.last30DaysMinutes,
                    activeDaysCount = uiState.last30DaysActiveCount,
                    totalMinutes = uiState.last30DaysTotalMinutes,
                    onOpenRoadmap = onOpenRoadmap,
                    onOpenStreakRecovery = onOpenStreakRecovery,
                    onOpenLearnedWords = onOpenLearnedWords
                )

                Spacer(modifier = Modifier.height(12.dp))

                // The "central box" - a standalone floating glass panel, gapped from
                // HomeHeroHeader above and the bottom nav bar below (not flush against either),
                // fully rounded on all four corners like the chapter/section/lesson cards inside
                // it. A real drawn shadow (Modifier.shadow, same recipe GlassSurface itself uses)
                // rather than a manually blurred fake-shadow layer - the blur-hack shadow only
                // reads correctly against an *opaque* surface sitting flush against its neighbor
                // (HomeHeroHeader); against this panel's translucent fill it bled straight through
                // the fill and smeared across the seam into the box above.
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    val centralShape = RoundedCornerShape(28.dp)
                    val reducedGlass = rememberReducedGlass()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(centralShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.4f))
                            .then(
                                if (reducedGlass) {
                                    Modifier
                                } else {
                                    Modifier.border(
                                        width = 1.dp,
                                        brush = Brush.linearGradient(
                                            listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                                        ),
                                        shape = centralShape
                                    )
                                }
                            )
                    ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp
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
                            ReviewEntryCard(
                                missedCount = uiState.missedWordsCount,
                                onClick = onOpenReview
                            )
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
                                userCoveragePercent = uiState.userCoveragePercentByChapter[chapter.id] ?: 0.0,
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
                                            category = lesson.category,
                                            progress = uiState.progressByLessonId[lesson.id],
                                            isCurrent = lesson.id == uiState.currentLessonId,
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
                                        category = lesson.category,
                                        progress = uiState.progressByLessonId[lesson.id],
                                        isCurrent = lesson.id == uiState.currentLessonId,
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

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

/**
 * Home's hero header - brand row plus a "current status" strip that pairs the learner's real
 * Qur'an-coverage progress ([HomeUiState.quranCoveragePercent], the same running total the
 * Progress tab's [DonutChart] shows) with the existing points/streak/daily-goal badges, all inside
 * one rounded, gradient, drop-shadowed panel (the "3D box" hero-surface recipe from
 * docs/UI_GUIDELINES.md - layered fake shadow + named elevation). Replaces the old flat
 * TopAppBar-plus-badge-row treatment, which read as two disconnected strips floating over a plain
 * background rather than a single deliberate surface.
 */
@Composable
private fun HomeHeroHeader(
    totalPoints: Int,
    currentStreak: Int,
    isStreakLocked: Boolean,
    streakRecoveryQuestionCount: Int,
    isDailyGoalMetToday: Boolean,
    quranCoveragePercent: Double,
    last30DaysMinutes: List<Int> = emptyList(),
    activeDaysCount: Int = 0,
    totalMinutes: Int = 0,
    onOpenRoadmap: () -> Unit,
    onOpenStreakRecovery: () -> Unit,
    onOpenLearnedWords: () -> Unit = {}
) {
    val shape = RoundedCornerShape(28.dp)

    Box(modifier = Modifier.fillMaxWidth()) {
        // Same glass-edge border highlight as GlassSurface (kept as a raw .border(...) rather than
        // routing this hero surface through GlassSurface itself, since its bespoke gradient fill
        // is a deliberate hero-only treatment worth keeping distinct). No drop shadow - per
        // feedback, shadows are off across every box on this screen.
        val reducedGlass = rememberReducedGlass()
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            MaterialTheme.colorScheme.primaryContainer,
                            MaterialTheme.colorScheme.surfaceVariant
                        )
                    )
                )
                .then(
                    if (reducedGlass) {
                        Modifier
                    } else {
                        Modifier.border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                listOf(Color.White.copy(alpha = 0.28f), Color.Transparent)
                            ),
                            shape = shape
                        )
                    }
                )
        ) {
            Box {
                // Gold lattice + starfield, denser/brighter than the ambient body background -
                // this panel is meant to be looked at, not just sat behind content.
                GeometricPatternBackground(
                    modifier = Modifier.matchParentSize(),
                    color = MaterialTheme.colorScheme.tertiary,
                    alpha = 0.14f,
                    tileSize = 44.dp
                )
                QuranStarfieldMotif(
                    modifier = Modifier.matchParentSize(),
                    count = 10,
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)
                )

                Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        QwLogo(size = 40.dp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            stringResource(R.string.home_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        val roadmapInteractionSource = remember { MutableInteractionSource() }
                        Surface(
                            modifier = Modifier
                                .clickable(
                                    interactionSource = roadmapInteractionSource,
                                    indication = null,
                                    onClick = onOpenRoadmap
                                )
                                .pressDepth(roadmapInteractionSource),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                            shadowElevation = 0.dp
                        ) {
                            Icon(
                                Icons.Filled.Map,
                                contentDescription = stringResource(R.string.roadmap_title),
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClick = onOpenLearnedWords)
                            .padding(vertical = 4.dp)
                    ) {
                        DonutChart(
                            percent = (quranCoveragePercent / 100.0).toFloat(),
                            size = 68.dp,
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.16f),
                            centerLabel = "${formatPercent(quranCoveragePercent)}%",
                            labelStyle = MaterialTheme.typography.labelLarge.copy(
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                stringResource(R.string.progress_quran_coverage),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                PointsBadge(totalPoints)
                                if (isStreakLocked) {
                                    StreakLockedChip(questionCount = streakRecoveryQuestionCount, onClick = onOpenStreakRecovery)
                                } else {
                                    StreakBadge(currentStreak)
                                }
                                DailyGoalBadge(visible = isDailyGoalMetToday)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 30-Day Activity Bar / Trend Chart + Time-Spent Spline Curve
                    Home30DayActivityTrendChart(
                        last30DaysMinutes = last30DaysMinutes,
                        activeDaysCount = activeDaysCount,
                        totalMinutes = totalMinutes
                    )
                }
            }
        }
    }
}

private fun Set<String>.toggled(id: String): Set<String> = if (id in this) this - id else this + id

private fun formatCompletedDate(epochMillis: Long, language: com.quranicwords.app.core.domain.model.Language): String {
    val date = Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
    val locale = when (language) {
        com.quranicwords.app.core.domain.model.Language.BANGLA -> java.util.Locale.forLanguageTag("bn-BD")
        com.quranicwords.app.core.domain.model.Language.URDU -> java.util.Locale.forLanguageTag("ur-PK")
        com.quranicwords.app.core.domain.model.Language.INDONESIAN -> java.util.Locale.forLanguageTag("id-ID")
        com.quranicwords.app.core.domain.model.Language.TURKISH -> java.util.Locale.forLanguageTag("tr-TR")
        com.quranicwords.app.core.domain.model.Language.FRENCH -> java.util.Locale.FRENCH
        com.quranicwords.app.core.domain.model.Language.ENGLISH -> java.util.Locale.ENGLISH
    }
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
    return com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(formatter.format(date), language)
}

/** Collapsed-by-default chapter row - tap the row to expand/collapse, tap the title specifically
 * to open the chapter intro (same split affordance the old flat list used for chapter/section
 * headers). Reuses [statusContainerColor]/[statusDefaultIconAndTint]/[unlockRevealShimmer] so this
 * reads as the same visual family as a lesson node, just one tier up the tree. [isCurrent] marks
 * the chapter containing the learner's actual next lesson with a tertiary (gold) outline - "you
 * are here" at a glance among six-plus collapsed chapter rows. [progress] (0f-1f, completed/total
 * lessons) draws a thin pill bar along the card's bottom edge - skipped while still LOCKED, since
 * "0% of a chapter you can't start yet" isn't a useful signal.
 * [ownCoveragePercent]/[userCoveragePercent] show "this chapter's share of the Qur'an ·
 * user's real covered occurrence percent in this chapter" (see [HomeUiState.userCoveragePercentByChapter]). */
@Composable
private fun ChapterSummaryNode(
    title: String,
    status: LessonStatus,
    progress: Float,
    ownCoveragePercent: Double,
    userCoveragePercent: Double,
    isCurrent: Boolean,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenIntro: () -> Unit
) {
    val language = rememberSelectedLanguage()
    val (icon, iconTint) = statusDefaultIconAndTint(status)
    val shape = MaterialTheme.shapes.medium
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        shape = shape,
        tint = statusContainerColor(status),
        accentBorderColor = if (isCurrent) MaterialTheme.colorScheme.tertiary else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .unlockRevealShimmer(status, MaterialTheme.colorScheme.tertiary),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            GlassStatusBadge(
                icon = icon,
                contentDescription = null,
                tint = iconTint,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                size = 40.dp,
                onClick = null
            )
            Column(modifier = Modifier.weight(1f).clickable(onClick = onOpenIntro)) {
                Text(title, style = MaterialTheme.typography.headlineSmall)
                Text(
                    stringResource(
                        R.string.home_chapter_coverage_stat,
                        com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(formatPercent(ownCoveragePercent), language),
                        com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits(formatPercent(userCoveragePercent), language)
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
    val sectionShape = RoundedCornerShape(12.dp)
    GlassSurface(
        modifier = Modifier.fillMaxWidth(),
        onClick = onToggle,
        shape = sectionShape,
        tint = statusContainerColor(status),
        tonalAlpha = 0.35f,
        accentBorderColor = if (isCurrent) MaterialTheme.colorScheme.tertiary else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp)
                .unlockRevealShimmer(status, MaterialTheme.colorScheme.tertiary)
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassStatusBadge(
                icon = icon,
                contentDescription = null,
                tint = iconTint,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                size = 32.dp,
                onClick = null
            )
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
    category: LemmaCategory = LemmaCategory.NOUN,
    progress: UserProgressEntity?,
    isCurrent: Boolean = false,
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
            category = category,
            progress = progress,
            isCurrent = isCurrent,
            onClick = onClick,
            modifier = Modifier.align(BiasAlignment(horizontalBias = bias, verticalBias = 0f))
        )
    }
}

@Composable
private fun LessonNode(
    title: String,
    kind: LessonKind,
    category: LemmaCategory = LemmaCategory.NOUN,
    progress: UserProgressEntity?,
    isCurrent: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = progress?.status ?: LessonStatus.LOCKED
    val isUnlocked = status != LessonStatus.LOCKED
    val kindVisual = rememberLessonKindVisual(kind)

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
        val (defaultIcon, defaultTint) = statusDefaultIconAndTint(status)
        val categoryAccent = com.quranicwords.app.core.ui.components.categoryAccentColor(category)
        val accentColor = if (kindVisual.isQuizOrExam) kindVisual.accentColor else categoryAccent

        val containerColor = statusContainerColor(status)
        val tint = if (isUnlocked) accentColor else defaultTint

        val icon = when {
            status == LessonStatus.LOCKED -> if (kindVisual.isQuizOrExam) kindVisual.icon else defaultIcon
            isCurrent && status == LessonStatus.UNLOCKED && !kindVisual.isQuizOrExam -> Icons.Filled.PlayArrow
            status == LessonStatus.COMPLETED -> if (kindVisual.isQuizOrExam) kindVisual.icon else Icons.Filled.CheckCircle
            else -> if (kindVisual.isQuizOrExam) kindVisual.icon else defaultIcon
        }

        val badgeAccentBorder = when {
            isCurrent -> MaterialTheme.colorScheme.tertiary
            isUnlocked -> accentColor.copy(alpha = 0.7f)
            else -> null
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .unlockRevealShimmer(status, if (kindVisual.isQuizOrExam) kindVisual.accentColor else MaterialTheme.colorScheme.tertiary),
            contentAlignment = Alignment.Center
        ) {
            GlassStatusBadge(
                icon = icon,
                contentDescription = title,
                tint = tint,
                containerColor = containerColor,
                size = 64.dp,
                onClick = if (isUnlocked) onClick else null,
                accentBorderColor = badgeAccentBorder
            )
        }

        GlassSurface(
            modifier = Modifier.width(170.dp),
            onClick = if (isUnlocked) onClick else null,
            tint = statusContainerColor(status),
            accentBorderColor = when {
                isCurrent -> MaterialTheme.colorScheme.tertiary
                isUnlocked -> accentColor
                else -> null
            },
            accentBorderWidth = if (isCurrent) 2.dp else 1.5.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Quiz / Exam or Noun / Verb / Particle Distinctive Tag
                if (kindVisual.isQuizOrExam) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(kindVisual.accentColor.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            kindVisual.icon,
                            contentDescription = null,
                            tint = kindVisual.accentColor,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            stringResource(kindVisual.labelResId),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = kindVisual.accentColor
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                } else {
                    // Explicit Noun vs Verb vs Particle Badge
                    val categoryIcon = com.quranicwords.app.core.ui.components.categoryIcon(category)
                    val categoryLabelRes = com.quranicwords.app.core.ui.components.categoryLabelRes(category)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryAccent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Icon(
                            categoryIcon,
                            contentDescription = null,
                            tint = categoryAccent,
                            modifier = Modifier.size(11.dp)
                        )
                        Text(
                            stringResource(categoryLabelRes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = categoryAccent
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }

                val labelColor = if (isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    color = labelColor,
                    maxLines = 2
                )
                if (status == LessonStatus.COMPLETED) {
                    Text(
                        "${progress?.bestScorePercent ?: 0}%",
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (kindVisual.isQuizOrExam) kindVisual.accentColor else labelColor
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

/** Shared circular "glass" status badge (play/lock/checkmark/exam icon) - the same [GlassSurface]
 * treatment as the chapter/section/lesson boxes it sits inside, just circular. Centralizes what
 * used to be three near-identical `Box.clip(CircleShape).background(...)` copies (chapter icon,
 * section icon, lesson play/lock circle) into one, so "give every box the glass look" only needed
 * fixing here once. */
@Composable
private fun GlassStatusBadge(
    icon: ImageVector,
    contentDescription: String?,
    tint: Color,
    containerColor: Color,
    size: Dp,
    onClick: (() -> Unit)?,
    accentBorderColor: Color? = null
) {
    GlassSurface(
        modifier = Modifier.size(size),
        onClick = onClick,
        shape = CircleShape,
        tint = containerColor,
        accentBorderColor = accentBorderColor,
        accentBorderWidth = 1.5.dp
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint)
        }
    }
}

/** Entry point into the dynamic Review session (see `LessonViewModel`'s `isReviewSession` path) -
 * only shown when [HomeUiState.hasReviewableItems] is true (i.e. [HomeUiState.missedWordsCount] > 0).
 * Displays a dedicated mistaken words review box with word count badge and quick practice CTA. */
@Composable
private fun ReviewEntryCard(
    missedCount: Int,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(20.dp)
    val reducedGlass = rememberReducedGlass()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pressDepth(interactionSource),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.9f)
        ),
        border = if (reducedGlass) null else androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = BrandGold.copy(alpha = 0.45f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick,
        interactionSource = interactionSource
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(R.string.home_review_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    if (missedCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ) {
                            Text(
                                text = "$missedCount",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (missedCount == 1) {
                        stringResource(R.string.home_review_single_mistake_subtitle)
                    } else if (missedCount > 1) {
                        stringResource(R.string.home_review_count_subtitle, missedCount)
                    } else {
                        stringResource(R.string.home_review_subtitle)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f)
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary
            ) {
                Text(
                    text = stringResource(R.string.home_review_action_button),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
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
        Card(
            modifier = Modifier.fillMaxWidth().pressDepth(interactionSource),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
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
                        formatCompletedDate(progress.completedAtEpochMillis!!, language),
                        com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits("${progress.bestScorePercent}", language),
                        formatDuration(progress.durationMillis, language)
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    stringResource(
                        R.string.home_history_position,
                        com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits("${position + 1}", language),
                        com.quranicwords.app.core.util.VerseReferenceFormatter.formatDigits("$total", language)
                    ),
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
