package com.quranicwords.app.feature.onboarding.font

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.QwSelectableCard
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.BrandGold
import com.quranicwords.app.core.ui.theme.toFontFamily
import com.quranicwords.app.core.util.QuranPreviewText

@Composable
fun FontSelectScreen(
    onContinue: (LearningPath) -> Unit,
    viewModel: FontSelectViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(top = 20.dp, start = 20.dp, end = 20.dp)
    ) {
        Text(
            stringResource(R.string.font_select_title),
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.font_select_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(QuranFontStyle.entries) { index, style ->
                StaggeredEntrance(index = index) {
                    FontOptionCard(
                        style = style,
                        onClick = { viewModel.selectFont(style, onContinue) }
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun FontOptionCard(style: QuranFontStyle, onClick: () -> Unit) {
    val language = rememberSelectedLanguage()
    val shape = RoundedCornerShape(20.dp)

    QwSelectableCard(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().clip(shape)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row: Script Family Pill Badge + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    // Script Family Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = style.scriptFamily.get(language),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Title
                    Text(
                        text = style.displayName.get(language),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Origin / Publisher Region
            Text(
                text = style.originRegion.get(language),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Arabic Preview Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = QuranPreviewText.SURAH_AL_KAWTHAR.joinToString("   "),
                    fontFamily = style.toFontFamily(),
                    fontSize = 22.sp,
                    lineHeight = 36.sp,
                    textAlign = TextAlign.End,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Key Orthographical Features List
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                style.keyFeatures.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
                        )
                    }
                }
            }
        }
    }
}
