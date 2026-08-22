package com.quranicwords.app.feature.progress

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Placeholder for the Progress tab (QwBottomNavShell's second tab) - charts (words learned, Quran
 * coverage, days practiced, points growth) and the folded-in Achievements section land here in a
 * follow-up pass. Exists now so the bottom-nav shell has a real, navigable third destination.
 */
@Composable
fun ProgressScreen() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Progress - coming soon", style = MaterialTheme.typography.titleMedium)
    }
}
