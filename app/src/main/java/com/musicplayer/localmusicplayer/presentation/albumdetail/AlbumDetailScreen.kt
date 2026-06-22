package com.musicplayer.localmusicplayer.presentation.albumdetail

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.library.components.EditSongDialog
import com.musicplayer.localmusicplayer.presentation.library.components.SongBottomSheet
import com.musicplayer.localmusicplayer.presentation.library.components.SongList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    onSongClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var bottomSheetSong by remember { mutableStateOf<Song?>(null) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.albumName.ifEmpty { stringResource(R.string.unknown_album) }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        },
        floatingActionButton = {
            if (uiState.songs.isNotEmpty()) {
                FloatingActionButton(onClick = { viewModel.playAll() }) {
                    Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all))
                }
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.wrapContentSize()) }
        } else {
            SongList(
                songs = uiState.songs,
                onSongClick = { song -> viewModel.playSong(song) },
                onSongMenuClick = { song -> bottomSheetSong = song },
                modifier = Modifier.padding(padding)
            )
        }
    }

    bottomSheetSong?.let { song ->
        SongBottomSheet(
            song = song, playlists = uiState.playlists,
            onDismiss = { bottomSheetSong = null },
            onEdit = { editingSong = song; showEditDialog = true },
            onAddToPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) },
            onDelete = { editingSong = song; showDeleteDialog = true }
        )
    }
    if (showEditDialog && editingSong != null) {
        EditSongDialog(song = editingSong!!, onSave = { viewModel.updateSong(it); showEditDialog = false; editingSong = null }, onDismiss = { showEditDialog = false; editingSong = null }, onPickAlbumArt = { }, pickedAlbumArtUri = null)
    }
    if (showDeleteDialog && editingSong != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false; editingSong = null },
            title = { Text(stringResource(R.string.delete_file)) },
            text = { Text(stringResource(R.string.delete_file_confirm, editingSong!!.title)) },
            confirmButton = { TextButton(onClick = { viewModel.deleteSong(editingSong!!); showDeleteDialog = false; editingSong = null }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { showDeleteDialog = false; editingSong = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
