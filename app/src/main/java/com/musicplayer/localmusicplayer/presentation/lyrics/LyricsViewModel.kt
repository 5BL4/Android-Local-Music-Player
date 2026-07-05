package com.musicplayer.localmusicplayer.presentation.lyrics

import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.data.remote.LyricsRemoteDataSource
import com.musicplayer.localmusicplayer.data.remote.SearchResult
import com.musicplayer.localmusicplayer.domain.model.LyricLine
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.LyricsLocalDataSource
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.util.AudioTagManager
import com.musicplayer.localmusicplayer.util.LrcParser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LyricsUiState(
    val lyrics: List<LyricLine> = emptyList(),
    val currentLineIndex: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null,
    // Scrub (seek-by-scroll): user is manually scrubbing the lyrics list
    val isScrubbing: Boolean = false,
    val scrubLineIndex: Int? = null,
    // Search
    val searchQuery: String = "",
    val searchSource: String = "netease",
    val searchResults: List<SearchResult> = emptyList(),
    val isSearching: Boolean = false,
    val searchError: String? = null,
    val showSearchPanel: Boolean = false,
    // Embed
    val isEmbedding: Boolean = false,
    val embedSuccess: Boolean = false,
    val embedMessage: String? = null,
    // Preview
    val previewLyric: String? = null,
    val previewTlyric: String? = null,
    val showPreview: Boolean = false,
    val previewSong: SearchResult? = null
)

val SOURCES = listOf(
    "netease" to "网易云",
    "tencent" to "QQ音乐",
    "kuwo" to "酷我",
    "spotify" to "Spotify",
    "apple" to "Apple Music"
)

// Buffer time (ms) before auto-clearing a backward scrub so the user has time
// to tap the seek pill before auto-scroll snaps back to the current line.
private const val SCRUB_TIMEOUT_MS = 4000L

