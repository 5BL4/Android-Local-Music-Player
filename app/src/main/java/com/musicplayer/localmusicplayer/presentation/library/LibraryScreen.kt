package com.musicplayer.localmusicplayer.presentation.library

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.library.components.AlbumBottomSheet
import com.musicplayer.localmusicplayer.presentation.library.components.AlbumGrid
import com.musicplayer.localmusicplayer.presentation.library.components.ArtistList
import com.musicplayer.localmusicplayer.presentation.library.components.EditPlaylistDialog
import com.musicplayer.localmusicplayer.presentation.library.components.EditSongDialog
import com.musicplayer.localmusicplayer.presentation.library.components.PagingSongList
import com.musicplayer.localmusicplayer.presentation.library.components.PlaylistGrid
import com.musicplayer.localmusicplayer.presentation.library.components.SongBottomSheet

// ─── Songs ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    onPlayerOpen: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsState()
    val editConfirmation by viewModel.editConfirmation.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }
    var contextMenuSong by remember { mutableStateOf<Song?>(null) }
    var editingSong by remember { mutableStateOf<Song?>(null) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var pickedAlbumArt by remember { mutableStateOf<String?>(null) }

    val albumArtPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri -> pickedAlbumArt = uri?.toString() }

    var pendingDeleteSong by remember { mutableStateOf<Song?>(null) }
    var pendingEditSong by remember { mutableStateOf<Song?>(null) }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingDeleteSong?.let { viewModel.deleteSongFile(it) }
            pendingEditSong?.let { viewModel.updateSongMetadata(it) }
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

    LaunchedEffect(Unit) { viewModel.scanMusic() }

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
                DeleteEvent.Deleted -> context.getString(R.string.edit_saved)
                DeleteEvent.Failed -> context.getString(R.string.delete_failed)
                DeleteEvent.Cancelled -> context.getString(R.string.delete_cancelled)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (showSearchBar) {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { viewModel.onSearchQueryChanged(it) },
                            placeholder = { Text(stringResource(R.string.search_music)) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(stringResource(R.string.tab_songs))
                    }
                },
                actions = {
                    if (!showSearchBar) {
                        IconButton(onClick = { viewModel.scanMusic() }) {
                            Icon(Icons.Default.Sync, contentDescription = stringResource(R.string.scan_music))
                        }
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                        }
                    } else {
                        TextButton(onClick = {
                            showSearchBar = false
                            viewModel.onSearchQueryChanged("")
                        }) {
                            Text(stringResource(R.string.done))
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                PagingSongList(
                    pagingFlow = viewModel.songPagingFlow,
                    onSongClick = { song ->
                        viewModel.playSong(song)
                        onPlayerOpen()
                    },
                    onSongMenuClick = { song -> contextMenuSong = song }
                )
            }

            contextMenuSong?.let { song ->
                SongBottomSheet(
                    song = song,
                    playlists = uiState.playlists,
                    onDismiss = { contextMenuSong = null },
                    onEdit = { editingSong = song; showEditDialog = true },
                    onAddToPlaylist = { plId -> viewModel.addSongToPlaylist(plId, song.id) },
                    onDelete = { editingSong = song; showDeleteDialog = true }
                )
            }

            if (showEditDialog && editingSong != null) {
                EditSongDialog(
                    song = editingSong!!,
                    onSave = { edited ->
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            pendingEditSong = edited
                            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.updateSongMetadata(edited)
                        }
                        showEditDialog = false
                        editingSong = null
                        pickedAlbumArt = null
                    },
                    onDismiss = { showEditDialog = false; editingSong = null; pickedAlbumArt = null },
                    onPickAlbumArt = { albumArtPicker.launch(androidx.activity.result.PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                    pickedAlbumArtUri = pickedAlbumArt
                )
            }

            if (showDeleteDialog && editingSong != null) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false; editingSong = null },
                    title = { Text(stringResource(R.string.delete_file)) },
                    text = { Text(stringResource(R.string.delete_file_confirm, editingSong!!.title)) },
                    confirmButton = {
                        TextButton(onClick = {
                            editingSong?.let { song ->
                                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                    pendingDeleteSong = song
                                    writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    viewModel.deleteSongFile(song)
                                }
                            }
                            showDeleteDialog = false
                            editingSong = null
                        }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false; editingSong = null }) { Text(stringResource(R.string.cancel)) }
                    }
                )
            }
        }
    }
}

