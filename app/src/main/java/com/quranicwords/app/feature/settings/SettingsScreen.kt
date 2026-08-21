package com.quranicwords.app.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.ui.components.QwLogo
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.ui.components.rememberIsBanglaSelected
import com.quranicwords.app.core.ui.theme.Elevation
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val fontStyle by viewModel.fontStyle.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val streakReminderEnabled by viewModel.streakReminderEnabled.collectAsStateWithLifecycle()
    val streakReminderHour by viewModel.streakReminderHour.collectAsStateWithLifecycle()
    val streakReminderMinute by viewModel.streakReminderMinute.collectAsStateWithLifecycle()
    val backupUiState by viewModel.backupUiState.collectAsStateWithLifecycle()
    val isBangla = rememberIsBanglaSelected()
    val context = LocalContext.current
    var showLicenses by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var notificationPermissionDenied by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { context.contentResolver.openOutputStream(it)?.let(viewModel::exportBackup) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { context.contentResolver.openInputStream(it)?.let(viewModel::importBackup) }
    }
    // POST_NOTIFICATIONS is only a real runtime permission on API 33+ - below that, notifications
    // just work, so the switch enables immediately without a request round-trip.
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationPermissionDenied = !granted
        if (granted) viewModel.setStreakReminderEnabled(true)
    }
    fun onStreakReminderToggled(enabled: Boolean) {
        if (!enabled) {
            viewModel.setStreakReminderEnabled(false)
            return
        }
        val alreadyGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (alreadyGranted) {
            notificationPermissionDenied = false
            viewModel.setStreakReminderEnabled(true)
        } else {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(backupUiState.lastMessageResId) {
        // Auto-dismiss so the Backup section doesn't permanently show a stale result banner.
        if (backupUiState.lastMessageResId != null) {
            delay(4000)
            viewModel.dismissBackupMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        StaggeredEntrance(index = 0) { SettingsSectionCard {
            SectionTitle(stringResource(R.string.settings_section_appearance))

            Text(stringResource(R.string.settings_theme_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    ThemeMode.SYSTEM to stringResource(R.string.settings_theme_system),
                    ThemeMode.LIGHT to stringResource(R.string.settings_theme_light),
                    ThemeMode.DARK to stringResource(R.string.settings_theme_dark)
                )
                options.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }

            Text(stringResource(R.string.settings_language_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    Language.ENGLISH to stringResource(R.string.language_option_english),
                    Language.BANGLA to stringResource(R.string.language_option_bangla)
                )
                options.forEachIndexed { index, (lang, label) ->
                    SegmentedButton(
                        selected = language == lang,
                        onClick = { viewModel.setLanguage(lang) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }

            Text(stringResource(R.string.settings_quran_font_label), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuranFontStyle.entries) { style ->
                    FilterChip(
                        selected = fontStyle == style,
                        onClick = { viewModel.setFontStyle(style) },
                        label = { Text(if (isBangla) style.displayNameBn else style.displayNameEn) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_reduce_motion_label), style = MaterialTheme.typography.labelLarge)
                Switch(checked = reduceMotion, onCheckedChange = viewModel::setReduceMotion)
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_sound_effects_label), style = MaterialTheme.typography.labelLarge)
                Switch(checked = soundEnabled, onCheckedChange = viewModel::setSoundEnabled)
            }

            Text(stringResource(R.string.settings_font_scale_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    FontScale.SMALL to stringResource(R.string.settings_font_scale_small),
                    FontScale.DEFAULT to stringResource(R.string.settings_font_scale_default),
                    FontScale.LARGE to stringResource(R.string.settings_font_scale_large),
                    FontScale.EXTRA_LARGE to stringResource(R.string.settings_font_scale_extra_large)
                )
                options.forEachIndexed { index, (scale, label) ->
                    SegmentedButton(
                        selected = fontScale == scale,
                        onClick = { viewModel.setFontScale(scale) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }
        } }

        StaggeredEntrance(index = 1) { SettingsSectionCard {
            SectionTitle(stringResource(R.string.settings_section_notifications))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_streak_reminder_label), style = MaterialTheme.typography.labelLarge)
                Switch(checked = streakReminderEnabled, onCheckedChange = ::onStreakReminderToggled)
            }
            Text(
                stringResource(R.string.settings_streak_reminder_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (streakReminderEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(stringResource(R.string.settings_streak_reminder_time_label), style = MaterialTheme.typography.labelLarge)
                    QwSecondaryButton(
                        text = "%02d:%02d".format(streakReminderHour, streakReminderMinute),
                        onClick = { showTimePicker = true }
                    )
                }
            }
            if (notificationPermissionDenied) {
                Text(
                    stringResource(R.string.settings_streak_reminder_permission_denied),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        } }

        StaggeredEntrance(index = 2) { SettingsSectionCard {
            SectionTitle(stringResource(R.string.settings_section_backup))
            Text(
                stringResource(R.string.settings_backup_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QwPrimaryButton(
                    text = stringResource(R.string.settings_backup_export),
                    enabled = !backupUiState.isWorking,
                    modifier = Modifier.weight(1f),
                    onClick = { exportLauncher.launch("quranicwords_backup.json") }
                )
                QwSecondaryButton(
                    text = stringResource(R.string.settings_backup_import),
                    enabled = !backupUiState.isWorking,
                    modifier = Modifier.weight(1f),
                    onClick = { importLauncher.launch(arrayOf("application/json")) }
                )
            }
            if (backupUiState.isWorking) {
                CircularProgressIndicator(modifier = Modifier.padding(top = 4.dp))
            }
            backupUiState.lastMessageResId?.let { resId ->
                Text(
                    stringResource(resId),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (backupUiState.lastMessageWasError) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.primary
                )
            }
        } }

        StaggeredEntrance(index = 3) { SettingsSectionCard {
            SectionTitle(stringResource(R.string.settings_section_about))
            QwLogo(size = 56.dp)
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
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = streakReminderHour,
            initialMinute = streakReminderMinute,
            is24Hour = false
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text(stringResource(R.string.settings_streak_reminder_time_label)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.setStreakReminderTime(timePickerState.hour, timePickerState.minute)
                    showTimePicker = false
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.settings_close)) }
            }
        ) {
            TimePicker(state = timePickerState)
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

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

/**
 * Gives each Settings section the "3D box" [Elevation.raised] card treatment instead of sitting
 * flat against the screen background - see `docs/UI_GUIDELINES.md`. Not itself tappable, so it
 * doesn't need `pressDepth`; the interactive controls inside (buttons, chips) carry their own.
 */
@Composable
private fun SettingsSectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Elevation.raised)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}
