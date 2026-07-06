package com.musicplayer.localmusicplayer.domain.repository

import androidx.paging.PagingData
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Artist
import com.musicplayer.localmusicplayer.domain.model.DeleteResult
import com.musicplayer.localmusicplayer.domain.model.EditResult
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.model.SortOption
import kotlinx.coroutines.flow.Flow

interface MusicRepository {
    val songs: Flow<List<Song>>
    // Paging 3 API — use for large libraries
    fun getSongsPaged(sort: SortOption = SortOption.Title, query: String = ""): Flow<PagingData<Song>>
    val albums: Flow<List<Album>>
    val artists: Flow<List<Artist>>
    val playbackState: Flow<PlaybackState>
    val currentPosition: Flow<Long>

    suspend fun scanMusicFiles()

    suspend fun getSongsByIds(ids: List<Long>): List<Song>
    suspend fun getSongById(id: Long): Song?
    fun getSongsByAlbum(albumId: Long): Flow<List<Song>>
    fun getSongsByArtist(artist: String): Flow<List<Song>>
    fun searchSongs(query: String): Flow<List<Song>>

    suspend fun getSortedSongs(sort: SortOption = SortOption.Title, query: String = ""): List<Song>

    fun play(song: Song, queue: List<Song>)
    fun togglePlayPause()
    fun skipToNext()
    fun skipToPrevious()
    fun seekTo(positionMs: Long)
    fun setRepeatMode(mode: RepeatMode)
    fun toggleShuffle()
    fun getCurrentQueue(): List<Song>

    val audioSessionId: Int

    val amplitudes: Flow<List<Float>>

    suspend fun deleteSongFile(song: Song): DeleteResult
    suspend fun deleteAlbumSongs(albumId: Long): DeleteResult
    suspend fun commitDelete(requestId: Long): Boolean
    suspend fun cancelDelete(requestId: Long)
    suspend fun updateSongMetadata(song: Song): EditResult
    suspend fun updateAlbumInfo(albumId: Long, newAlbum: String, newArtist: String): EditResult
    suspend fun commitEdit(requestId: Long): Boolean
    suspend fun cancelEdit(requestId: Long)
}
