package com.musicplayer.localmusicplayer.presentation.playlistdetail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.library.components.SongItem
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    onBack: () -> Unit,
    viewModel: PlaylistDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var songToDelete by remember { mutableStateOf<Song?>(null) }
    var draggedIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragTargetIndex by remember { mutableIntStateOf(-1) }
    val scrollState = rememberScrollState()
    val itemHeightDp = 64f
    val density = androidx.compose.ui.platform.LocalDensity.current
    val itemHeightPx = with(density) { itemHeightDp.dp.toPx() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (uiState.isEditing) {
                        TextField(
                            value = uiState.editName,
                            onValueChange = { viewModel.onEditNameChanged(it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleLarge
                        )
                    } else {
                        Text(
                            text = uiState.playlist?.name ?: stringResource(R.string.playlists),
                            modifier = Modifier.clickable { viewModel.startEditing() }
                        )
                    }
                },
                actions = {
                    if (uiState.isEditing) {
                        TextButton(onClick = { viewModel.saveEdit() }) {
                            Text(stringResource(R.string.save))
                        }
                    } else if (uiState.songs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playAll() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.songs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_music_found), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
            ) {
                uiState.songs.forEachIndexed { index, song ->
                    val isDragged = index == draggedIndex
                    val offsetY = if (isDragged) dragOffset else 0f
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .zIndex(if (isDragged) 1f else 0f)
                            .graphicsLayer {
                                translationY = offsetY
                                shadowElevation = if (isDragged) 8f else 0f
                            }
                            .height(64.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Drag handle
                        Icon(
                            imageVector = Icons.Default.DragHandle,
                            contentDescription = "Reorder",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .size(24.dp)
                                .pointerInput(index) {
                                    detectDragGesturesAfterLongPress(
                                        onDragStart = { draggedIndex = index },
                                        onDragEnd = {
                                            if (draggedIndex >= 0 && dragTargetIndex >= 0 && draggedIndex != dragTargetIndex) {
                                                val newSongs = uiState.songs.toMutableList()
                                                val item = newSongs.removeAt(draggedIndex)
                                                newSongs.add(dragTargetIndex, item)
                                                viewModel.reorderSongs(newSongs.map { it.id })
                                            }
                                            draggedIndex = -1
                                            dragOffset = 0f
                                            dragTargetIndex = -1
                                        },
                                        onDragCancel = {
                                            draggedIndex = -1
                                            dragOffset = 0f
                                            dragTargetIndex = -1
                                        },
                                        onDrag = { change, offset ->
                                            change.consume()
                                            dragOffset += offset.y
                                            dragTargetIndex = (index + (dragOffset / itemHeightPx).roundToInt())
                                                .coerceIn(0, uiState.songs.size - 1)
                                        }
                                    )
                                }
                        )
                        // Song content
                        SongItem(
                            song = song,
                            onClick = { viewModel.playSong(song) },
                            onMenuClick = { songToDelete = song },
                            modifier = Modifier.weight(1f),
                            horizontalPadding = 4.dp
                        )
                    }
                    if (index < uiState.songs.size - 1) {
                        Divider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
    }

    if (songToDelete != null) {
        AlertDialog(
            onDismissRequest = { songToDelete = null },
            title = { Text(stringResource(R.string.remove_song_title)) },
            text = { Text(stringResource(R.string.remove_song_confirm, songToDelete!!.title)) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeSong(songToDelete!!.id)
                    songToDelete = null
                }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { songToDelete = null }) { Text(stringResource(R.string.cancel)) }
            }
        )
    }
}
