package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.res.stringResource
import coil3.compose.AsyncImage
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.util.formatDuration
import androidx.compose.ui.tooling.preview.Preview
import android.content.res.Configuration

@Composable
fun SongItem(
    song: Song,
    onClick: () -> Unit,
    onMenuClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    currentSongId: Long? = null
) {
    val isCurrentSong = currentSongId != null && song.id == currentSongId
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (isCurrentSong)
                    Modifier.background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(8.dp))
                else Modifier
            )
            .clickable(onClick = onClick)
            .padding(
                start = horizontalPadding,
                end = if (onMenuClick != null) 0.dp else horizontalPadding,
                top = 8.dp,
                bottom = 8.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            if (song.albumArtUri != null) {
                AsyncImage(
                    model = song.albumArtUri,
                    contentDescription = song.album,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.MusicNote,
                    contentDescription = stringResource(R.string.album_art),
                    modifier = Modifier.padding(10.dp).fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(song.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${song.artist} - ${song.album}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Text(formatDuration(song.durationMs), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (onMenuClick != null) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.menu_more), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SongItemPreview() {
    MaterialTheme {
        SongItem(
            song = Song(
                id = 1,
                mediaStoreId = 1001,
                title = "Bohemian Rhapsody",
                artist = "Queen",
                album = "A Night at the Opera",
                albumId = 101,
                durationMs = 354000L,
                filePath = "/storage/music/bohemian_rhapsody.mp3",
                contentUri = "content://media/external/audio/media/1001",
                albumArtUri = null,
                year = 1975,
                trackNumber = 11,
                discNumber = 1,
                genre = "Rock"
            ),
            onClick = {},
            onMenuClick = {}
        )
    }
}

@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SongItemPreview_CurrentlyPlaying() {
    MaterialTheme {
        SongItem(
            song = Song(
                id = 2,
                mediaStoreId = 1002,
                title = "Stairway to Heaven",
                artist = "Led Zeppelin",
                album = "Led Zeppelin IV",
                albumId = 102,
                durationMs = 482000L,
                filePath = "/storage/music/stairway_to_heaven.mp3",
                contentUri = "content://media/external/audio/media/1002",
                albumArtUri = null,
                year = 1971,
                trackNumber = 4,
                discNumber = 1,
                genre = "Rock"
            ),
            onClick = {},
            onMenuClick = null,
            currentSongId = 2
        )
    }
}
