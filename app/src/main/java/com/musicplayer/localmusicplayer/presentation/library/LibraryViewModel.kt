package com.musicplayer.localmusicplayer.presentation.library

import android.content.IntentSender
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.musicplayer.localmusicplayer.data.deletion.MediaStoreDeleteManager
import com.musicplayer.localmusicplayer.data.edit.MediaStoreEditManager
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Artist
import com.musicplayer.localmusicplayer.domain.model.DeleteResult
import com.musicplayer.localmusicplayer.domain.model.EditResult
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.repository.ThemeRepository
import com.musicplayer.localmusicplayer.domain.usecase.DeletePlaylistUseCase
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import com.musicplayer.localmusicplayer.domain.usecase.ScanMusicFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LibraryUiState(
    val isLoading: Boolean = true,
    val isScanning: Boolean = false,
    val albums: List<Album> = emptyList(),
    val artists: List<Artist> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val sortOption: SortOption = SortOption.Title,
    val selectedTab: LibraryTab = LibraryTab.Songs,
    val searchQuery: String = ""
)

enum class LibraryTab { Songs, Albums, Artists, Playlists }

sealed class DeleteEvent {
    object Deleted : DeleteEvent()
    object Failed : DeleteEvent()
    object Cancelled : DeleteEvent()
}

