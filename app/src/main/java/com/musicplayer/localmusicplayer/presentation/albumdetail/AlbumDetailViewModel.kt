package com.musicplayer.localmusicplayer.presentation.albumdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val albumName: String = "",
    val artistName: String = "",
    val songs: List<Song> = emptyList(),
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    private val albumId: Long = savedStateHandle.get<Long>("albumId") ?: -1L

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

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
    }

    fun playSong(song: Song) { playSongUseCase(song, _uiState.value.songs) }
    fun playAll() { val s = _uiState.value.songs; if (s.isNotEmpty()) playSongUseCase(s.first(), s) }
    fun updateSong(song: Song) { viewModelScope.launch { musicRepository.updateSongMetadata(song) } }
    fun deleteSong(song: Song) { viewModelScope.launch { musicRepository.deleteSongFile(song) } }
    fun addSongToPlaylist(plId: Long, songId: Long) { viewModelScope.launch { playlistRepository.addSongToPlaylist(plId, songId) } }
}