@HiltViewModel
class LyricsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val musicRepository: MusicRepository,
    private val lyricsLocalDataSource: LyricsLocalDataSource,
    private val remoteDataSource: LyricsRemoteDataSource,
    private val audioTagManager: AudioTagManager
) : ViewModel() {

    private val songId: Long = savedStateHandle.get<Long>("songId") ?: 0L
    private var currentSong: Song? = null

    // Last time the user dragged the lyrics list, used to give a
    // buffer before auto-clearing backward scrubs (see position collector).
    private var lastScrubTimeMs: Long = 0L

    // Cache of the last position emitted by the collector. The lyrics-loading
    // success path uses this to compute currentLineIndex immediately — the
    // position collector's initial StateFlow emission arrives before lyrics
    // are loaded and is skipped by the isNotEmpty() guard, so without this
    // cache the highlight stays at 0 when playback is paused (no further
    // emissions arrive to correct it).
    private var lastKnownPosition: Long = 0L

    private val _uiState = MutableStateFlow(LyricsUiState())
    val uiState: StateFlow<LyricsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val song = musicRepository.getSongById(songId)
            currentSong = song
            if (song != null) {
                _uiState.update { it.copy(searchQuery = song.title) }
                lyricsLocalDataSource.findAndParse(song)
                    .onSuccess { lyrics ->
                        val idx = lyrics.indexOfLast { it.timestampMs <= lastKnownPosition }
                        _uiState.update {
                            it.copy(
                                lyrics = lyrics,
                                isLoading = false,
                                currentLineIndex = idx.coerceAtLeast(0)
                            )
                        }
                    }
                    .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "No lyrics found", isLoading = false) } }
            } else {
                _uiState.update { it.copy(error = "Song not found", isLoading = false) }
            }
        }
        viewModelScope.launch {
            musicRepository.currentPosition.collect { position ->
                lastKnownPosition = position
                val state = _uiState.value
                val lyrics = state.lyrics
                if (lyrics.isNotEmpty()) {
                    val idx = lyrics.indexOfLast { it.timestampMs <= position }
                    val newCurrent = idx.coerceAtLeast(0)
                    // Auto-clear scrubbing so auto-scroll can resume. The logic is
                    // direction-aware:
                    //  - Forward scrub (target ahead of playback): never clear
                    //    while still ahead; once playback catches up the target
                    //    is no longer ahead and the timeout branch fires — by
                    //    then the 4 s buffer has long expired, so it clears at
                    //    once (preserving instant catch-up for forward scrubs).
                    //  - Backward / at-current scrub (target at or behind
                    //    playback): a position-based check is meaningless
                    //    (playback is already past the target), so use a
                    //    timeout from the last drag frame to give the user
                    //    time to tap the seek pill.
                    val scrubIdx = state.scrubLineIndex
                    val now = SystemClock.elapsedRealtime()
                    val shouldClearScrub = state.isScrubbing &&
                        scrubIdx != null &&
                        if (scrubIdx > newCurrent) false
                        else now - lastScrubTimeMs > SCRUB_TIMEOUT_MS
                    _uiState.update {
                        it.copy(
                            currentLineIndex = newCurrent,
                            isScrubbing = if (shouldClearScrub) false else it.isScrubbing,
                            scrubLineIndex = if (shouldClearScrub) null else it.scrubLineIndex
                        )
                    }
                }
            }
        }
    }

    fun enterScrubMode(lineIndex: Int) {
        val lyrics = _uiState.value.lyrics
        if (lyrics.isEmpty()) return
        val safeIndex = lineIndex.coerceIn(lyrics.indices)
        lastScrubTimeMs = SystemClock.elapsedRealtime()
        _uiState.update { it.copy(isScrubbing = true, scrubLineIndex = safeIndex) }
    }

    fun seekTo(positionMs: Long) {
        musicRepository.seekTo(positionMs)
        _uiState.update { it.copy(isScrubbing = false, scrubLineIndex = null) }
    }

    fun setShowSearchPanel(show: Boolean) {
        _uiState.update { it.copy(showSearchPanel = show, searchResults = emptyList(), searchError = null) }
    }

    fun onSearchQueryChanged(q: String) {
        _uiState.update { it.copy(searchQuery = q) }
    }

    fun onSourceChanged(src: String) {
        _uiState.update { it.copy(searchSource = src) }
    }

    fun search() {
        val query = _uiState.value.searchQuery.trim()
        if (query.isEmpty()) return
        val source = _uiState.value.searchSource
        _uiState.update { it.copy(isSearching = true, searchError = null, searchResults = emptyList()) }
        viewModelScope.launch {
            remoteDataSource.search(source, query)
                .onSuccess { results ->
                    _uiState.update { it.copy(isSearching = false, searchResults = results) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isSearching = false, searchError = e.message ?: "Search failed") }
                }
        }
    }

    fun onResultClicked(result: SearchResult) {
        val source = _uiState.value.searchSource
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            remoteDataSource.getLyric(source, result.id)
                .onSuccess { rsp ->
                    if (rsp.lyric != null) {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                previewLyric = rsp.lyric,
                                previewTlyric = rsp.tlyric,
                                previewSong = result,
                                showPreview = true
                            )
                        }
                    } else {
                        _uiState.update { it.copy(isLoading = false, searchError = "No lyrics found for this song") }
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, searchError = e.message ?: "Failed to load lyrics") }
                }
        }
    }

    fun hidePreview() {
        _uiState.update { it.copy(showPreview = false, previewLyric = null, previewTlyric = null, previewSong = null) }
    }

    fun embedLyrics() {
        val lrc = _uiState.value.previewLyric ?: return
        val song = currentSong ?: return
        _uiState.update { it.copy(isEmbedding = true, embedMessage = null) }
        viewModelScope.launch {
            // Combine with translation if available
            val content = if (_uiState.value.previewTlyric != null) {
                lrc + "\n\n" + _uiState.value.previewTlyric!!
            } else lrc
            val ok = audioTagManager.embedLyrics(song, content)
            if (ok) {
                val lines = LrcParser.parseSyncedPair(lrc, _uiState.value.previewTlyric)
                val newLyrics = lines.map { LyricLine(it.timeMs, it.text) }
                val idx = newLyrics.indexOfLast { it.timestampMs <= lastKnownPosition }
                _uiState.update {
                    it.copy(
                        isEmbedding = false, embedSuccess = true,
                        embedMessage = "歌词已保存",
                        lyrics = newLyrics,
                        currentLineIndex = idx.coerceAtLeast(0),
                        showPreview = false, showSearchPanel = false, error = null
                    )
                }
            } else {
                _uiState.update { it.copy(isEmbedding = false, embedMessage = "保存失败") }
            }
        }
    }
}
