package com.musicplayer.localmusicplayer.presentation.albumdetail

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.components.DeleteConfirmationDialog
import com.musicplayer.localmusicplayer.presentation.library.components.EditSongDialog
import com.musicplayer.localmusicplayer.presentation.library.components.SongBottomSheet
import com.musicplayer.localmusicplayer.presentation.library.components.SongList
import com.musicplayer.localmusicplayer.presentation.library.DeleteEvent
import com.musicplayer.localmusicplayer.presentation.library.EditEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailScreen(
    albumId: Long,
    onSongClick: (Long) -> Unit,
    onBack: () -> Unit,
    viewModel: AlbumDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsState()
    val editConfirmation by viewModel.editConfirmation.collectAsState()
    var bottomSheetSong by remember { mutableStateOf<Song?>(null) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }
    var pendingEditSong by remember { mutableStateOf<Song?>(null) }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingDeleteSong?.let { viewModel.deleteSong(it) }
            pendingEditSong?.let { viewModel.updateSong(it) }
        }
        pendingDeleteSong = null
        pendingEditSong = null
    }

    val deleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val reqId = deleteConfirmation ?: return@rememberLauncherForActivityResult
        viewModel.onDeleteDialogResult(reqId, result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(deleteConfirmation) {
        deleteConfirmation?.let { reqId ->
            viewModel.intentSenderForConfirmation()?.let { sender ->
                deleteLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } ?: viewModel.onDeleteDialogResult(reqId, false)
        }
    }

    val editLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val reqId = editConfirmation ?: return@rememberLauncherForActivityResult
        viewModel.onEditDialogResult(reqId, result.resultCode == android.app.Activity.RESULT_OK)
    }

    LaunchedEffect(editConfirmation) {
        editConfirmation?.let { reqId ->
            viewModel.intentSenderForEditConfirmation()?.let { sender ->
                editLauncher.launch(IntentSenderRequest.Builder(sender).build())
            } ?: viewModel.onEditDialogResult(reqId, false)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.editEvent.collect { event ->
            val msg = when (event) {
                EditEvent.Saved -> context.getString(R.string.edit_saved)
                EditEvent.Failed -> context.getString(R.string.edit_failed)
                EditEvent.Cancelled -> context.getString(R.string.edit_cancelled)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.deleteEvent.collect { event ->
            val msg = when (event) {
                DeleteEvent.Deleted -> context.getString(R.string.delete_success)
                DeleteEvent.Failed -> context.getString(R.string.delete_failed)
                DeleteEvent.Cancelled -> context.getString(R.string.delete_cancelled)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.albumName.ifEmpty { stringResource(R.string.unknown_album) }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    if (uiState.songs.isNotEmpty()) {
                        IconButton(onClick = { viewModel.playAll() }) {
                            Icon(Icons.Default.PlayArrow, contentDescription = stringResource(R.string.play_all))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.wrapContentSize()) }
        } else {
            SongList(
                songs = uiState.songs,
                onSongClick = { song -> viewModel.playSong(song) },
                onSongMenuClick = { song -> bottomSheetSong = song },
                modifier = Modifier.padding(padding),
                currentSongId = uiState.currentSongId
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
        EditSongDialog(song = editingSong!!, onSave = { edited ->
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                pendingEditSong = edited
                writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            } else {
                viewModel.updateSong(edited)
            }
            showEditDialog = false; editingSong = null
        }, onDismiss = { showEditDialog = false; editingSong = null }, onPickAlbumArt = { }, pickedAlbumArtUri = null)
    }
    if (showDeleteDialog && editingSong != null) {
        DeleteConfirmationDialog(
            title = stringResource(R.string.delete_file),
            message = stringResource(R.string.delete_file_confirm, editingSong!!.title),
            onConfirm = {
                editingSong?.let { song ->
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        pendingDeleteSong = song
                        writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        viewModel.deleteSong(song)
                    }
                }
                showDeleteDialog = false; editingSong = null
            },
            onDismiss = { showDeleteDialog = false; editingSong = null }
        )
    }
}
