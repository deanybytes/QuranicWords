package com.quranicwords.app.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quranicwords.app.R
import com.quranicwords.app.core.domain.model.DailyGoalLevel
import com.quranicwords.app.core.domain.model.FontScale
import com.quranicwords.app.core.domain.model.Language
import com.quranicwords.app.core.domain.model.LearningPath
import com.quranicwords.app.core.domain.model.LearningStyle
import com.quranicwords.app.core.domain.model.QuranFontStyle
import com.quranicwords.app.core.domain.model.ThemeMode
import com.quranicwords.app.core.ui.components.QwIconButton
import com.quranicwords.app.core.ui.components.QwPrimaryButton
import com.quranicwords.app.core.ui.components.QwSecondaryButton
import com.quranicwords.app.core.ui.components.SectionCard
import com.quranicwords.app.core.ui.components.SectionTitle
import com.quranicwords.app.core.ui.components.StaggeredEntrance
import com.quranicwords.app.core.domain.model.get
import com.quranicwords.app.core.ui.components.rememberSelectedLanguage
import com.quranicwords.app.core.ui.theme.Elevation
import com.quranicwords.app.core.ui.theme.toFontFamily
import com.quranicwords.app.core.util.QuranPreviewText
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    showBackButton: Boolean = true,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val themeMode by viewModel.themeMode.collectAsStateWithLifecycle()
    val language by viewModel.language.collectAsStateWithLifecycle()
    val fontStyle by viewModel.fontStyle.collectAsStateWithLifecycle()
    val reduceMotion by viewModel.reduceMotion.collectAsStateWithLifecycle()
    val reduceGlassEffects by viewModel.reduceGlassEffects.collectAsStateWithLifecycle()
    val soundEnabled by viewModel.soundEnabled.collectAsStateWithLifecycle()
    val fontScale by viewModel.fontScale.collectAsStateWithLifecycle()
    val streakReminderEnabled by viewModel.streakReminderEnabled.collectAsStateWithLifecycle()
    val streakReminderHour by viewModel.streakReminderHour.collectAsStateWithLifecycle()
    val streakReminderMinute by viewModel.streakReminderMinute.collectAsStateWithLifecycle()
    val backupUiState by viewModel.backupUiState.collectAsStateWithLifecycle()
    val learningPath by viewModel.learningPath.collectAsStateWithLifecycle()
    val learningStyle by viewModel.learningStyle.collectAsStateWithLifecycle()
    val dailyGoalLevel by viewModel.dailyGoalLevel.collectAsStateWithLifecycle()
    val progressResetDone by viewModel.progressResetDone.collectAsStateWithLifecycle()
    val displayLanguage = rememberSelectedLanguage()
    val context = LocalContext.current
    var showTimePicker by remember { mutableStateOf(false) }
    var showResetProgressConfirm by remember { mutableStateOf(false) }
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

    LaunchedEffect(progressResetDone) {
        if (progressResetDone) {
            delay(4000)
            viewModel.dismissProgressResetDone()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.shadow(
                    Elevation.raised,
                    RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                ),
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    // Absent when hosted as a bottom-nav tab (QwBottomNavShell) - there's no
                    // "back" to go to from a tab, unlike when this was a pushed Route.Settings
                    // destination.
                    if (showBackButton) {
                        QwIconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                        }
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
        StaggeredEntrance(index = 0) { SectionCard {
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
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(Language.entries) { lang ->
                    FilterChip(
                        selected = language == lang,
                        onClick = { viewModel.setLanguage(lang) },
                        label = { Text(lang.nativeName) }
                    )
                }
            }

            Text(stringResource(R.string.settings_quran_font_label), style = MaterialTheme.typography.labelLarge)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(QuranFontStyle.entries) { style ->
                    FilterChip(
                        selected = fontStyle == style,
                        onClick = { viewModel.setFontStyle(style) },
                        label = { Text(style.displayName.get(displayLanguage)) }
                    )
                }
            }

            // Live preview showing selected font rendering
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    Text(
                        text = QuranPreviewText.SURAH_AL_KAWTHAR.joinToString("   ۝   "),
                        fontFamily = fontStyle.toFontFamily(),
                        fontSize = 20.sp,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Right,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth()
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
                Text(stringResource(R.string.settings_reduce_glass_label), style = MaterialTheme.typography.labelLarge)
                Switch(checked = reduceGlassEffects, onCheckedChange = viewModel::setReduceGlassEffects)
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

        StaggeredEntrance(index = 1) { SectionCard {
            SectionTitle(stringResource(R.string.settings_section_sound))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(stringResource(R.string.settings_sound_effects_label), style = MaterialTheme.typography.labelLarge)
                Switch(checked = soundEnabled, onCheckedChange = viewModel::setSoundEnabled)
            }
        } }

        StaggeredEntrance(index = 2) { SectionCard {
            SectionTitle(stringResource(R.string.settings_section_mode))

            Text(stringResource(R.string.settings_learning_path_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    LearningPath.LEARN to stringResource(R.string.settings_learning_path_learn),
                    LearningPath.TEST_ONLY to stringResource(R.string.settings_learning_path_test_only)
                )
                options.forEachIndexed { index, (path, label) ->
                    SegmentedButton(
                        selected = learningPath == path,
                        onClick = { viewModel.setLearningPath(path) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }

            // Repeat-count tiers only apply to the Learn path's teach step - Test/Quiz-only has no
            // teach step for them to govern, same reason it skips LearningStyleSelect in onboarding.
            if (learningPath == LearningPath.LEARN) {
                Text(stringResource(R.string.settings_learning_style_label), style = MaterialTheme.typography.labelLarge)
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val options = listOf(
                        LearningStyle.SHARP to stringResource(R.string.format_repeat_count, LearningStyle.SHARP.repeatCount),
                        LearningStyle.SLOW to stringResource(R.string.format_repeat_count, LearningStyle.SLOW.repeatCount),
                        LearningStyle.COZY to stringResource(R.string.format_repeat_count, LearningStyle.COZY.repeatCount)
                    )
                    options.forEachIndexed { index, (style, label) ->
                        SegmentedButton(
                            selected = learningStyle == style,
                            onClick = { viewModel.setLearningStyle(style) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                        ) { Text(label) }
                    }
                }
                val learningStyleDesc = when (learningStyle) {
                    LearningStyle.SHARP -> R.string.learning_style_sharp_description
                    LearningStyle.SLOW -> R.string.learning_style_slow_description
                    LearningStyle.COZY -> R.string.learning_style_cozy_description
                }
                Text(
                    stringResource(learningStyleDesc),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(stringResource(R.string.settings_daily_goal_label), style = MaterialTheme.typography.labelLarge)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options = listOf(
                    DailyGoalLevel.CASUAL to stringResource(R.string.format_minutes_short, DailyGoalLevel.CASUAL.minutes),
                    DailyGoalLevel.STEADY to stringResource(R.string.format_minutes_short, DailyGoalLevel.STEADY.minutes),
                    DailyGoalLevel.DEVOTED to stringResource(R.string.format_minutes_short, DailyGoalLevel.DEVOTED.minutes)
                )
                options.forEachIndexed { index, (level, label) ->
                    SegmentedButton(
                        selected = dailyGoalLevel == level,
                        onClick = { viewModel.setDailyGoalLevel(level) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
                    ) { Text(label) }
                }
            }
            val dailyGoalDesc = when (dailyGoalLevel) {
                DailyGoalLevel.CASUAL -> R.string.daily_goal_casual_description
                DailyGoalLevel.STEADY -> R.string.daily_goal_steady_description
                DailyGoalLevel.DEVOTED -> R.string.daily_goal_devoted_description
            }
            Text(
                stringResource(dailyGoalDesc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } }

        StaggeredEntrance(index = 3) { SectionCard {
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

        StaggeredEntrance(index = 4) { SectionCard {
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
                    onClick = {
                        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"))
                        exportLauncher.launch("quranicwords_backup_$timestamp.json")
                    }
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

            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

            Text(stringResource(R.string.settings_reset_progress_label), style = MaterialTheme.typography.labelLarge)
            Text(
                stringResource(R.string.settings_reset_progress_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            QwSecondaryButton(
                text = stringResource(R.string.settings_reset_progress_button),
                onClick = { showResetProgressConfirm = true }
            )
            if (progressResetDone) {
                Text(
                    stringResource(R.string.settings_reset_progress_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
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

    if (showResetProgressConfirm) {
        AlertDialog(
            onDismissRequest = { showResetProgressConfirm = false },
            title = { Text(stringResource(R.string.settings_reset_progress_confirm_title)) },
            text = { Text(stringResource(R.string.settings_reset_progress_confirm_message)) },
            confirmButton = {
                TextButton(onClick = {
                    showResetProgressConfirm = false
                    viewModel.resetProgress()
                }) { Text(stringResource(R.string.settings_reset_progress_confirm_button), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showResetProgressConfirm = false }) { Text(stringResource(R.string.settings_close)) }
            }
        )
    }

}
