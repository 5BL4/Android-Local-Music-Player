package com.musicplayer.localmusicplayer.presentation.equalizer

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R

private val PRESET_ZH: Map<String, String> = mapOf(
    "Classical" to "古典", "Dance" to "舞曲",
    "Folk" to "民谣", "Heavy Metal" to "重金属",
    "Hip Hop" to "嘻哈", "Jazz" to "爵士", "Pop" to "流行",
    "Rock" to "摇滚", "R&B" to "节奏蓝调", "Latin" to "拉丁",
    "Country" to "乡村", "Custom" to "自定义"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    onBack: () -> Unit,
    viewModel: EqualizerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lang = java.util.Locale.getDefault().language

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.equalizer)) },
                actions = {
                    Switch(
                        checked = uiState.isEnabled,
                        onCheckedChange = { viewModel.toggleEnabled() }
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(8.dp)
        ) {
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = {
                            val name = uiState.presets.find { it.index == uiState.currentPresetIndex }?.name ?: "Custom"
                            if (lang == "zh") PRESET_ZH[name] ?: name else name
                        }(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.preset)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        uiState.presets.forEach { preset ->
                            DropdownMenuItem(
                                text = { Text(if (lang == "zh") PRESET_ZH[preset.name] ?: preset.name else preset.name) },
                                onClick = {
                                    viewModel.onPresetSelected(preset.index)
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.bands.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        uiState.bands.forEach { band ->
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "${band.levelDb / 100}dB",
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 1
                                )
                                BoxWithConstraints(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Slider(
                                        value = band.levelDb.toFloat(),
                                        onValueChange = { viewModel.onBandLevelChanged(band.index, it.toInt()) },
                                        valueRange = -1500f..1500f,
                                        modifier = Modifier
                                            .requiredWidth(maxHeight)
                                            .height(56.dp)
                                            .rotate(270f)
                                    )
                                }
                                Text(
                                    text = if (band.frequencyHz >= 1000) "${band.frequencyHz / 1000}K" else "${band.frequencyHz}",
                                    style = MaterialTheme.typography.labelSmall,
                                    textAlign = TextAlign.Center,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }
    }
}
