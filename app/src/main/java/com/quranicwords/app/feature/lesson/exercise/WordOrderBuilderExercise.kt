package com.quranicwords.app.feature.lesson.exercise

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.ExerciseContent
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected

/**
 * Tap-to-place word-order exercise: chips from [content.orderedChips] are shown shuffled in the
 * "available" pool; tapping one appends its id to [selectedChipIds] (the build area, in tap
 * order) and removes it from the pool; tapping a build-area chip undoes that pick. Correctness is
 * a chip-id sequence match against [content.orderedChips] - see `LessonViewModel.onCheckPressed`.
 */
@Composable
fun WordOrderBuilderExerciseContent(
    content: ExerciseContent.WordOrderBuilder,
    selectedChipIds: List<String>,
    isChecked: Boolean,
    onSelectChip: (String) -> Unit,
    onDeselectChip: (String) -> Unit
) {
    val isBangla = rememberIsBanglaSelected()
    val shuffledChips = remember(content) { content.orderedChips.shuffled() }
    val chipsById = remember(content) { content.orderedChips.associateBy { it.id } }
    val availableChips = shuffledChips.filter { it.id !in selectedChipIds }

    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = if (isBangla) content.translationBn else content.translationEn,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(R.string.word_order_build_area_label),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth().height(56.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                selectedChipIds.forEach { chipId ->
                    val chip = chipsById[chipId] ?: return@forEach
                    WordOrderChip(
                        text = chip.arabicText,
                        selected = true,
                        enabled = !isChecked,
                        onClick = { onDeselectChip(chip.id) }
                    )
                }
            }
        }

        HorizontalDivider()

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableChips.forEach { chip ->
                WordOrderChip(
                    text = chip.arabicText,
                    selected = false,
                    enabled = !isChecked,
                    onClick = { onSelectChip(chip.id) }
                )
            }
        }
    }
}

@Composable
private fun WordOrderChip(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick,
        enabled = enabled
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            fontSize = 20.sp,
            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
