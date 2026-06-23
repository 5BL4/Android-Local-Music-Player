package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Album

@Composable
fun AlbumGrid(
    albums: List<Album>,
    onAlbumClick: (Long) -> Unit,
    onAlbumLongClick: ((Album) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (albums.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(R.string.no_albums_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Composite key: albumId alone is NOT unique when an album edit partially fails.
            // getAlbums() does SELECT DISTINCT album, album_id, artist, album_art_uri — so the
            // same albumId can appear in two rows (old name vs new name) during/after a partial
            // tag edit. Using albumId alone as the LazyVerticalGrid key throws
            // IllegalArgumentException("Key X was already used") and crashes the app.
            items(items = albums, key = { "${it.albumId}_${it.name}" }) { album ->
                AlbumCard(
                    album = album,
                    onClick = { onAlbumClick(album.albumId) },
                    onLongClick = onAlbumLongClick?.let { { it(album) } }
                )
            }
        }
    }
}
