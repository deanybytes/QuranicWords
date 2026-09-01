package com.quranicwords.app.feature.lessonsummary

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonKind
import com.quranicwords.app.core.domain.AchievementCatalog
import com.quranicwords.app.core.domain.model.LemmaCategory
import com.quranicwords.app.core.domain.model.LessonSessionType
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.domain.requiresPassingScore
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.AchievementBadge
import com.quranicwords.app.core.ui.components.CelebrationBurst
import com.quranicwords.app.core.ui.components.CelebrationIntensity
import com.quranicwords.app.core.ui.components.CrescentMoonMotif
import com.quranicwords.app.core.ui.components.GlassSurface
import com.quranicwords.app.core.ui.components.MosqueSilhouetteMotif
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.Qw3DFlipCard
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.StreakFlame
import com.quranicwords.app.core.ui.components.displayName
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.BrandGreen
import com.quranicwords.app.core.ui.theme.LocalQuranFontFamily
import com.quranicwords.app.core.ui.theme.MedallionShapeDefault
import com.quranicwords.app.core.ui.theme.QuranCitationFontFamily
import com.quranicwords.app.core.util.GamificationConfig
import com.quranicwords.app.core.util.VerseReferenceFormatter

@Composable
fun LessonSummaryScreen(
    route: Route.LessonSummary,
    onContinue: () -> Unit,
    onBackHome: () -> Unit = onContinue,
    viewModel: LessonSummaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val language = rememberSelectedLanguage()
    val scrollState = rememberScrollState()

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { visible = true }

    val animatedPointsEarned by animateIntAsState(
        targetValue = if (visible) route.pointsAwarded else 0,
        animationSpec = MotionSpecs.countUp,
        label = "pointsEarned"
    )

    val isStreakRecoverySession = route.sessionType == LessonSessionType.STREAK_RECOVERY
    val requiresPassing = !isStreakRecoverySession && (route.lessonKind?.requiresPassingScore() ?: false)
    val passed = if (isStreakRecoverySession) route.streakIncreased
    else !requiresPassing || route.accuracyPercent >= GamificationConfig.PASSING_SCORE_PERCENT

    val celebrationIntensity = if (route.accuracyPercent >= 100) CelebrationIntensity.PERFECT else CelebrationIntensity.PASSED
    val newlyUnlockedAchievements = remember(route.newlyUnlockedAchievementIds) {
        route.newlyUnlockedAchievementIds.mapNotNull { AchievementCatalog.byId[it] }
    }

    val isExamPass = passed && (route.lessonKind == LessonKind.SECTION_EXAM || route.lessonKind == LessonKind.CHAPTER_EXAM)
    val mistakeCount = (route.totalCount - route.correctCount).coerceAtLeast(0)

    val wordsCount = if (uiState.wordsCoveredCount > 0) uiState.wordsCoveredCount else route.totalCount
    val localizedWordsCovered = VerseReferenceFormatter.formatDigits(wordsCount.toString(), language)
    val localizedAccuracy = VerseReferenceFormatter.formatDigits("${route.accuracyPercent}%", language)
    val localizedMistakes = VerseReferenceFormatter.formatDigits(mistakeCount.toString(), language)
    val localizedTotalLearned = VerseReferenceFormatter.formatDigits(uiState.totalWordsLearned.toString(), language)
    val localizedCoveragePercent = VerseReferenceFormatter.formatDigits(String.format("%.1f%%", uiState.quranCoveragePercent), language)

    Box(modifier = Modifier.fillMaxSize()) {
        if (isExamPass) {
            MosqueSilhouetteMotif(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize()
                    .padding(32.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f)
            )
        }
        if (route.streakIncreased) {
            CrescentMoonMotif(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(36.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
            )
        }

        if (visible && passed) {
            CelebrationBurst(intensity = celebrationIntensity, modifier = Modifier.fillMaxSize())
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // 1. Hero Title & Gratitude Header
            StaggeredEntrance(index = 0) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = stringResource(R.string.lesson_summary_alhamdulillah_title),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFamily = QuranCitationFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() },
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (uiState.lessonTitle != null) {
                        Text(
                            text = uiState.lessonTitle?.get(language) ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // 2. 3D Badges (Points & Streak)
            StaggeredEntrance(index = 1) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Qw3DFlipCard(
                        flipped = visible,
                        front = { BadgeMedallionPlaceholder() },
                        back = { PointsBadge(route.newTotalPoints) }
                    )
                    Qw3DFlipCard(
                        flipped = visible,
                        front = { BadgeMedallionPlaceholder() },
                        back = { StreakBadge(route.currentStreak) }
                    )
                }
            }

            // 3. Performance Metrics Grid (Words Covered, Accuracy, Mistakes, Points)
            StaggeredEntrance(index = 2) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricCard(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            title = stringResource(R.string.lesson_summary_words_covered, wordsCount),
                            value = localizedWordsCovered,
                            accentColor = BrandGreen,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            icon = Icons.Filled.CheckCircle,
                            title = stringResource(R.string.lesson_summary_accuracy, route.accuracyPercent),
                            value = localizedAccuracy,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SummaryMetricCard(
                            icon = if (mistakeCount == 0) Icons.Filled.Stars else Icons.Filled.ErrorOutline,
                            title = if (mistakeCount == 0) stringResource(R.string.lesson_summary_flawless) else stringResource(R.string.lesson_summary_mistakes_count, mistakeCount),
                            value = if (mistakeCount == 0) "0" else localizedMistakes,
                            accentColor = if (mistakeCount == 0) BrandGold else MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        SummaryMetricCard(
                            icon = Icons.AutoMirrored.Filled.TrendingUp,
                            title = stringResource(R.string.lesson_summary_points_earned, animatedPointsEarned),
                            value = "+$animatedPointsEarned",
                            accentColor = BrandGold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 4. Words Mastered in This Lesson
            if (uiState.wordsCoveredList.isNotEmpty()) {
                StaggeredEntrance(index = 3) {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        tint = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lesson_summary_words_in_this_lesson),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(uiState.wordsCoveredList) { word ->
                                    val categoryColor = when (word.category) {
                                        LemmaCategory.VERB -> BrandGold
                                        LemmaCategory.PARTICLE -> Color(0xFF0288D1)
                                        else -> BrandGreen
                                    }

                                    GlassSurface(
                                        shape = RoundedCornerShape(12.dp),
                                        tint = MaterialTheme.colorScheme.surface
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = word.arabicWord,
                                                fontFamily = LocalQuranFontFamily.current,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = word.meaning.get(language),
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(categoryColor.copy(alpha = 0.15f))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = when (word.category) {
                                                        LemmaCategory.VERB -> stringResource(R.string.word_category_verb)
                                                        LemmaCategory.PARTICLE -> stringResource(R.string.word_category_particle)
                                                        else -> stringResource(R.string.word_category_noun)
                                                    },
                                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                                                    color = categoryColor
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

            // 5. Cumulative Progress Milestone Card
            StaggeredEntrance(index = 4) {
                GlassSurface(
                    modifier = Modifier.fillMaxWidth(),
                    tint = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.lesson_summary_cumulative_title),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = localizedCoveragePercent,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = BrandGold
                            )
                        }

                        val progressAnim by animateFloatAsState(
                            targetValue = if (visible) (uiState.quranCoveragePercent / 100f).toFloat().coerceIn(0f, 1f) else 0f,
                            animationSpec = MotionSpecs.gentle(),
                            label = "cumProgress"
                        )

                        LinearProgressIndicator(
                            progress = { progressAnim },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = BrandGold,
                            trackColor = MaterialTheme.colorScheme.surface
                        )

                        Text(
                            text = stringResource(
                                R.string.lesson_summary_cumulative_desc,
                                localizedTotalLearned,
                                localizedCoveragePercent
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 6. Next Lesson Preview Card
            if (uiState.nextLessonId != null) {
                StaggeredEntrance(index = 5) {
                    GlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        tint = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
                        accentBorderColor = BrandGold.copy(alpha = 0.6f)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = stringResource(R.string.lesson_summary_next_lesson_title),
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                if (uiState.nextLessonCategory != null) {
                                    val nextCatColor = when (uiState.nextLessonCategory) {
                                        LemmaCategory.VERB -> BrandGold
                                        LemmaCategory.PARTICLE -> Color(0xFF0288D1)
                                        else -> BrandGreen
                                    }
                                    val nextCatIcon = when (uiState.nextLessonCategory) {
                                        LemmaCategory.VERB -> Icons.Filled.FlashOn
                                        LemmaCategory.PARTICLE -> Icons.Filled.AutoAwesome
                                        else -> Icons.Filled.AutoStories
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(nextCatColor.copy(alpha = 0.15f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Icon(nextCatIcon, contentDescription = null, tint = nextCatColor, modifier = Modifier.size(12.dp))
                                        Text(
                                            text = when (uiState.nextLessonCategory) {
                                                LemmaCategory.VERB -> stringResource(R.string.lesson_category_verb)
                                                LemmaCategory.PARTICLE -> stringResource(R.string.lesson_category_particle)
                                                else -> stringResource(R.string.lesson_category_noun)
                                            },
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                            color = nextCatColor
                                        )
                                    }
                                }
                            }

                            if (uiState.nextLessonTitle != null) {
                                Text(
                                    text = uiState.nextLessonTitle?.get(language) ?: "",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (uiState.nextLessonWordCount > 0) {
                                Text(
                                    text = stringResource(R.string.lesson_summary_next_words_count, uiState.nextLessonWordCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }
            }

            // Streak Increased Banner / Achievements
            if (route.streakIncreased) {
                AnimatedVisibility(visible = visible, enter = fadeIn() + scaleIn(initialScale = 0.7f)) {
                    Row(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        StreakFlame(streakDays = route.currentStreak)
                        Text(
                            stringResource(R.string.lesson_summary_streak_increased),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            if (newlyUnlockedAchievements.isNotEmpty()) {
                StaggeredEntrance(index = 6) {
                    Column(
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive },
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            stringResource(R.string.lesson_summary_achievement_unlocked),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            items(newlyUnlockedAchievements) { achievement ->
                                Qw3DFlipCard(
                                    flipped = visible,
                                    front = { BadgeMedallionPlaceholder() },
                                    back = {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            AchievementBadge(motifKind = achievement.motifKind, unlocked = true)
                                            Text(achievement.displayName(), style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action Buttons (Proceed to Next Lesson & Return Home)
            StaggeredEntrance(index = 7) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QwPrimaryButton(
                        text = stringResource(
                            when {
                                route.sessionType == LessonSessionType.OPEN_PRACTICE -> R.string.lesson_summary_keep_practicing
                                route.nextLessonId != null -> R.string.lesson_summary_next_lesson_proceed
                                else -> R.string.lesson_summary_back_to_dashboard
                            }
                        ),
                        onClick = onContinue,
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (route.nextLessonId != null) {
                        QwSecondaryButton(
                            text = stringResource(R.string.lesson_summary_back_to_dashboard),
                            onClick = onBackHome,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryMetricCard(
    icon: ImageVector,
    title: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    GlassSurface(
        modifier = modifier,
        tint = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(18.dp)
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BadgeMedallionPlaceholder() {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = MedallionShapeDefault,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {}
}

private val Route.LessonSummary.accuracyPercent: Int
    get() = GamificationConfig.percentOf(correctCount, totalCount)
