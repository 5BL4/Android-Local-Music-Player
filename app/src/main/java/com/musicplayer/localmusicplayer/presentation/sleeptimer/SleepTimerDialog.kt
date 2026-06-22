package com.musicplayer.localmusicplayer.presentation.sleeptimer

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R

@Composable
fun SleepTimerDialog(
    isActive: Boolean,
    remainingMinutes: Int,
    onStart: (Int) -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(5, 15, 30, 45, 60, 90, 120)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.sleep_timer)) },
        text = {
            Column {
                if (isActive) {
                    Text(
                        text = stringResource(R.string.timer_active, remainingMinutes),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(onClick = onCancel) {
                        Text(stringResource(R.string.cancel_timer))
                    }
                } else {
                    Text(stringResource(R.string.stop_playback_after))
                    Spacer(modifier = Modifier.height(8.dp))
                    options.forEach { minutes ->
                        TextButton(
                            onClick = { onStart(minutes) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.minutes_format, minutes))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
