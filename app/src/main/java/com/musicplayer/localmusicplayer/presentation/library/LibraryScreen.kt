package com.musicplayer.localmusicplayer.presentation.library

import androidx.activity.compose.rememberLauncherForActivityResult
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.presentation.library.components.AlbumBottomSheet
import com.musicplayer.localmusicplayer.presentation.library.components.AlbumGrid
import com.musicplayer.localmusicplayer.presentation.library.components.ArtistList
import com.musicplayer.localmusicplayer.presentation.library.components.EditPlaylistDialog
import com.musicplayer.localmusicplayer.presentation.library.components.EditSongDialog
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.presentation.library.components.PagingSongList
import com.musicplayer.localmusicplayer.presentation.library.components.PlaylistGrid
import com.musicplayer.localmusicplayer.presentation.library.components.SongBottomSheet
import com.musicplayer.localmusicplayer.presentation.library.components.SongList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onPlayerOpen: () -> Unit,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
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
                    Text(stringResource(R.string.music_library))
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

        TabRow(selectedTabIndex = uiState.selectedTab.ordinal) {
            LibraryTab.entries.forEach { tab ->
                Tab(
                    selected = uiState.selectedTab == tab,
                    onClick = { viewModel.onTabSelected(tab) },
                    text = {
                        Text(
                            when (tab) {
                                LibraryTab.Songs -> stringResource(R.string.tab_songs)
                                LibraryTab.Albums -> stringResource(R.string.tab_albums)
                                LibraryTab.Artists -> stringResource(R.string.tab_artists)
                                LibraryTab.Playlists -> stringResource(R.string.tab_playlists)
                            }
                        )
                    }
                )
            }
        }

        when (uiState.selectedTab) {
            LibraryTab.Songs -> {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize()) {
                        CircularProgressIndicator(Modifier.wrapContentSize())
                    }
                } else {
                    var contextMenuSong by remember { mutableStateOf<Song?>(null) }
                    var editingSong by remember { mutableStateOf<Song?>(null) }
                    var showEditDialog by remember { mutableStateOf(false) }
                    var showDeleteDialog by remember { mutableStateOf(false) }
                    var pickedAlbumArt by remember { mutableStateOf<String?>(null) }

                    val albumArtPicker = rememberLauncherForActivityResult(
                        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
                    ) { uri -> pickedAlbumArt = uri?.toString() }

                    PagingSongList(
                        pagingFlow = viewModel.songPagingFlow,
                        onSongClick = { song ->
                            viewModel.playSong(song)
                            onPlayerOpen()
                        },
                        onSongMenuClick = { song -> contextMenuSong = song }
                    )

                    // Bottom sheet
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

                    // Edit dialog
                    if (showEditDialog && editingSong != null) {
                        EditSongDialog(
                            song = editingSong!!,
                            onSave = { edited ->
                                viewModel.updateSongMetadata(edited)
                                showEditDialog = false
                                editingSong = null
                                pickedAlbumArt = null
                            },
                            onDismiss = { showEditDialog = false; editingSong = null; pickedAlbumArt = null },
                            onPickAlbumArt = { albumArtPicker.launch(androidx.activity.result.PickVisualMediaRequest(androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                            pickedAlbumArtUri = pickedAlbumArt
                        )
                    }

                    // Delete confirmation
                    if (showDeleteDialog && editingSong != null) {
                        AlertDialog(
                            onDismissRequest = { showDeleteDialog = false; editingSong = null },
                            title = { Text(stringResource(R.string.delete_file)) },
                            text = { Text(stringResource(R.string.delete_file_confirm, editingSong!!.title)) },
                            confirmButton = {
                                TextButton(onClick = {
                                    editingSong?.let { viewModel.deleteSongFile(it) }
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
            LibraryTab.Albums -> {
                var albumSheet by remember { mutableStateOf<Album?>(null) }
                var editingAlbum by remember { mutableStateOf<Album?>(null) }
                var showAlbumEdit by remember { mutableStateOf(false) }
                var showAlbumDelete by remember { mutableStateOf(false) }
                var editAlbumName by remember { mutableStateOf("") }
                var editAlbumArtist by remember { mutableStateOf("") }
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
                        confirmButton = { TextButton(onClick = { viewModel.updateAlbumInfo(eAlb.albumId, editAlbumName, editAlbumArtist); showAlbumEdit = false; editingAlbum = null }) { Text("保存") } },
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
                                viewModel.deleteAlbumSongs(dAlb.albumId, emptyList())
                                showAlbumDelete = false; editingAlbum = null
                            }) { Text("删除", color = MaterialTheme.colorScheme.error) }
                        },
                        dismissButton = { TextButton(onClick = { showAlbumDelete = false; editingAlbum = null }) { Text(stringResource(R.string.cancel)) } }
                    )
                }
            }
            LibraryTab.Artists -> {
                ArtistList(
                    artists = uiState.artists,
                    onArtistClick = onArtistClick
                )
            }
            LibraryTab.Playlists -> {
                var contextMenuPlaylist by remember { mutableStateOf<Playlist?>(null) }
                var showEditPlaylistDialog by remember { mutableStateOf(false) }
                var editTargetPlaylist by remember { mutableStateOf<Playlist?>(null) }
                var showDeletePlaylistDialog by remember { mutableStateOf(false) }
                var pickedCoverUri by remember { mutableStateOf<Uri?>(null) }
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

                Box {
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
                }
                // Delete dialog
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
                // Edit dialog
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
            }
        }
    }
}
