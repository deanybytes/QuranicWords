package com.quranicwords.app.feature.lesson

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.util.VerseReferenceFormatter
import kotlinx.coroutines.delay
import java.util.Locale

/**
 * Shown instead of starting a Learn lesson while hearts are at zero. Friendly rather than
 * punitive: a live countdown to the next heart, and a direct route to practice (which restores
 * one). The lesson starts by itself the moment a heart comes back - see LessonViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OutOfHeartsSheet(
    nextHeartAtMillis: Long?,
    onPractice: () -> Unit,
    onBack: () -> Unit
) {
    val language = rememberSelectedLanguage()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(nextHeartAtMillis) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    val remainingSeconds = ((nextHeartAtMillis ?: now) - now).coerceAtLeast(0L) / 1000
    val countdown = VerseReferenceFormatter.formatDigits(
        String.format(Locale.US, "%d:%02d", remainingSeconds / 60, remainingSeconds % 60),
        language
    )

    ModalBottomSheet(onDismissRequest = onBack, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Filled.FavoriteBorder,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Text(
                stringResource(R.string.out_of_hearts_title),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                stringResource(R.string.out_of_hearts_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (nextHeartAtMillis != null) {
                Text(
                    stringResource(R.string.out_of_hearts_next, countdown),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            QwPrimaryButton(
                text = stringResource(R.string.out_of_hearts_practice),
                onClick = onPractice,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                stringResource(R.string.out_of_hearts_practice_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            QwSecondaryButton(
                text = stringResource(R.string.lesson_summary_back_to_dashboard),
                onClick = onBack,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
