package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Playlist

@Composable
fun PlaylistGrid(
    playlists: List<Playlist>,
    onPlaylistClick: (Long) -> Unit,
    onPlaylistLongClick: ((Long, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (playlists.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_playlists_yet),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(modifier = modifier.fillMaxSize()) {
            items(items = playlists, key = { it.id }) { playlist ->
                PlaylistCard(
                    playlist = playlist,
                    onClick = { onPlaylistClick(playlist.id) },
                    onLongClick = onPlaylistLongClick?.let { { it(playlist.id, playlist.name) } }
                )
            }
        }
    }
}
