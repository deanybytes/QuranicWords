package com.quranicwords.app.feature.onboarding.font

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.QwSelectableCard
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.toFontFamily
import com.quranicwords.app.core.util.QuranPreviewText

@Composable
fun FontSelectScreen(
    onContinue: () -> Unit,
    viewModel: FontSelectViewModel = hiltViewModel()
) {
    Column(modifier = Modifier.fillMaxSize().padding(top = 24.dp, start = 24.dp, end = 24.dp)) {
        Text(stringResource(R.string.font_select_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.font_select_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            itemsIndexed(QuranFontStyle.entries) { index, style ->
                StaggeredEntrance(index = index) {
                    FontOptionCard(style = style, onClick = { viewModel.selectFont(style, onContinue) })
                }
            }
        }
    }
}

@Composable
private fun FontOptionCard(style: QuranFontStyle, onClick: () -> Unit) {
    val language = rememberSelectedLanguage()
    QwSelectableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(style.displayName.get(language), style = MaterialTheme.typography.titleMedium)
            Text(
                text = QuranPreviewText.SURAH_AL_KAWTHAR.joinToString("   "),
                fontFamily = style.toFontFamily(),
                fontSize = 22.sp,
                lineHeight = 34.sp,
                textAlign = TextAlign.End,
                modifier = Modifier.fillMaxWidth()
            )
            if (!style.isBundled) {
                Text(
                    stringResource(R.string.font_pending_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
