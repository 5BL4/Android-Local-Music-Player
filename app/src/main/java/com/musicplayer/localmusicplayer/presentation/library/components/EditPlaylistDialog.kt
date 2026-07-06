package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Playlist
import coil3.compose.AsyncImage

@Composable
fun EditPlaylistDialog(
    playlist: Playlist,
    pickedCoverUri: String?,
    onSave: (name: String, coverUri: String?) -> Unit,
    onPickCover: () -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(playlist.name) }
    val displayCover = pickedCoverUri ?: playlist.coverArtUri

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_playlist_title)) },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.playlist_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(72.dp),
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        if (displayCover != null) {
                            AsyncImage(
                                model = displayCover,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                Icons.Default.PlaylistPlay,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(16.dp)
                            )
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    TextButton(onClick = onPickCover) {
                        Text(stringResource(R.string.choose_playlist_icon))
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, displayCover) }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
