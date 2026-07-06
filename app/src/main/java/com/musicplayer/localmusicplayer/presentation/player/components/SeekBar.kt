package com.musicplayer.localmusicplayer.presentation.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.util.formatDuration
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration

@Composable
fun SeekBar(
    currentPositionMs: Long,
    durationMs: Long,
    onSeek: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var sliderPosition by remember { mutableStateOf(currentPositionMs.toFloat()) }
    var isDragging by remember { mutableStateOf(false) }
    val seekBarDescription = stringResource(R.string.seek_bar)

    // Sync with playback position only when not dragging
    LaunchedEffect(currentPositionMs) {
        if (!isDragging) {
            sliderPosition = currentPositionMs.toFloat()
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Slider(
            value = sliderPosition,
            onValueChange = { isDragging = true; sliderPosition = it },
            onValueChangeFinished = {
                onSeek(sliderPosition.toLong())
                isDragging = false
            },
            valueRange = 0f..durationMs.toFloat().coerceAtLeast(1f),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .semantics {
                    contentDescription = seekBarDescription
                }
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(formatDuration(sliderPosition.toLong()), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(formatDuration(durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeekBarPreview() {
    MaterialTheme {
        SeekBar(
            currentPositionMs = 60000L,
            durationMs = 180000L,
            onSeek = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SeekBarPreview_Start() {
    MaterialTheme {
        SeekBar(
            currentPositionMs = 0L,
            durationMs = 240000L,
            onSeek = {}
        )
    }
}
