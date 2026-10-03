package com.quranicwords.app.feature.achievements

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.AchievementBadge
import com.quranicwords.app.core.ui.components.displayDescription
import com.quranicwords.app.core.ui.components.displayName
import com.quranicwords.app.core.ui.motion.rememberReducedGlass
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import com.quranicwords.app.core.ui.components.QwIconButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.QwShapes
import com.quranicwords.app.core.util.VerseReferenceFormatter

/**
 * Progress tab's achievements preview: one glassmorphic card with the unlocked count, the few
 * most relevant achievements (latest unlocks, then the nearest to done) and a way into the full
 * [AchievementsScreen]. A plain, non-scrolling column, so the host screen owns all scrolling.
 */
@Composable
fun AchievementsSection(onOpenAll: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedGlass = rememberReducedGlass()
    val shape = RoundedCornerShape(28.dp)

    Column(modifier = Modifier.fillMaxWidth()) {
        // Section heading above the glass card
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(bottom = 14.dp)
                .semantics { heading() }
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.85f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.EmojiEvents,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = stringResource(R.string.achievements_title),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        // Glassmorphic container with its own fixed-height internal scroll
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(
                    if (reducedGlass) MaterialTheme.colorScheme.surfaceContainer
                    else MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f)
                )
                .then(
                    if (reducedGlass) Modifier
                    else Modifier.border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            listOf(
                                Color.White.copy(alpha = 0.32f),
                                Color.White.copy(alpha = 0.06f),
                                Color.Transparent
                            )
                        ),
                        shape = shape
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (uiState.isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    val language = rememberSelectedLanguage()
                    Text(
                        stringResource(
                            R.string.achievements_unlocked_count,
                            VerseReferenceFormatter.formatNumber(uiState.unlockedCount, language),
                            VerseReferenceFormatter.formatNumber(uiState.items.size, language)
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    uiState.preview.forEach { item ->
                        AchievementRow(item = item, reducedGlass = reducedGlass)
                    }
                    QwSecondaryButton(
                        text = stringResource(R.string.achievements_view_all),
                        onClick = onOpenAll,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Top specular highlight streak (3D glass sheen)
            if (!reducedGlass) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.13f),
                                    Color.Transparent
                                )
                            )
                        )
                )
            }
        }
    }
}

/** Every achievement, each locked one with its requirement and a progress bar towards it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AchievementsScreen(onBack: () -> Unit, viewModel: AchievementsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val reducedGlass = rememberReducedGlass()
    val language = rememberSelectedLanguage()
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.achievements_title)) },
                navigationIcon = {
                    QwIconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }
        // Unlocked first (newest on top), then locked ones nearest to done.
        val ordered = uiState.items.filter { it.unlocked }.sortedByDescending { it.unlockedAtEpochMillis ?: 0L } +
            uiState.items.filterNot { it.unlocked }.sortedByDescending { it.progress?.fraction ?: 0f }
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    stringResource(
                        R.string.achievements_unlocked_count,
                        VerseReferenceFormatter.formatNumber(uiState.unlockedCount, language),
                        VerseReferenceFormatter.formatNumber(uiState.items.size, language)
                    ),
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(ordered, key = { it.def.id }) { item ->
                AchievementRow(item = item, reducedGlass = reducedGlass)
            }
        }
    }
}

@Composable
private fun AchievementRow(item: AchievementUiItem, reducedGlass: Boolean) {
    val rowShape = RoundedCornerShape(16.dp)

    val bgColor = if (item.unlocked) {
        MaterialTheme.colorScheme.tertiaryContainer.copy(
            alpha = if (reducedGlass) 1f else 0.60f
        )
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(
            alpha = if (reducedGlass) 1f else 0.50f
        )
    }

    val accentColor = if (item.unlocked)
        MaterialTheme.colorScheme.tertiary
    else
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(bgColor)
            .then(
                if (reducedGlass) Modifier
                else Modifier.border(
                    width = 1.dp,
                    brush = Brush.linearGradient(
                        listOf(
                            accentColor.copy(alpha = if (item.unlocked) 0.50f else 0.18f),
                            Color.Transparent
                        )
                    ),
                    shape = rowShape
                )
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            AchievementBadge(
                motifKind = item.def.motifKind,
                unlocked = item.unlocked,
                size = 52.dp
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.def.displayName(),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (item.unlocked)
                        MaterialTheme.colorScheme.onTertiaryContainer
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(2.dp))
                // The requirement is shown either way - a locked medallion says what earns it.
                Text(
                    text = item.def.displayDescription(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (item.unlocked)
                        MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.78f)
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.80f)
                )
                val progress = item.progress
                if (!item.unlocked && progress != null && progress.target > 1) {
                    val language = rememberSelectedLanguage()
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        LinearProgressIndicator(
                            progress = { progress.fraction },
                            modifier = Modifier.weight(1f).height(6.dp).clip(QwShapes.extraSmall),
                            color = MaterialTheme.colorScheme.tertiary,
                            trackColor = MaterialTheme.colorScheme.surface
                        )
                        Text(
                            stringResource(
                                R.string.quest_progress,
                                VerseReferenceFormatter.formatNumber(progress.current, language),
                                VerseReferenceFormatter.formatNumber(progress.target, language)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Gold dot for unlocked achievements
            if (item.unlocked) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                )
            }
        }
    }
}