sealed class EditEvent {
    object Saved : EditEvent()
    object Failed : EditEvent()
    object Cancelled : EditEvent()
}

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val themeRepository: ThemeRepository,
    private val scanMusicFilesUseCase: ScanMusicFilesUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase,
    private val deleteManager: MediaStoreDeleteManager,
    private val editManager: MediaStoreEditManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    private val _deleteConfirmation = MutableStateFlow<Long?>(null)
    val deleteConfirmation: StateFlow<Long?> = _deleteConfirmation.asStateFlow()

    private val _deleteEvent = MutableSharedFlow<DeleteEvent>()
    val deleteEvent: SharedFlow<DeleteEvent> = _deleteEvent.asSharedFlow()

    private val _editConfirmation = MutableStateFlow<Long?>(null)
    val editConfirmation: StateFlow<Long?> = _editConfirmation.asStateFlow()

    private val _editEvent = MutableSharedFlow<EditEvent>()
    val editEvent: SharedFlow<EditEvent> = _editEvent.asSharedFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val songPagingFlow: Flow<PagingData<Song>> =
        _uiState.flatMapLatest { state ->
            musicRepository.getSongsPaged(state.sortOption, state.searchQuery)
        }.cachedIn(viewModelScope)

    init {
        viewModelScope.launch {
            songPagingFlow.collect { _uiState.update { it.copy(isLoading = false) } }
        }
        viewModelScope.launch {
            musicRepository.albums.collect { albums ->
                _uiState.update { it.copy(albums = albums) }
            }
        }
        viewModelScope.launch {
            musicRepository.artists.collect { artists ->
                _uiState.update { it.copy(artists = artists) }
            }
        }
        viewModelScope.launch {
            playlistRepository.playlists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
        viewModelScope.launch {
            themeRepository.defaultSortOption.collect { sortOption ->
                _uiState.update { it.copy(sortOption = sortOption) }
            }
        }
    }

    fun scanMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            try {
                scanMusicFilesUseCase()
            } catch (e: Exception) {
                android.util.Log.e("LibraryViewModel", "Music scan failed", e)
            } finally {
                _uiState.update { it.copy(isScanning = false) }
            }
        }
    }

    fun onTabSelected(tab: LibraryTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onSortOptionSelected(option: SortOption) {
        viewModelScope.launch { themeRepository.setDefaultSortOption(option) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            // Paging 3 does not expose the full song list, so query the DB for the
            // complete sorted queue to enable next/previous navigation.
            val queue = musicRepository.getSortedSongs(_uiState.value.sortOption, _uiState.value.searchQuery)
            playSongUseCase(song, queue.ifEmpty { listOf(song) })
        }
    }

    fun deleteSongFile(song: Song) {
        viewModelScope.launch {
            when (val r = musicRepository.deleteSongFile(song)) {
                is DeleteResult.NeedsConfirmation -> _deleteConfirmation.value = r.requestId
                DeleteResult.Success -> _deleteEvent.emit(DeleteEvent.Deleted)
                DeleteResult.Error -> _deleteEvent.emit(DeleteEvent.Failed)
            }
        }
    }

    fun updateSongMetadata(song: Song) {
        viewModelScope.launch {
            try {
                when (val r = musicRepository.updateSongMetadata(song)) {
                    is EditResult.NeedsConfirmation -> _editConfirmation.value = r.requestId
                    EditResult.Success -> _editEvent.emit(EditEvent.Saved)
                    EditResult.Error -> _editEvent.emit(EditEvent.Failed)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("LibraryViewModel", "updateSongMetadata failed", e)
                _editEvent.emit(EditEvent.Failed)
            }
        }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { deletePlaylistUseCase(playlistId) }
    }

    fun createPlaylist(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            viewModelScope.launch { playlistRepository.createPlaylist(trimmed) }
        }
    }

    fun updatePlaylistInfo(id: Long, name: String, description: String?, coverUri: String?) {
        viewModelScope.launch { playlistRepository.updatePlaylistInfo(id, name, description, coverUri) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.addSongToPlaylist(playlistId, songId) }
    }

    fun updateAlbumInfo(albumId: Long, newAlbum: String, newArtist: String) {
        viewModelScope.launch {
            try {
                when (val r = musicRepository.updateAlbumInfo(albumId, newAlbum, newArtist)) {
                    is EditResult.NeedsConfirmation -> _editConfirmation.value = r.requestId
                    EditResult.Success -> _editEvent.emit(EditEvent.Saved)
                    EditResult.Error -> _editEvent.emit(EditEvent.Failed)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("LibraryViewModel", "updateAlbumInfo failed", e)
                _editEvent.emit(EditEvent.Failed)
            }
        }
    }

    fun deleteAlbumSongs(albumId: Long) {
        viewModelScope.launch {
            when (val r = musicRepository.deleteAlbumSongs(albumId)) {
                is DeleteResult.NeedsConfirmation -> _deleteConfirmation.value = r.requestId
                DeleteResult.Success -> _deleteEvent.emit(DeleteEvent.Deleted)
                DeleteResult.Error -> _deleteEvent.emit(DeleteEvent.Failed)
            }
        }
    }

    fun onDeleteDialogResult(requestId: Long, success: Boolean) {
        viewModelScope.launch {
            if (success) {
                musicRepository.commitDelete(requestId)
                val moreSenders = deleteManager.hasMoreSenders(requestId)
                if (moreSenders) {
                    // keep _deleteConfirmation, UI will launch next sender
                } else {
                    _deleteConfirmation.value = null
                    _deleteEvent.emit(DeleteEvent.Deleted)
                }
            } else {
                musicRepository.cancelDelete(requestId)
                _deleteConfirmation.value = null
                _deleteEvent.emit(DeleteEvent.Cancelled)
            }
        }
    }

    fun intentSenderForConfirmation(): IntentSender? {
        val reqId = _deleteConfirmation.value ?: return null
        return deleteManager.intentSenderFor(reqId)
    }

    fun onEditDialogResult(requestId: Long, success: Boolean) {
        viewModelScope.launch {
            try {
                if (success) {
                    musicRepository.commitEdit(requestId)
                    val moreSenders = editManager.hasMoreSenders(requestId)
                    if (moreSenders) {
                        // keep _editConfirmation, UI will launch next sender
                    } else {
                        _editConfirmation.value = null
                        _editEvent.emit(EditEvent.Saved)
                    }
                } else {
                    musicRepository.cancelEdit(requestId)
                    _editConfirmation.value = null
                    _editEvent.emit(EditEvent.Cancelled)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                android.util.Log.e("LibraryViewModel", "onEditDialogResult failed", e)
                _editConfirmation.value = null
                _editEvent.emit(EditEvent.Failed)
            }
        }
    }

    fun intentSenderForEditConfirmation(): IntentSender? {
        val reqId = _editConfirmation.value ?: return null
        return editManager.intentSenderFor(reqId)
    }
}