// ─── Albums ──────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    onAlbumClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val deleteConfirmation by viewModel.deleteConfirmation.collectAsState()
    val editConfirmation by viewModel.editConfirmation.collectAsState()
    var albumSheet by remember { mutableStateOf<Album?>(null) }
    var editingAlbum by remember { mutableStateOf<Album?>(null) }
    var showAlbumEdit by remember { mutableStateOf(false) }
    var showAlbumDelete by remember { mutableStateOf(false) }
    var editAlbumName by remember { mutableStateOf("") }
    var editAlbumArtist by remember { mutableStateOf("") }

    var pendingAlbumDelete by remember { mutableStateOf<Long?>(null) }
    var pendingAlbumEdit by remember { mutableStateOf<Triple<Long, String, String>?>(null) }
    val writePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            pendingAlbumDelete?.let { viewModel.deleteAlbumSongs(it) }
            pendingAlbumEdit?.let { (albumId, newAlbum, newArtist) ->
                viewModel.updateAlbumInfo(albumId, newAlbum, newArtist)
            }
        }
        pendingAlbumDelete = null
        pendingAlbumEdit = null
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
                DeleteEvent.Deleted -> context.getString(R.string.edit_saved)
                DeleteEvent.Failed -> context.getString(R.string.delete_failed)
                DeleteEvent.Cancelled -> context.getString(R.string.delete_cancelled)
            }
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_albums)) })
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AlbumGrid(
                albums = uiState.albums,
                onAlbumClick = onAlbumClick,
                onAlbumLongClick = { album -> albumSheet = album }
            )

            albumSheet?.let { alb ->
                AlbumBottomSheet(
                    album = alb,
                    onDismiss = { albumSheet = null },
                    onEdit = { editingAlbum = alb; editAlbumName = alb.name; editAlbumArtist = alb.artist; showAlbumEdit = true; albumSheet = null },
                    onDelete = { editingAlbum = alb; showAlbumDelete = true; albumSheet = null }
                )
            }

            if (showAlbumEdit && editingAlbum != null) {
                val eAlb = editingAlbum!!
                AlertDialog(
                    onDismissRequest = { showAlbumEdit = false; editingAlbum = null },
                    title = { Text("编辑专辑信息") },
                    text = {
                        Column {
                            OutlinedTextField(value = editAlbumName, onValueChange = { editAlbumName = it }, label = { Text("专辑名") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(value = editAlbumArtist, onValueChange = { editAlbumArtist = it }, label = { Text("艺术家") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                        }
                    },
                    confirmButton = { TextButton(onClick = {
                        val albumId = eAlb.albumId
                        val newAlbum = editAlbumName
                        val newArtist = editAlbumArtist
                        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                            ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                            pendingAlbumEdit = Triple(albumId, newAlbum, newArtist)
                            writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        } else {
                            viewModel.updateAlbumInfo(albumId, newAlbum, newArtist)
                        }
                        showAlbumEdit = false; editingAlbum = null
                    }) { Text("保存") } },
                    dismissButton = { TextButton(onClick = { showAlbumEdit = false; editingAlbum = null }) { Text(stringResource(R.string.cancel)) } }
                )
            }

            if (showAlbumDelete && editingAlbum != null) {
                val dAlb = editingAlbum!!
                AlertDialog(
                    onDismissRequest = { showAlbumDelete = false; editingAlbum = null },
                    title = { Text("删除专辑") },
                    text = { Text("确定要删除专辑 \"${dAlb.name}\" 中的所有歌曲吗？此操作不可撤销。") },
                    confirmButton = {
                        TextButton(onClick = {
                            val albumId = dAlb.albumId
                            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P &&
                                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                                pendingAlbumDelete = albumId
                                writePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            } else {
                                viewModel.deleteAlbumSongs(albumId)
                            }
                            showAlbumDelete = false; editingAlbum = null
                        }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                    },
                    dismissButton = { TextButton(onClick = { showAlbumDelete = false; editingAlbum = null }) { Text(stringResource(R.string.cancel)) } }
                )
            }
        }
    }
}

