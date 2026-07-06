package com.musicplayer.localmusicplayer.presentation.albumdetail

import android.content.IntentSender
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.data.deletion.MediaStoreDeleteManager
import com.musicplayer.localmusicplayer.data.edit.MediaStoreEditManager
import com.musicplayer.localmusicplayer.domain.model.DeleteResult
import com.musicplayer.localmusicplayer.domain.model.EditResult
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import com.musicplayer.localmusicplayer.presentation.library.DeleteEvent
import com.musicplayer.localmusicplayer.presentation.library.EditEvent
import com.musicplayer.localmusicplayer.service.PlaybackManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumName: String = "",
    val artistName: String = "",
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
    val currentSongId: Long? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playSongUseCase: PlaySongUseCase,
    private val deleteManager: MediaStoreDeleteManager,
    private val editManager: MediaStoreEditManager,
    private val playbackManager: PlaybackManager
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: -1L

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private val _deleteConfirmation = MutableStateFlow<Long?>(null)
    val deleteConfirmation: StateFlow<Long?> = _deleteConfirmation.asStateFlow()

    private val _deleteEvent = MutableSharedFlow<DeleteEvent>()
    val deleteEvent: SharedFlow<DeleteEvent> = _deleteEvent.asSharedFlow()

    private val _editConfirmation = MutableStateFlow<Long?>(null)
    val editConfirmation: StateFlow<Long?> = _editConfirmation.asStateFlow()

    private val _editEvent = MutableSharedFlow<EditEvent>()
    val editEvent: SharedFlow<EditEvent> = _editEvent.asSharedFlow()

    init {
        if (albumId < 0) {
            viewModelScope.launch { _uiState.update { it.copy(isLoading = false) } }
        } else {
            viewModelScope.launch {
                musicRepository.getSongsByAlbum(albumId)
                    .catch { e ->
                        Log.e("AlbumDetailVM", "Error loading album", e)
                        emit(emptyList())
                    }
                    .collect { songs ->
                        _uiState.update { it.copy(
                            albumName = songs.firstOrNull()?.album ?: "Unknown Album",
                            artistName = songs.firstOrNull()?.artist ?: "Unknown Artist",
                            songs = songs,
                            isLoading = false
                        ) }
                    }
            }
        }
        viewModelScope.launch {
            playlistRepository.playlists.collect { pls -> _uiState.update { it.copy(playlists = pls) } }
        }
        viewModelScope.launch {
            playbackManager.playbackState.collect { state ->
                val songId = when (state) {
                    is PlaybackState.Playing -> state.currentSong.id
                    is PlaybackState.Paused -> state.currentSong.id
                    else -> null
                }
                _uiState.update { it.copy(currentSongId = songId) }
            }
        }
    }

    fun playSong(song: Song) { playSongUseCase(song, _uiState.value.songs) }
    fun playAll() { val s = _uiState.value.songs; if (s.isNotEmpty()) playSongUseCase(s.first(), s) }
    fun updateSong(song: Song) {
        viewModelScope.launch {
            try {
                when (val r = musicRepository.updateSongMetadata(song)) {
                    is EditResult.NeedsConfirmation -> _editConfirmation.value = r.requestId
                    EditResult.Success -> _editEvent.emit(EditEvent.Saved)
                    EditResult.Error -> _editEvent.emit(EditEvent.Failed)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                Log.e("AlbumDetailVM", "updateSong failed", e)
                _editEvent.emit(EditEvent.Failed)
            }
        }
    }
    fun deleteSong(song: Song) {
        viewModelScope.launch {
            when (val r = musicRepository.deleteSongFile(song)) {
                is DeleteResult.NeedsConfirmation -> _deleteConfirmation.value = r.requestId
                DeleteResult.Success -> _deleteEvent.emit(DeleteEvent.Deleted)
                DeleteResult.Error -> _deleteEvent.emit(DeleteEvent.Failed)
            }
        }
    }
    fun addSongToPlaylist(plId: Long, songId: Long) { viewModelScope.launch { playlistRepository.addSongToPlaylist(plId, songId) } }

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
                Log.e("AlbumDetailVM", "onEditDialogResult failed", e)
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
