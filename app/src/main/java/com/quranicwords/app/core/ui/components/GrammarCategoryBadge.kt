package com.quranicwords.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.LemmaCategory

/**
 * Resolves the 3-part classical Arabic grammar category from word ID prefix:
 * - `wn_` -> [LemmaCategory.NOUN] (Ism - الاسم)
 * - `wv_` -> [LemmaCategory.VERB] (Fi'l - الفعل)
 * - `wp_` -> [LemmaCategory.PARTICLE] (Ḥarf - الحرف)
 */
fun resolveCategoryFromWordId(wordId: String?): LemmaCategory? = when {
    wordId == null -> null
    wordId.startsWith("wv_") -> LemmaCategory.VERB
    wordId.startsWith("wp_") -> LemmaCategory.PARTICLE
    wordId.startsWith("wn_") -> LemmaCategory.NOUN
    else -> null
}

/**
 * Renders a distinctive visual badge indicating whether a word is an Ism (Noun),
 * Fi'l (Verb), or Ḥarf (Particle).
 */
@Composable
fun GrammarCategoryBadge(
    category: LemmaCategory?,
    modifier: Modifier = Modifier
) {
    if (category == null || category == LemmaCategory.MIXED) return

    val particleAccent = Color(0xFF0288D1)
    val verbAccent = Color(0xFFD4AF37)
    val nounAccent = Color(0xFF2E7D32)

    val icon = when (category) {
        LemmaCategory.VERB -> Icons.Filled.FlashOn
        LemmaCategory.PARTICLE -> Icons.Filled.AutoAwesome
        LemmaCategory.NOUN, LemmaCategory.MIXED -> Icons.Filled.AutoStories
    }

    val labelResId = when (category) {
        LemmaCategory.VERB -> R.string.word_category_verb
        LemmaCategory.PARTICLE -> R.string.word_category_particle
        LemmaCategory.NOUN, LemmaCategory.MIXED -> R.string.word_category_noun
    }

    val tint = when (category) {
        LemmaCategory.VERB -> verbAccent
        LemmaCategory.PARTICLE -> particleAccent
        LemmaCategory.NOUN, LemmaCategory.MIXED -> nounAccent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(tint.copy(alpha = 0.15f))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(14.dp)
        )
        Text(
            text = stringResource(labelResId),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = tint
        )
    }
}
