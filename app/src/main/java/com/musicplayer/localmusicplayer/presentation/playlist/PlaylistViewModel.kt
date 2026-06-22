package com.musicplayer.localmusicplayer.presentation.playlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.usecase.CreatePlaylistUseCase
import com.musicplayer.localmusicplayer.domain.usecase.DeletePlaylistUseCase
import com.musicplayer.localmusicplayer.domain.usecase.UpdatePlaylistUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlaylistUiState(
    val playlists: List<Playlist> = emptyList(),
    val showCreateDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val deleteTargetId: Long = 0L,
    val deleteTargetName: String = "",
    val newPlaylistName: String = ""
)

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
    private val createPlaylistUseCase: CreatePlaylistUseCase,
    private val updatePlaylistUseCase: UpdatePlaylistUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlaylistUiState())
    val uiState: StateFlow<PlaylistUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            playlistRepository.playlists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }
    }

    fun showCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, newPlaylistName = "") }
    }

    fun hideCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, newPlaylistName = "") }
    }

    fun onNewPlaylistNameChanged(name: String) {
        _uiState.update { it.copy(newPlaylistName = name) }
    }

    fun createPlaylist() {
        val name = _uiState.value.newPlaylistName.trim()
        if (name.isNotEmpty()) {
            viewModelScope.launch {
                createPlaylistUseCase(name)
                hideCreateDialog()
            }
        }
    }

    fun showDeleteDialog(playlistId: Long, name: String) {
        _uiState.update { it.copy(showDeleteDialog = true, deleteTargetId = playlistId, deleteTargetName = name) }
    }

    fun hideDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false, deleteTargetId = 0L, deleteTargetName = "") }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch {
            try {
                deletePlaylistUseCase(playlistId)
                Log.d("PlaylistVM", "Deleted playlist $playlistId")
            } catch (e: Exception) {
                Log.e("PlaylistVM", "Failed to delete playlist $playlistId", e)
            }
            hideDeleteDialog()
        }
    }
}
