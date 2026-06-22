package com.musicplayer.localmusicplayer.presentation.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Artist
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

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val musicRepository: MusicRepository,
    private val playlistRepository: PlaylistRepository,
    private val themeRepository: ThemeRepository,
    private val scanMusicFilesUseCase: ScanMusicFilesUseCase,
    private val playSongUseCase: PlaySongUseCase,
    private val deletePlaylistUseCase: DeletePlaylistUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

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
        scanMusic()
    }

    fun scanMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            scanMusicFilesUseCase()
            _uiState.update { it.copy(isScanning = false) }
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
        // For Paging mode, pass empty queue — the player will populate from current view
        playSongUseCase(song, emptyList())
    }

    fun deleteSongFile(song: Song) {
        viewModelScope.launch { musicRepository.deleteSongFile(song) }
    }

    fun updateSongMetadata(song: Song) {
        viewModelScope.launch { musicRepository.updateSongMetadata(song) }
    }

    fun deletePlaylist(playlistId: Long) {
        viewModelScope.launch { deletePlaylistUseCase(playlistId) }
    }

    fun updatePlaylistInfo(id: Long, name: String, description: String?, coverUri: String?) {
        viewModelScope.launch { playlistRepository.updatePlaylistInfo(id, name, description, coverUri) }
    }

    fun addSongToPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.addSongToPlaylist(playlistId, songId) }
    }

    fun updateAlbumInfo(albumId: Long, newAlbum: String, newArtist: String) {
        viewModelScope.launch { musicRepository.updateAlbumInfo(albumId, newAlbum, newArtist) }
    }

    fun deleteAlbumSongs(albumId: Long, songs: List<Song>) {
        viewModelScope.launch { musicRepository.deleteAlbumSongs(albumId, songs) }
    }
}
