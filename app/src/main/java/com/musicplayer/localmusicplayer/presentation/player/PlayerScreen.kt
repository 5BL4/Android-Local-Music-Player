package com.musicplayer.localmusicplayer.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.player.components.AddToPlaylistDialog
import com.musicplayer.localmusicplayer.presentation.player.components.AlbumArt
import com.musicplayer.localmusicplayer.presentation.player.components.PlaybackControls
import com.musicplayer.localmusicplayer.presentation.player.components.SeekBar

@Composable
fun PlayerScreen(
    onLyricsClick: (Long) -> Unit,
    onEqualizerClick: () -> Unit,
    onBack: () -> Unit,
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val playbackState = uiState.playbackState

    Box(
        modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBars)
    ) {
        when (playbackState) {
            is PlaybackState.Idle -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_song_selected),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            is PlaybackState.Error -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (playbackState as PlaybackState.Error).message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            else -> {
                // Persist last known info across Buffering state transitions
                var lastSong by remember { mutableStateOf<Song?>(null) }
                var lastShuffle by remember { mutableStateOf(false) }
                var lastRepeat by remember { mutableStateOf(RepeatMode.Off) }
                var lastDuration by remember { mutableStateOf(0L) }

                val song = when (playbackState) {
                    is PlaybackState.Playing -> { lastSong = playbackState.currentSong; playbackState.currentSong }
                    is PlaybackState.Paused -> { lastSong = playbackState.currentSong; playbackState.currentSong }
                    is PlaybackState.Buffering -> lastSong
                    else -> null
                }
                val isPlaying = playbackState is PlaybackState.Playing || playbackState is PlaybackState.Buffering
                val shuffle = when (playbackState) {
                    is PlaybackState.Playing -> { lastShuffle = playbackState.isShuffleEnabled; playbackState.isShuffleEnabled }
                    is PlaybackState.Paused -> { lastShuffle = playbackState.isShuffleEnabled; playbackState.isShuffleEnabled }
                    is PlaybackState.Buffering -> lastShuffle
                    else -> false
                }
                val rpt = when (playbackState) {
                    is PlaybackState.Playing -> { lastRepeat = playbackState.repeatMode; playbackState.repeatMode }
                    is PlaybackState.Paused -> { lastRepeat = playbackState.repeatMode; playbackState.repeatMode }
                    is PlaybackState.Buffering -> lastRepeat
                    else -> RepeatMode.Off
                }
                val dur = when (playbackState) {
                    is PlaybackState.Playing -> { lastDuration = playbackState.durationMs; playbackState.durationMs }
                    is PlaybackState.Paused -> { lastDuration = playbackState.durationMs; playbackState.durationMs }
                    is PlaybackState.Buffering -> lastDuration
                    else -> 0L
                }

                if (song != null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.weight(0.5f))
                        AlbumArt(albumArtUri = song.albumArtUri)
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.headlineMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = song.artist,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (song.album.isNotBlank()) {
                            Text(
                                text = song.album,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        SeekBar(
                            currentPositionMs = uiState.currentPositionMs,
                            durationMs = dur,
                            onSeek = { viewModel.seekTo(it) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        PlaybackControls(
                            isPlaying = isPlaying,
                            isShuffleEnabled = shuffle,
                            repeatMode = rpt,
                            onPlayPauseClick = { viewModel.togglePlayPause() },
                            onSkipPreviousClick = { viewModel.skipToPrevious() },
                            onSkipNextClick = { viewModel.skipToNext() },
                            onShuffleClick = { viewModel.toggleShuffle() },
                            onRepeatClick = {
                                val nextMode = when (rpt) {
                                    RepeatMode.Off -> RepeatMode.All
                                    RepeatMode.All -> RepeatMode.One
                                    RepeatMode.One -> RepeatMode.Off
                                }
                                viewModel.setRepeatMode(nextMode)
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            IconButton(onClick = { onLyricsClick(song.id) }) {
                                Icon(Icons.Default.Lyrics, contentDescription = stringResource(R.string.lyrics))
                            }
                            IconButton(onClick = { viewModel.showAddToPlaylistDialog() }) {
                                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = stringResource(R.string.add_to_playlist))
                            }
                            IconButton(onClick = onEqualizerClick) {
                                Icon(Icons.Default.Equalizer, contentDescription = stringResource(R.string.equalizer))
                            }
                        }
                        // Add to playlist dialog
                        if (uiState.showAddToPlaylistDialog) {
                            AddToPlaylistDialog(
                                playlists = uiState.playlists,
                                onCreateNew = { name -> viewModel.createAndAddToPlaylist(name, song.id) },
                                onAddToExisting = { playlistId -> viewModel.addToPlaylist(playlistId, song.id) },
                                onDismiss = { viewModel.hideAddToPlaylistDialog() }
                            )
                        }
                        Spacer(modifier = Modifier.weight(0.3f))
                        LyricsPreviewCard(
                            onClick = { onLyricsClick(song.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }

        // Back button overlay (top-left, floats above content and shows in all states)
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Tappable card that fills the dead space below the player controls and
 * routes the user to the lyrics screen. Uses a muted surfaceVariant fill
 * to stay visually subordinate to the album art while still reading as
 * a distinct, interactive element.
 */
@Composable
private fun LyricsPreviewCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lyrics,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.view_lyrics),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
