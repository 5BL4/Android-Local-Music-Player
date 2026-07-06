package com.musicplayer.localmusicplayer.presentation.player.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Playlist

@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onCreateNew: (String) -> Unit,
    onAddToExisting: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateField by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            Column {
                if (playlists.isEmpty() || showCreateField) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = { newName = it },
                        label = { Text(stringResource(R.string.playlist_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                        Button(onClick = {
                            if (newName.isNotBlank()) {
                                onCreateNew(newName.trim())
                                onDismiss()
                            }
                        }) {
                            Text(stringResource(R.string.create))
                        }
                    }
                    if (playlists.isNotEmpty()) {
                        TextButton(onClick = { showCreateField = false }) {
                            Text(stringResource(R.string.choose_existing_playlist))
                        }
                    }
                } else {
                    LazyColumn {
                        items(items = playlists, key = { it.id }) { playlist ->
                            ListItem(
                                headlineContent = { Text(playlist.name) },
                                supportingContent = { Text(stringResource(R.string.songs_count, playlist.songCount)) },
                                modifier = Modifier.clickable {
                                    onAddToExisting(playlist.id)
                                    onDismiss()
                                }
                            )
                        }
                    }
                    TextButton(onClick = { showCreateField = true }) {
                        Text(stringResource(R.string.create_new_playlist_btn))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
