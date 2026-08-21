package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quranicwords.app.core.domain.AchievementDef
import com.quranicwords.app.core.ui.theme.MedallionShapeDefault

/** Resolves [AchievementDef.nameRes] as a plain string when [AchievementDef.nameArg] is null, or
 * as a `%d`-templated one when it isn't - see [AchievementDef]'s doc comment for why the catalog
 * carries both forms instead of always requiring an arg. */
@Composable
fun AchievementDef.displayName(): String =
    if (nameArg != null) stringResource(nameRes, nameArg) else stringResource(nameRes)

@Composable
fun AchievementDef.displayDescription(): String =
    if (descriptionArg != null) stringResource(descriptionRes, descriptionArg) else stringResource(descriptionRes)

/**
 * The medallion badge for one achievement - [MedallionShapeDefault] container (the shape
 * `docs/UI_GUIDELINES.md` already reserves for streak/celebratory badges) with an [IslamicMotif]
 * icon inside, per the category->motif mapping the achievements catalog assigns. [unlocked]
 * dims to a silhouette-style outline treatment instead of a blank placeholder, so a locked
 * achievement still communicates "this exists, keep going" rather than nothing at all.
 */
@Composable
fun AchievementBadge(motifKind: MotifKind, unlocked: Boolean, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 64.dp) {
    val containerColor = if (unlocked) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val iconColor = if (unlocked) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)

    Surface(modifier = modifier.size(size), shape = MedallionShapeDefault, color = containerColor) {
        IslamicMotif(kind = motifKind, modifier = Modifier.padding(size * 0.2f), color = iconColor)
    }
}
