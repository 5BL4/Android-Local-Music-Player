package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.player.components.AddToPlaylistDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongBottomSheet(
    song: Song,
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onAddToPlaylist: (Long) -> Unit,
    onDelete: () -> Unit,
    onCreatePlaylist: (String) -> Unit = {}
) {
    var showPlaylistPicker by remember { mutableStateOf(false) }

    if (showPlaylistPicker) {
        AddToPlaylistDialog(
            playlists = playlists,
            onCreateNew = onCreatePlaylist,
            onAddToExisting = { playlistId ->
                onAddToPlaylist(playlistId)
                showPlaylistPicker = false
                onDismiss()
            },
            onDismiss = { showPlaylistPicker = false }
        )
    } else {
        ModalBottomSheet(onDismissRequest = onDismiss) {
            Column(modifier = Modifier.padding(bottom = 32.dp)) {
                Text(song.title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
                Divider()
                ListItem(
                    headlineContent = { Text(stringResource(R.string.edit_song_info)) },
                    modifier = Modifier.clickable { onEdit(); onDismiss() }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.add_to_playlist)) },
                    modifier = Modifier.clickable { showPlaylistPicker = true }
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.delete_file), color = MaterialTheme.colorScheme.error) },
                    modifier = Modifier.clickable { onDelete(); onDismiss() }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}
