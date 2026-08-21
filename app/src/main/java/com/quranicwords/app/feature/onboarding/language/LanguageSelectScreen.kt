package com.quranicwords.app.feature.onboarding.language

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.QwSelectableCard

@Composable
fun LanguageSelectScreen(
    onContinue: () -> Unit,
    viewModel: LanguageSelectViewModel = hiltViewModel()
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        QwLogo(size = 64.dp)
        Text(
            stringResource(R.string.language_select_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            stringResource(R.string.language_select_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Each language names itself (its own nativeName) - the universal convention for language
        // pickers, and the only labeling that doesn't need 12 languages' worth of translated
        // string resources for "English"/"Bangla"/etc. just to show this one screen.
        Language.entries.forEachIndexed { index, lang ->
            StaggeredEntrance(index = index) {
                LanguageOptionCard(
                    label = lang.nativeName,
                    onClick = { viewModel.selectLanguage(lang, onContinue) }
                )
            }
        }
    }
}

@Composable
private fun LanguageOptionCard(label: String, onClick: () -> Unit) {
    QwSelectableCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
    }
}
