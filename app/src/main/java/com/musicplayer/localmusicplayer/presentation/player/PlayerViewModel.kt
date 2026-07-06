package com.musicplayer.localmusicplayer.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.model.WaveformStyle
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.repository.ThemeRepository
import com.musicplayer.localmusicplayer.domain.usecase.ControlPlaybackUseCase
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PlayerUiState(
    val playbackState: PlaybackState = PlaybackState.Idle,
    val currentPositionMs: Long = 0L,
    val playlists: List<Playlist> = emptyList(),
    val showAddToPlaylistDialog: Boolean = false,
    val amplitudes: List<Float> = emptyList(),
    val waveformStyle: WaveformStyle = WaveformStyle.MirroredBars
)

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val themeRepository: ThemeRepository,
    private val controlPlaybackUseCase: ControlPlaybackUseCase,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(PlayerUiState())
    val uiState: StateFlow<PlayerUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            musicRepository.playbackState.collect { state ->
                _uiState.update { it.copy(playbackState = state) }
            }
        }

        viewModelScope.launch {
            musicRepository.currentPosition.collect { pos ->
                _uiState.update { it.copy(currentPositionMs = pos) }
            }
        }

        viewModelScope.launch {
            playlistRepository.playlists.collect { playlists ->
                _uiState.update { it.copy(playlists = playlists) }
            }
        }

        viewModelScope.launch {
            musicRepository.amplitudes.collect { amplitudes ->
                _uiState.update { it.copy(amplitudes = amplitudes) }
            }
        }

        viewModelScope.launch {
            themeRepository.waveformStyle.collect { style ->
                _uiState.update { it.copy(waveformStyle = style) }
            }
        }
    }

    fun togglePlayPause() = controlPlaybackUseCase.togglePlayPause()
    fun skipToNext() = controlPlaybackUseCase.skipToNext()
    fun skipToPrevious() = controlPlaybackUseCase.skipToPrevious()
    fun seekTo(positionMs: Long) = controlPlaybackUseCase.seekTo(positionMs)
    fun setRepeatMode(mode: RepeatMode) = controlPlaybackUseCase.setRepeatMode(mode)
    fun toggleShuffle() = controlPlaybackUseCase.toggleShuffle()

    fun playSong(song: Song, queue: List<Song>) {
        playSongUseCase(song, queue)
    }

    fun showAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = true) }
    }

    fun hideAddToPlaylistDialog() {
        _uiState.update { it.copy(showAddToPlaylistDialog = false) }
    }

    fun createAndAddToPlaylist(name: String, songId: Long) {
        viewModelScope.launch {
            try {
                val playlistId = playlistRepository.createPlaylist(name)
                playlistRepository.addSongToPlaylist(playlistId, songId)
            } catch (e: Exception) {
                // silent fail
            }
        }
    }

    fun addToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch {
            try {
                playlistRepository.addSongToPlaylist(playlistId, songId)
            } catch (e: Exception) {
                // silent fail
            }
        }
    }
}
