package com.musicplayer.localmusicplayer.domain.repository

import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

interface PlaylistRepository {
    val playlists: Flow<List<Playlist>>

    suspend fun createPlaylist(name: String): Long
    suspend fun updatePlaylist(playlist: Playlist)
    suspend fun deletePlaylist(playlistId: Long)
    suspend fun getPlaylistById(id: Long): Playlist?
    fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>>
    suspend fun updatePlaylistInfo(id: Long, name: String, description: String?, coverArtUri: String?)
    suspend fun addSongToPlaylist(playlistId: Long, songId: Long)
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)
    suspend fun reorderPlaylist(playlistId: Long, songIds: List<Long>)
}
