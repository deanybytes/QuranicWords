package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.core.domain.model.ChoiceOption
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.domain.model.localizedLabel
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected
import com.quranicwords.app.core.ui.motion.MotionSpecs

@Composable
fun MultipleChoiceExerciseContent(
    content: ExerciseContent.MultipleChoice,
    selectedOptionId: String?,
    isChecked: Boolean,
    onSelect: (String) -> Unit
) {
    val isBangla = rememberIsBanglaSelected()

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        content.promptArabic?.let { arabic ->
            Text(
                text = arabic,
                fontSize = 64.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            content.options.forEach { option ->
                OptionCard(
                    option = option,
                    isBangla = isBangla,
                    isSelected = option.id == selectedOptionId,
                    isChecked = isChecked,
                    isCorrectOption = option.id == content.correctOptionId,
                    onClick = { onSelect(option.id) }
                )
            }
        }
    }
}

@Composable
internal fun OptionCard(
    option: ChoiceOption,
    isBangla: Boolean,
    isSelected: Boolean,
    isChecked: Boolean,
    isCorrectOption: Boolean,
    onClick: () -> Unit
) {
    val targetContainer = when {
        isChecked && isCorrectOption -> MaterialTheme.colorScheme.primaryContainer
        isChecked && isSelected && !isCorrectOption -> MaterialTheme.colorScheme.errorContainer
        isSelected -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    // Morphs instead of snapping when isChecked flips, so the correct/incorrect reveal reads as
    // a deliberate moment rather than an instant color swap.
    val container by animateColorAsState(targetValue = targetContainer, label = "optionContainer")

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val density = LocalDensity.current
    val pressSpec = MotionSpecs.snappy<Float>()
    val scale by animateFloatAsState(targetValue = if (isPressed) 0.98f else 1f, animationSpec = pressSpec, label = "optionPressScale")
    val tiltX by animateFloatAsState(targetValue = if (isPressed) -3f else 0f, animationSpec = pressSpec, label = "optionPressTilt")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                rotationX = tiltX
                cameraDistance = 12f * density.density
            },
        colors = CardDefaults.cardColors(containerColor = container),
        onClick = onClick,
        enabled = !isChecked,
        interactionSource = interactionSource
    ) {
        Text(
            text = option.localizedLabel(isBangla),
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
    }
}
