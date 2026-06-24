package com.musicplayer.localmusicplayer.presentation.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Language
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.model.ThemeColor
import com.musicplayer.localmusicplayer.domain.model.ThemeMode
import com.musicplayer.localmusicplayer.presentation.sleeptimer.SleepTimerDialog

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showThemePicker by remember { mutableStateOf(false) }
    var showSortPicker by remember { mutableStateOf(false) }
    var showLangPicker by remember { mutableStateOf(false) }
    var showColorPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(stringResource(R.string.theme), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))

        // Theme mode picker (AlertDialog style)
        val themeLabel = when (uiState.themeMode) {
            ThemeMode.System -> stringResource(R.string.system)
            ThemeMode.Light -> stringResource(R.string.light)
            ThemeMode.Dark -> stringResource(R.string.dark)
        }
        ListItem(
            headlineContent = { Text(stringResource(R.string.theme_mode)) },
            supportingContent = { Text(themeLabel) },
            modifier = Modifier.clickable { showThemePicker = true }
        )

        // Theme color (same indent as ListItem)
        Text(stringResource(R.string.theme_color), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 16.dp)) {
            ThemeColor.entries.take(5).forEach { color ->
                val isSelected = color == uiState.themeColor
                Box(
                    modifier = Modifier
                        .size(36.dp).clip(CircleShape).background(color.seedColor)
                        .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { viewModel.setThemeColor(color) }
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 16.dp)) {
            ThemeColor.entries.drop(5).forEach { color ->
                val isSelected = color == uiState.themeColor
                Box(
                    modifier = Modifier
                        .size(36.dp).clip(CircleShape).background(color.seedColor)
                        .then(if (isSelected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                        .clickable { viewModel.setThemeColor(color) }
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // Language (AlertDialog style)
        Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.language)) },
            supportingContent = { Text(uiState.language.displayName) },
            leadingContent = { Icon(Icons.Default.Language, contentDescription = null) },
            modifier = Modifier.clickable { showLangPicker = true }
        )

        Spacer(Modifier.height(24.dp))

        // Sorting (AlertDialog style)
        Text(stringResource(R.string.sorting), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.default_sort)) },
            supportingContent = { Text(uiState.defaultSortOption.localized()) },
            modifier = Modifier.clickable { showSortPicker = true }
        )

        Spacer(Modifier.height(24.dp))

        // Sleep Timer
        Text(stringResource(R.string.sleep_timer), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = {
                Text(if (uiState.isSleepTimerActive) stringResource(R.string.timer_active, uiState.sleepTimerRemainingMinutes) else stringResource(R.string.set_sleep_timer))
            },
            leadingContent = { Icon(Icons.Default.Bedtime, contentDescription = null) },
            modifier = Modifier.clickable { viewModel.showSleepTimerDialog() }
        )

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.library_section), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.rescan_library)) },
            leadingContent = { Icon(Icons.Default.Sync, contentDescription = null) },
            supportingContent = { if (uiState.isScanning) Text(stringResource(R.string.scanning)) },
            modifier = Modifier.clickable { viewModel.rescanMusic() }
        )

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.debug), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        ListItem(
            headlineContent = { Text(stringResource(R.string.dump_logs)) },
            leadingContent = { Icon(Icons.Default.BugReport, contentDescription = null) },
            modifier = Modifier.clickable {
                viewModel.dumpLogs()
                Toast.makeText(context, context.getString(R.string.logs_dumped), Toast.LENGTH_SHORT).show()
            }
        )

        Spacer(Modifier.height(24.dp))

        Text(stringResource(R.string.about), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(8.dp))
        val versionName = remember {
            runCatching {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            }.getOrNull() ?: ""
        }
        ListItem(
            headlineContent = { Text("LocalMusicPlayer v$versionName") },
            supportingContent = { Text(stringResource(R.string.about_text)) }
        )
    }

    // --- AlertDialog Pickers (matching sleep timer style) ---
    if (showThemePicker) {
        AlertDialog(
            onDismissRequest = { showThemePicker = false },
            title = { Text(stringResource(R.string.theme_mode)) },
            text = {
                Column {
                    ThemeMode.entries.forEach { mode ->
                        TextButton(onClick = { viewModel.setThemeMode(mode); showThemePicker = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(when (mode) {
                                ThemeMode.System -> stringResource(R.string.system)
                                ThemeMode.Light -> stringResource(R.string.light)
                                ThemeMode.Dark -> stringResource(R.string.dark)
                            })
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showThemePicker = false }) { Text(stringResource(R.string.close)) } }
        )
    }

    if (showLangPicker) {
        AlertDialog(
            onDismissRequest = { showLangPicker = false },
            title = { Text(stringResource(R.string.language)) },
            text = {
                Column {
                    Language.entries.forEach { lang ->
                        TextButton(onClick = {
                            viewModel.setLanguage(lang)
                            com.musicplayer.localmusicplayer.MusicPlayerApplication.savedLanguage = lang
                            showLangPicker = false
                            (context as? android.app.Activity)?.recreate()
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text(lang.displayName)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLangPicker = false }) { Text(stringResource(R.string.close)) } }
        )
    }

    if (showSortPicker) {
        AlertDialog(
            onDismissRequest = { showSortPicker = false },
            title = { Text(stringResource(R.string.default_sort)) },
            text = {
                Column {
                    SortOption.entries.forEach { option ->
                        TextButton(onClick = { viewModel.setDefaultSortOption(option); showSortPicker = false }, modifier = Modifier.fillMaxWidth()) {
                            Text(option.localized())
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSortPicker = false }) { Text(stringResource(R.string.close)) } }
        )
    }

    if (uiState.showSleepTimerDialog) {
        SleepTimerDialog(
            isActive = uiState.isSleepTimerActive,
            remainingMinutes = uiState.sleepTimerRemainingMinutes,
            onStart = { viewModel.startSleepTimer(it) },
            onCancel = { viewModel.cancelSleepTimer() },
            onDismiss = { viewModel.hideSleepTimerDialog() }
        )
    }
}
