package com.musicplayer.localmusicplayer.presentation.artistdetail

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import com.musicplayer.localmusicplayer.presentation.navigation.Screen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ArtistDetailUiState(
    val artistName: String = "",
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ArtistDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    private val artistName: String = savedStateHandle.get<String>("artistName")
        ?.let { Screen.ArtistDetail.decodeName(it) } ?: ""

    private val _uiState = MutableStateFlow(ArtistDetailUiState(artistName = artistName))
    val uiState: StateFlow<ArtistDetailUiState> = _uiState.asStateFlow()

    init {
        Log.d("ArtistDetailVM", "Loading artist: '$artistName'")
        if (artistName.isBlank()) {
            viewModelScope.launch { _uiState.update { it.copy(isLoading = false, error = "No artist specified") } }
        } else {
            viewModelScope.launch {
            musicRepository.getSongsByArtist(artistName)
                .catch { e ->
                    Log.e("ArtistDetailVM", "Error loading songs for artist '$artistName'", e)
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    emit(emptyList())
                }
                .collect { songs ->
                    Log.d("ArtistDetailVM", "Found ${songs.size} songs for artist '$artistName'")
                    val albums = songs
                        .groupBy { it.albumId }
                        .map { (albumId, albumSongs) ->
                            val first = albumSongs.first()
                            Album(
                                albumId = albumId,
                                name = first.album,
                                artist = first.artist,
                                albumArtUri = first.albumArtUri,
                                songCount = albumSongs.size
                            )
                        }
                        .sortedBy { it.name }
                    Log.d("ArtistDetailVM", "Derived ${albums.size} albums")
                    _uiState.update {
                        it.copy(albums = albums, songs = songs, isLoading = false)
                    }
                }
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
}
