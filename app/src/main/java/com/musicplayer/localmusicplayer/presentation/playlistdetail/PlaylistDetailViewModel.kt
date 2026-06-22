package com.musicplayer.localmusicplayer.presentation.playlistdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.usecase.ManagePlaylistSongsUseCase
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import com.musicplayer.localmusicplayer.domain.usecase.UpdatePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistDetailUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val editName: String = ""
)

@HiltViewModel
class PlaylistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
    private val updatePlaylistUseCase: UpdatePlaylistUseCase,
    private val managePlaylistSongsUseCase: ManagePlaylistSongsUseCase,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    private val playlistId: Long = savedStateHandle.get<Long>("playlistId") ?: 0L

    private val _uiState = MutableStateFlow(PlaylistDetailUiState())
    val uiState: StateFlow<PlaylistDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.getPlaylistById(playlistId)?.let { playlist ->
                _uiState.update { it.copy(playlist = playlist, editName = playlist.name) }
            }
        }
        viewModelScope.launch {
            playlistRepository.getSongsInPlaylist(playlistId).collect { songs ->
                _uiState.update { it.copy(songs = songs, isLoading = false) }
            }
        }
    }

    fun playSong(song: Song) {
        playSongUseCase(song, _uiState.value.songs)
    }

    fun playAll() {
        val songs = _uiState.value.songs
        if (songs.isNotEmpty()) {
            playSongUseCase(songs.first(), songs)
        }
    }

    fun removeSong(songId: Long) {
        viewModelScope.launch {
            managePlaylistSongsUseCase.removeSong(playlistId, songId)
        }
    }

    fun reorderSongs(songIds: List<Long>) {
        viewModelScope.launch {
            playlistRepository.reorderPlaylist(playlistId, songIds)
        }
    }

    fun startEditing() {
        _uiState.update { it.copy(isEditing = true) }
    }

    fun onEditNameChanged(name: String) {
        _uiState.update { it.copy(editName = name) }
    }

    fun saveEdit() {
        val playlist = _uiState.value.playlist ?: return
        val newName = _uiState.value.editName.trim()
        if (newName.isNotEmpty()) {
            viewModelScope.launch {
                updatePlaylistUseCase(playlist.copy(name = newName))
                _uiState.update { it.copy(isEditing = false) }
            }
        }
    }
}
