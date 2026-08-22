package com.quranicwords.app.feature.lesson

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.localizedLabel
import com.quranicwords.app.core.navigation.Route
import com.quranicwords.app.core.ui.components.AnswerFeedbackOverlay
import com.quranicwords.app.core.ui.components.FeedbackBanner
import com.quranicwords.app.core.ui.components.FeedbackType
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberQwHaptics
import com.quranicwords.app.feature.lesson.exercise.FillInTheBlankExerciseContent
import com.quranicwords.app.feature.lesson.exercise.ListenAndTypeExerciseContent
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

    LaunchedEffect(uiState.isChecked, uiState.lastAnswerCorrect) {
        if (uiState.isChecked) {
            when (uiState.lastAnswerCorrect) {
                true -> haptics.onCorrectAnswer()
                false -> haptics.onIncorrectAnswer()
                null -> Unit
            }
        }
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
                    durationMillis = result.durationMillis
                )
            )
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                TopAppBar(
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
                        IconButton(onClick = { showExitDialog = true }) {
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

            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Box(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    when (val content = uiState.currentContent) {
                        is ExerciseContent.MultipleChoice -> MultipleChoiceExerciseContent(
                            content = content,
                            selectedOptionId = uiState.attempt.selectedOptionId,
                            isChecked = uiState.isChecked,
                            onSelect = viewModel::selectOption
                        )
                        is ExerciseContent.TapWhatYouHear -> TapWhatYouHearExerciseContent(
                            content = content,
                            selectedOptionId = uiState.attempt.selectedOptionId,
                            isChecked = uiState.isChecked,
                            onPlay = viewModel::playAudio,
                            onSelect = viewModel::selectOption
                        )
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
                            onPlay = viewModel::playAudio
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
                        is ExerciseContent.ListenAndType -> ListenAndTypeExerciseContent(
                            content = content,
                            typedAnswer = uiState.attempt.typedAnswer,
                            isChecked = uiState.isChecked,
                            onPlay = viewModel::playAudio,
                            onTypedAnswerChange = viewModel::updateTypedAnswer
                        )
                        is ExerciseContent.TapWordInVerse -> TapWordInVerseExerciseContent(
                            content = content,
                            selectedSpan = uiState.attempt.selectedSpan,
                            onSelectWord = viewModel::selectVerseWord
                        )
                        null -> {}
                    }
                }

                if (uiState.isChecked) {
                    val feedbackMessage = if (uiState.lastAnswerCorrect == true) {
                        stringResource(R.string.lesson_feedback_correct)
                    } else {
                        val correctLabel = correctAnswerLabel(uiState.currentContent, language)
                        stringResource(R.string.lesson_feedback_incorrect_with_answer, correctLabel)
                    }
                    FeedbackBanner(
                        type = if (uiState.lastAnswerCorrect == true) FeedbackType.CORRECT else FeedbackType.INCORRECT,
                        message = feedbackMessage
                    )
                }

                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    val isLastExercise = uiState.currentIndex == uiState.contents.lastIndex
                    when {
                        uiState.isChecked -> QwPrimaryButton(
                            text = stringResource(
                                if (isLastExercise) R.string.lesson_finish_button else R.string.lesson_continue_button
                            ),
                            onClick = viewModel::onContinuePressed,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.currentContent is ExerciseContent.MultipleChoice ||
                            uiState.currentContent is ExerciseContent.TapWhatYouHear ||
                            uiState.currentContent is ExerciseContent.FillInTheBlank -> QwPrimaryButton(
                            text = stringResource(R.string.lesson_check_button),
                            enabled = uiState.attempt.selectedOptionId != null,
                            onClick = viewModel::onCheckPressed,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.currentContent is ExerciseContent.WordOrderBuilder -> QwPrimaryButton(
                            text = stringResource(R.string.lesson_check_button),
                            enabled = uiState.attempt.orderedChipIds.size ==
                                (uiState.currentContent as ExerciseContent.WordOrderBuilder).orderedChips.size,
                            onClick = viewModel::onCheckPressed,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.currentContent is ExerciseContent.ListenAndType -> QwPrimaryButton(
                            text = stringResource(R.string.lesson_check_button),
                            enabled = uiState.attempt.typedAnswer.isNotBlank(),
                            onClick = viewModel::onCheckPressed,
                            modifier = Modifier.fillMaxWidth()
                        )
                        uiState.currentContent is ExerciseContent.WordIntro -> QwPrimaryButton(
                            text = stringResource(R.string.lesson_teach_continue_button),
                            onClick = viewModel::onContinuePressed,
                            modifier = Modifier.fillMaxWidth()
                        )
                        else -> Unit // matching self-advances once solved
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
        content.options.firstOrNull { it.id == content.correctOptionId }?.localizedLabel(language).orEmpty()
    is ExerciseContent.FillInTheBlank ->
        content.options.firstOrNull { it.id == content.correctOptionId }?.localizedLabel(language).orEmpty()
    is ExerciseContent.WordOrderBuilder ->
        content.orderedChips.joinToString(" ") { it.arabicText }
    is ExerciseContent.ListenAndType -> content.correctAnswer
    is ExerciseContent.TapWordInVerse -> content.verseArabic.substring(content.correctWordStart, content.correctWordEnd)
    else -> ""
}
