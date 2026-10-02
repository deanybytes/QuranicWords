package com.quranicwords.app.feature.lesson

import com.quranicwords.app.core.domain.model.practicedItemId
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.cleanArabicDisplay
import com.quranicwords.app.core.domain.model.localizedLabel
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.AnswerFeedbackOverlay
import com.quranicwords.app.core.ui.components.FeedbackBanner
import com.quranicwords.app.core.ui.components.FeedbackType
import com.quranicwords.app.core.ui.components.LocalWordCategories
import com.quranicwords.app.core.ui.components.QwIconButton
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.chapterLabel
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberQwHaptics
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.feature.lesson.exercise.ChapterIntroExerciseContent
import com.quranicwords.app.feature.lesson.exercise.FillInTheBlankExerciseContent
import com.quranicwords.app.feature.lesson.exercise.MatchingExerciseContent
import com.quranicwords.app.feature.lesson.exercise.MultipleChoiceExerciseContent
import com.quranicwords.app.feature.lesson.exercise.TapWhatYouHearExerciseContent
import com.quranicwords.app.feature.lesson.exercise.TapWordInVerseExerciseContent
import com.quranicwords.app.feature.lesson.exercise.WordIntroExerciseContent
import com.quranicwords.app.feature.lesson.exercise.WordOrderBuilderExerciseContent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LessonScreen(
    onExit: () -> Unit,
    onFinished: (Route.LessonSummary) -> Unit,
    viewModel: LessonViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showExitDialog by remember { mutableStateOf(false) }
    val language = rememberSelectedLanguage()
    val haptics = rememberQwHaptics()

    var selectedMeaningIndex by remember(uiState.currentIndex) { androidx.compose.runtime.mutableIntStateOf(0) }
    var visitedSenses by remember(uiState.currentIndex) { mutableStateOf(setOf(0)) }

    BackHandler(enabled = !uiState.isLoading) {
        showExitDialog = true
    }

    LaunchedEffect(uiState.isChecked, uiState.lastAnswerCorrect) {
        if (uiState.isChecked) {
            when (uiState.lastAnswerCorrect) {
                true -> haptics.onCorrectAnswer()
                false -> haptics.onIncorrectAnswer()
                null -> Unit
            }
        }
    }

    // Mismatched Matching taps vibrate from MatchingExerciseContent's own shake effect - not
    // repeated here, which used to double-buzz every wrong tap.

    // Practice time is counted from the first rendered exercise, and only while the screen is
    // in the foreground (see LessonViewModel's SessionTimer).
    val hasContent = !uiState.isLoading && uiState.contents.isNotEmpty()
    LaunchedEffect(hasContent) {
        if (hasContent) viewModel.onContentShown()
    }
    LifecycleStartEffect(viewModel) {
        viewModel.onResume()
        onStopOrDispose { viewModel.onPause() }
    }

    LaunchedEffect(uiState.isFinished) {
        val result = uiState.result
        if (uiState.isFinished && result != null) {
            onFinished(
                Route.LessonSummary(
                    lessonId = result.lessonId,
                    correctCount = result.correctCount,
                    totalCount = result.totalCount,
                    pointsAwarded = result.pointsAwarded,
                    newTotalPoints = result.newTotalPoints,
                    currentStreak = result.currentStreak,
                    streakIncreased = result.streakIncreased,
                    nextLessonId = result.nextLessonId,
                    lessonKind = result.lessonKind,
                    newlyUnlockedAchievementIds = uiState.newlyUnlockedAchievements.map { it.id },
                    durationMillis = result.durationMillis,
                    sessionType = result.sessionType,
                    openPracticeMode = viewModel.openPracticeMode,
                    practicedWordIds = uiState.contents.flatMap { content ->
                        when (content) {
                            is ExerciseContent.Matching -> content.pairs.mapNotNull { it.wordId }
                            else -> listOfNotNull(content.practicedItemId())
                        }
                    }.distinct()
                )
            )
        }
    }

    CompositionLocalProvider(LocalWordCategories provides uiState.wordCategories) {
        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        modifier = Modifier.shadow(
                            Elevation.raised,
                            RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                        ),
                        title = {
                            val animatedProgress by animateFloatAsState(
                                targetValue = uiState.progressFraction,
                                animationSpec = MotionSpecs.gentle(),
                                label = "lessonProgress"
                            )
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(50))
                                    .height(6.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        },
                        navigationIcon = {
                            QwIconButton(onClick = { showExitDialog = true }) {
                                Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_back))
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

                if (uiState.contents.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .padding(padding)
                            .fillMaxSize()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.lesson_empty_title),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(R.string.lesson_empty_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            QwPrimaryButton(
                                text = stringResource(R.string.lesson_summary_back_to_dashboard),
                                onClick = onExit
                            )
                        }
                    }
                    return@Scaffold
                }

                Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                    Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                        key(uiState.currentIndex) {
                            when (val content = uiState.currentContent) {
                                is ExerciseContent.MultipleChoice -> MultipleChoiceExerciseContent(
                                    content = content,
                                    selectedOptionId = uiState.attempt.selectedOptionId,
                                    isChecked = uiState.isChecked,
                                    onSelect = viewModel::selectOption
                                )
                                is ExerciseContent.TapWhatYouHear -> {
                                    // Plays once on arrival - the exercise is unanswerable unheard.
                                    LaunchedEffect(content.audioAssetPath) { viewModel.playPronunciation(content.audioAssetPath) }
                                    TapWhatYouHearExerciseContent(
                                        content = content,
                                        selectedOptionId = uiState.attempt.selectedOptionId,
                                        isChecked = uiState.isChecked,
                                        onPlay = viewModel::playPronunciation,
                                        onSelect = viewModel::selectOption
                                    )
                                }
                                is ExerciseContent.Matching -> MatchingExerciseContent(
                                    content = content,
                                    matchedPairIds = uiState.attempt.matchedPairIds,
                                    pendingLeftId = uiState.attempt.pendingLeftId,
                                    lastMismatch = uiState.attempt.lastMismatch,
                                    onSelectLeft = viewModel::selectMatchingLeft,
                                    onSelectRight = viewModel::selectMatchingRight
                                )
                                is ExerciseContent.WordIntro -> WordIntroExerciseContent(
                                    content = content,
                                    strength = uiState.wordStrengths[content.wordId],
                                    selectedMeaningIndex = selectedMeaningIndex,
                                    onMeaningSelected = {
                                        selectedMeaningIndex = it
                                        visitedSenses = visitedSenses + it
                                    }
                                )
                                is ExerciseContent.ChapterIntro -> ChapterIntroExerciseContent(
                                    content = content
                                )
                                is ExerciseContent.FillInTheBlank -> FillInTheBlankExerciseContent(
                                    content = content,
                                    selectedOptionId = uiState.attempt.selectedOptionId,
                                    isChecked = uiState.isChecked,
                                    onSelect = viewModel::selectOption
                                )
                                is ExerciseContent.WordOrderBuilder -> WordOrderBuilderExerciseContent(
                                    content = content,
                                    selectedChipIds = uiState.attempt.orderedChipIds,
                                    isChecked = uiState.isChecked,
                                    onSelectChip = viewModel::selectWordOrderChip,
                                    onDeselectChip = viewModel::deselectWordOrderChip
                                )
                                is ExerciseContent.TapWordInVerse -> TapWordInVerseExerciseContent(
                                    content = content,
                                    selectedSpan = uiState.attempt.selectedSpan,
                                    onSelectWord = viewModel::selectVerseWord
                                )
                                else -> {}
                            }
                        }
                    }

                    if (uiState.isChecked) {
                        val feedbackMessage = if (uiState.lastAnswerCorrect == true) {
                            stringResource(R.string.lesson_feedback_correct)
                        } else {
                            val correctLabel = correctAnswerLabel(uiState.currentContent, language)
                            // Matching (several answers) has no single label to show.
                            if (correctLabel.isBlank()) stringResource(R.string.lesson_feedback_incorrect_tryagain)
                            else stringResource(R.string.lesson_feedback_incorrect_with_answer, correctLabel)
                        }
                        FeedbackBanner(
                            type = if (uiState.lastAnswerCorrect == true) FeedbackType.CORRECT else FeedbackType.INCORRECT,
                            message = feedbackMessage
                        )
                    }

                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        val isLastExercise = uiState.currentIndex == uiState.contents.lastIndex
                        when {
                            uiState.isChecked -> {
                                if (uiState.lastAnswerCorrect == false) {
                                    // Stacked rather than three weighted buttons in one row, which
                                    // clipped/overflowed with longer translations or large fonts.
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            if (uiState.currentIndex > 0) {
                                                QwSecondaryButton(
                                                    text = stringResource(R.string.lesson_previous_button),
                                                    onClick = viewModel::onPreviousPressed,
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                            QwSecondaryButton(
                                                text = stringResource(R.string.lesson_try_again_button),
                                                onClick = viewModel::onTryAgainPressed,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        QwPrimaryButton(
                                            text = stringResource(
                                                if (isLastExercise) R.string.lesson_finish_button else R.string.lesson_continue_button
                                            ),
                                            onClick = viewModel::onContinuePressed,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        if (uiState.currentIndex > 0) {
                                            QwSecondaryButton(
                                                text = stringResource(R.string.lesson_previous_button),
                                                onClick = viewModel::onPreviousPressed,
                                                modifier = Modifier.weight(1f)
                                            )
                                        }
                                        QwPrimaryButton(
                                            text = stringResource(
                                                if (isLastExercise) R.string.lesson_finish_button else R.string.lesson_continue_button
                                            ),
                                            onClick = viewModel::onContinuePressed,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                            uiState.currentContent is ExerciseContent.MultipleChoice ||
                                uiState.currentContent is ExerciseContent.TapWhatYouHear ||
                                uiState.currentContent is ExerciseContent.FillInTheBlank -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.currentIndex > 0) {
                                        QwSecondaryButton(
                                            text = stringResource(R.string.lesson_previous_button),
                                            onClick = viewModel::onPreviousPressed,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    QwPrimaryButton(
                                        text = stringResource(R.string.lesson_check_button),
                                        enabled = uiState.attempt.selectedOptionId != null,
                                        onClick = viewModel::onCheckPressed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            uiState.currentContent is ExerciseContent.WordOrderBuilder -> {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.currentIndex > 0) {
                                        QwSecondaryButton(
                                            text = stringResource(R.string.lesson_previous_button),
                                            onClick = viewModel::onPreviousPressed,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    QwPrimaryButton(
                                        text = stringResource(R.string.lesson_check_button),
                                        enabled = uiState.attempt.orderedChipIds.size ==
                                            (uiState.currentContent as ExerciseContent.WordOrderBuilder).orderedChips.size,
                                        onClick = viewModel::onCheckPressed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            uiState.currentContent is ExerciseContent.WordIntro -> {
                                val intro = uiState.currentContent as ExerciseContent.WordIntro
                                val totalSenses = intro.polysemyEntries.size
                                val allVisited = visitedSenses.size >= totalSenses || totalSenses <= 1
                                val onContinueOrNextSense = {
                                    if (allVisited) {
                                        viewModel.onContinuePressed()
                                    } else {
                                        val nextUnvisited = (0 until totalSenses).firstOrNull { it !in visitedSenses }
                                        if (nextUnvisited != null) {
                                            selectedMeaningIndex = nextUnvisited
                                            visitedSenses = visitedSenses + nextUnvisited
                                        }
                                    }
                                }
                                val btnTextRes = if (allVisited) R.string.lesson_teach_continue_button else R.string.next_sense
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.currentIndex > 0) {
                                        QwSecondaryButton(
                                            text = stringResource(R.string.lesson_previous_button),
                                            onClick = viewModel::onPreviousPressed,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    QwPrimaryButton(
                                        text = stringResource(btnTextRes),
                                        onClick = onContinueOrNextSense,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            uiState.currentContent is ExerciseContent.ChapterIntro -> {
                                val chNum = (uiState.currentContent as ExerciseContent.ChapterIntro).chapterNumber
                                val btnText = chapterLabel(
                                    number = chNum,
                                    language = language,
                                    numericRes = R.string.chapter_intro_begin_button,
                                    ordinalRes = R.string.chapter_intro_begin_button_ordinal
                                )
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.currentIndex > 0) {
                                        QwSecondaryButton(
                                            text = stringResource(R.string.lesson_previous_button),
                                            onClick = viewModel::onPreviousPressed,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    QwPrimaryButton(
                                        text = btnText,
                                        onClick = viewModel::onContinuePressed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            uiState.currentContent is ExerciseContent.Matching -> {
                                // Matching self-checks once every pair is solved; Skip is the way out
                                // for a learner who can't finish it (graded as wrong).
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    if (uiState.currentIndex > 0) {
                                        QwSecondaryButton(
                                            text = stringResource(R.string.lesson_previous_button),
                                            onClick = viewModel::onPreviousPressed,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    QwSecondaryButton(
                                        text = stringResource(R.string.lesson_skip_button),
                                        onClick = viewModel::onSkipPressed,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            else -> Unit // TapWordInVerse checks itself on the first tap
                        }
                    }
                }
            }

            AnswerFeedbackOverlay(
                type = when {
                    !uiState.isChecked -> null
                    uiState.lastAnswerCorrect == true -> FeedbackType.CORRECT
                    uiState.lastAnswerCorrect == false -> FeedbackType.INCORRECT
                    else -> null
                }
            )
        }
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text(stringResource(R.string.lesson_exit_confirm_title)) },
            text = { Text(stringResource(R.string.lesson_exit_confirm_message)) },
            confirmButton = {
                TextButton(onClick = onExit) { Text(stringResource(R.string.lesson_exit_confirm_leave)) }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text(stringResource(R.string.lesson_exit_confirm_stay))
                }
            }
        )
    }
}

@Composable
private fun correctAnswerLabel(content: ExerciseContent?, language: Language): String = when (content) {
    is ExerciseContent.MultipleChoice ->
        content.options.firstOrNull { it.id == content.correctOptionId }?.localizedLabel(language).orEmpty()
    is ExerciseContent.TapWhatYouHear ->
        content.options.firstOrNull { it.id == content.correctOptionId }
            ?.let { option -> option.labelArabic?.cleanArabicDisplay()?.takeIf { it.isNotBlank() } ?: option.localizedLabel(language) }
            .orEmpty()
    // FillInTheBlank's options are the Arabic words themselves, so the answer is the Arabic word
    // (its meaning only as a fallback for an option with no Arabic label).
    is ExerciseContent.FillInTheBlank ->
        content.options.firstOrNull { it.id == content.correctOptionId }
            ?.let { option -> option.labelArabic?.cleanArabicDisplay()?.takeIf { it.isNotBlank() } ?: option.localizedLabel(language) }
            .orEmpty()
    is ExerciseContent.WordOrderBuilder ->
        content.orderedChips.joinToString(" ") { it.arabicText }
    is ExerciseContent.TapWordInVerse -> {
        // Clamped like FillInTheBlankExercise's blank: content offsets that drift past the verse
        // (e.g. after a content-pipeline text change) must degrade, not crash.
        val start = content.correctWordStart.coerceIn(0, content.verseArabic.length)
        val end = content.correctWordEnd.coerceIn(start, content.verseArabic.length)
        content.verseArabic.substring(start, end)
    }
    else -> ""
}
