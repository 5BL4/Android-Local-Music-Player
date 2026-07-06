package com.musicplayer.localmusicplayer.presentation.player.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.musicplayer.localmusicplayer.R

@Composable
fun AlbumArt(
    albumArtUri: String?,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        val size: Dp = minOf(maxWidth * 0.85f, 320.dp)
        Surface(
            modifier = Modifier
                .size(size)
                .clip(RoundedCornerShape(16.dp)),
            color = MaterialTheme.colorScheme.surfaceVariant,
            shadowElevation = 8.dp
        ) {
            if (albumArtUri != null) {
                AsyncImage(
                    model = albumArtUri,
                    contentDescription = stringResource(R.string.album_art),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MusicNote,
                        contentDescription = stringResource(R.string.album_art),
                        modifier = Modifier.size(size * 0.33f),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
