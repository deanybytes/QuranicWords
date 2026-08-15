package com.quranicwords.app.feature.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.data.local.entity.LessonStatus
import com.quranicwords.app.core.data.local.entity.ModuleEntity
import com.quranicwords.app.core.data.local.entity.UserProgressEntity
import com.quranicwords.app.core.ui.components.PointsBadge
import com.quranicwords.app.core.ui.components.StreakBadge
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected
import com.quranicwords.app.core.ui.motion.MotionSpecs
import com.quranicwords.app.core.ui.motion.rememberReducedMotion
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenLesson: (String) -> Unit,
    onOpenReview: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isBangla = rememberIsBanglaSelected()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = stringResource(R.string.home_open_settings))
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PointsBadge(uiState.totalPoints)
                StreakBadge(uiState.currentStreak)
            }

            // A single flat lazy list for the whole screen (module headers + every lesson node)
            // rather than nesting a plain eager Column of lesson nodes inside each LazyColumn
            // item - the vocabulary module alone can have hundreds of lessons, and composing all
            // of them immediately (instead of only what's on screen) is real, measurable jank.
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

                uiState.modules.forEach { module ->
                    if (module.isImplemented) {
                        item(key = "header_${module.id}") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    if (isBangla) module.titleBn else module.titleEn,
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    if (isBangla) module.descriptionBn else module.descriptionEn,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        val lessons = uiState.lessonsByModuleId[module.id] ?: emptyList()
                        itemsIndexed(lessons, key = { _, lesson -> lesson.id }) { index, lesson ->
                            LessonPathNode(
                                index = index,
                                title = if (isBangla) lesson.titleBn else lesson.titleEn,
                                progress = uiState.progressByLessonId[lesson.id],
                                onClick = { onOpenLesson(lesson.id) }
                            )
                        }
                    } else {
                        item(key = "locked_${module.id}") {
                            LockedModuleCard(module, isBangla)
                        }
                    }
                }
            }
        }
    }
}

/**
 * One node of the winding "skill path" (à la Duolingo) - offset left/right in a sine wave, with
 * a short dashed spine segment behind it that chains into a continuous-looking line as
 * consecutive nodes scroll into view. Each node is a self-contained lazy list item (see
 * [HomeScreen]) rather than part of one eagerly-composed path, so a module with hundreds of
 * lessons only composes the ones actually on screen.
 */
@Composable
private fun LessonPathNode(
    index: Int,
    title: String,
    progress: UserProgressEntity?,
    onClick: () -> Unit
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val bias = sin(index * Math.PI / 3.0).toFloat() * 0.55f

    Box(
        modifier = Modifier
            .fillMaxWidth()
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
            progress = progress,
            onClick = onClick,
            modifier = Modifier.align(BiasAlignment(horizontalBias = bias, verticalBias = 0f))
        )
    }
}

@Composable
private fun LessonNode(
    title: String,
    progress: UserProgressEntity?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val status = progress?.status ?: LessonStatus.LOCKED
    val isUnlocked = status != LessonStatus.LOCKED
    val reducedMotion = rememberReducedMotion()

    // Detects the actual locked->unlocked transition (not just "this node happens to be
    // unlocked when it first scrolls into view") so the pop+shimmer below only plays once, right
    // when it happens - never replayed on every scroll-in/out of this lazy-list item.
    var previousStatus by remember { mutableStateOf<LessonStatus?>(null) }
    var justUnlocked by remember { mutableStateOf(false) }
    LaunchedEffect(status) {
        if (previousStatus == LessonStatus.LOCKED && status != LessonStatus.LOCKED && !reducedMotion) {
            justUnlocked = true
        }
        previousStatus = status
    }
    val shimmer = remember { Animatable(0f) }
    LaunchedEffect(justUnlocked) {
        if (justUnlocked) {
            shimmer.snapTo(1f)
            shimmer.animateTo(0f, animationSpec = tween(durationMillis = 600))
            justUnlocked = false
        }
    }

    val scale by animateFloatAsState(
        targetValue = if (isUnlocked) 1f else 0.9f,
        animationSpec = MotionSpecs.celebratory(),
        label = "nodeScale"
    )
    val glowColor = MaterialTheme.colorScheme.tertiary

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        val containerColor = when (status) {
            LessonStatus.COMPLETED -> MaterialTheme.colorScheme.primary
            LessonStatus.UNLOCKED -> MaterialTheme.colorScheme.primaryContainer
            LessonStatus.LOCKED -> MaterialTheme.colorScheme.surfaceVariant
        }
        val (icon, tint) = when (status) {
            LessonStatus.COMPLETED -> Icons.Filled.CheckCircle to MaterialTheme.colorScheme.onPrimary
            LessonStatus.UNLOCKED -> Icons.Filled.PlayArrow to MaterialTheme.colorScheme.onPrimaryContainer
            LessonStatus.LOCKED -> Icons.Filled.Lock to MaterialTheme.colorScheme.onSurfaceVariant
        }

        Box(
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .drawBehind {
                    if (shimmer.value > 0f) {
                        drawCircle(
                            color = glowColor,
                            radius = (size.minDimension / 2f) * (1f + shimmer.value * 0.7f),
                            alpha = shimmer.value * 0.45f
                        )
                    }
                }
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
 * item whose most recent attempt was wrong. */
@Composable
private fun ReviewEntryCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        onClick = onClick
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

@Composable
private fun LockedModuleCard(module: ModuleEntity, isBangla: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(Icons.Filled.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Column {
                Text(if (isBangla) module.titleBn else module.titleEn, style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(R.string.home_module_coming_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