// ─── Artists ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    onArtistClick: (String) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_artists)) })
        }
    ) { padding ->
        ArtistList(
            artists = uiState.artists,
            onArtistClick = onArtistClick,
            modifier = Modifier.fillMaxSize().padding(padding)
        )
    }
}

// ─── Playlists ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    onPlaylistClick: (Long) -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var contextMenuPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showEditPlaylistDialog by remember { mutableStateOf(false) }
    var editTargetPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var showDeletePlaylistDialog by remember { mutableStateOf(false) }
    var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }
    val playlistsCtx = LocalContext.current

    val coverPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { srcUri ->
            try {
                val input = playlistsCtx.contentResolver.openInputStream(srcUri)
                val dir = java.io.File(playlistsCtx.filesDir, "playlist_covers")
                dir.mkdirs()
                val destFile = java.io.File(dir, "cover_${System.currentTimeMillis()}.jpg")
                input?.use { inp -> java.io.FileOutputStream(destFile).use { out -> inp.copyTo(out) } }
                pickedCoverUri = Uri.fromFile(destFile)
            } catch (_: Exception) { }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.tab_playlists)) })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true; newPlaylistName = "" }) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_playlist))
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            PlaylistGrid(
                playlists = uiState.playlists,
                onPlaylistClick = onPlaylistClick,
                onPlaylistLongClick = { id, _ ->
                    contextMenuPlaylist = uiState.playlists.find { it.id == id }
                }
            )

            contextMenuPlaylist?.let { pl ->
                AlertDialog(
                    onDismissRequest = { contextMenuPlaylist = null },
                    title = { Text(pl.name) },
                    text = {
                        Column {
                            TextButton(onClick = {
                                editTargetPlaylist = pl
                                showEditPlaylistDialog = true
                                pickedCoverUri = null
                                contextMenuPlaylist = null
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.edit_info_menu))
                            }
                            TextButton(onClick = {
                                editTargetPlaylist = pl
                                showDeletePlaylistDialog = true
                                contextMenuPlaylist = null
                            }, modifier = Modifier.fillMaxWidth()) {
                                Text(stringResource(R.string.delete_menu), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { contextMenuPlaylist = null }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                )
            }

            if (showDeletePlaylistDialog && contextMenuPlaylist == null) {
                val pl = editTargetPlaylist
                if (pl != null) {
                    AlertDialog(
                        onDismissRequest = { showDeletePlaylistDialog = false },
                        title = { Text(stringResource(R.string.delete_playlist_title)) },
                        text = { Text(stringResource(R.string.delete_playlist_confirm, pl.name)) },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.deletePlaylist(pl.id)
                                showDeletePlaylistDialog = false
                                editTargetPlaylist = null
                            }) { Text(stringResource(R.string.delete), color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeletePlaylistDialog = false }) {
                                Text(stringResource(R.string.cancel))
                            }
                        }
                    )
                }
            }

            if (showEditPlaylistDialog && editTargetPlaylist != null) {
                EditPlaylistDialog(
                    playlist = editTargetPlaylist!!,
                    pickedCoverUri = pickedCoverUri?.toString(),
                    onSave = { name, cover ->
                        viewModel.updatePlaylistInfo(editTargetPlaylist!!.id, name, null, cover)
                        showEditPlaylistDialog = false
                        editTargetPlaylist = null
                        pickedCoverUri = null
                    },
                    onPickCover = { coverPicker.launch("image/*") },
                    onDismiss = {
                        showEditPlaylistDialog = false
                        editTargetPlaylist = null
                        pickedCoverUri = null
                    }
                )
            }

            if (showCreateDialog) {
                AlertDialog(
                    onDismissRequest = { showCreateDialog = false; newPlaylistName = "" },
                    title = { Text(stringResource(R.string.create_playlist)) },
                    text = {
                        TextField(
                            value = newPlaylistName,
                            onValueChange = { newPlaylistName = it },
                            placeholder = { Text(stringResource(R.string.playlist_name)) },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.createPlaylist(newPlaylistName)
                            showCreateDialog = false
                            newPlaylistName = ""
                        }) { Text(stringResource(R.string.create)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCreateDialog = false; newPlaylistName = "" }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        }
    }
}
