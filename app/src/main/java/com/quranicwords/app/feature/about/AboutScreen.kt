package com.quranicwords.app.feature.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.quranicwords.app.R
import com.quranicwords.app.core.ui.components.AbstractBookMotif
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.SectionCard
import com.quranicwords.app.core.ui.components.SectionTitle
import com.quranicwords.app.core.ui.components.StaggeredEntrance

/**
 * The app's own bottom-nav tab (Home / Progress / About / Settings) - was previously the bottom-
 * most section inside [com.quranicwords.app.feature.settings.SettingsScreen], split out into its
 * own tab so the app's identity/provenance/contact info isn't buried at the end of a long
 * preferences scroll. Same content as before (logo, motto, copyright, content-provenance note,
 * licenses, Connect links), same [SectionCard]/[SectionTitle] treatment, just its own screen.
 */
@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var showLicenses by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item {
            StaggeredEntrance(index = 0) {
                Text(
                    stringResource(R.string.settings_section_about),
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.semantics { heading() }
                )
            }
        }

        item {
            StaggeredEntrance(index = 1) { SectionCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    QwLogo(size = 56.dp)
                    AbstractBookMotif(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.18f)
                    )
                }
                Text(
                    stringResource(R.string.settings_motto),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(stringResource(R.string.settings_copyright), style = MaterialTheme.typography.bodySmall)
                Text(
                    stringResource(R.string.settings_content_provenance_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                QwSecondaryButton(
                    text = stringResource(R.string.settings_licenses_button),
                    onClick = { showLicenses = true }
                )
            } }
        }

        item {
            StaggeredEntrance(index = 2) { SectionCard {
                SectionTitle(stringResource(R.string.settings_section_connect))
                ConnectLinkRow(
                    icon = Icons.Filled.Code,
                    label = stringResource(R.string.settings_connect_github),
                    onClick = { uriHandler.openUri("https://github.com/deanybytes/QuranicWords") }
                )
                ConnectLinkRow(
                    icon = Icons.Filled.PlayCircle,
                    label = stringResource(R.string.settings_connect_youtube),
                    onClick = { uriHandler.openUri("https://youtube.com/@deanytalks") }
                )
                ConnectLinkRow(
                    icon = Icons.Filled.Email,
                    label = stringResource(R.string.settings_connect_email),
                    onClick = { uriHandler.openUri("mailto:deanybytes@gmail.com") }
                )
                ConnectLinkRow(
                    icon = Icons.AutoMirrored.Filled.Send,
                    label = stringResource(R.string.settings_connect_telegram),
                    onClick = { uriHandler.openUri("https://t.me/deanytalks") }
                )
                ConnectLinkRow(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    label = stringResource(R.string.settings_connect_whatsapp),
                    onClick = { uriHandler.openUri("https://whatsapp.com/channel/0029VaLkfgUEwEk0cgLkcD3G") }
                )
            } }
        }
    }

    if (showLicenses) {
        val noticeText = remember {
            runCatching { context.assets.open("NOTICE.txt").bufferedReader().use { it.readText() } }
                .getOrDefault("")
        }
        AlertDialog(
            onDismissRequest = { showLicenses = false },
            title = { Text(stringResource(R.string.settings_licenses_title)) },
            text = {
                Text(
                    noticeText,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.heightIn(max = 480.dp).verticalScroll(rememberScrollState())
                )
            },
            confirmButton = {
                TextButton(onClick = { showLicenses = false }) {
                    Text(stringResource(R.string.settings_close))
                }
            }
        )
    }
}

/** One row in the "Connect" list - opens an external link/app via [LocalUriHandler] (browser,
 * mail client, or the Telegram/WhatsApp app if installed). URLs are user-supplied contact
 * channels, not seeded content, so they're plain hardcoded strings here rather than a data/domain
 * model - moved verbatim from the old Settings-hosted About section. */
@Composable
private fun ConnectLinkRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
