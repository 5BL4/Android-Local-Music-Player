package com.musicplayer.localmusicplayer.data.repository

import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistDao
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistSongCrossRefDao
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistEntity
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistSongCrossRef
import com.musicplayer.localmusicplayer.data.mapper.toDomain
import com.musicplayer.localmusicplayer.data.mapper.toEntity
import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaylistRepositoryImpl @Inject constructor(
    private val playlistDao: PlaylistDao,
    private val crossRefDao: PlaylistSongCrossRefDao
) : PlaylistRepository {

    override val playlists: Flow<List<Playlist>> = flow {
        playlistDao.getAllPlaylists().collect { entities ->
            val playlists = entities.map { entity ->
                val count = crossRefDao.getSongCount(entity.id)
                entity.toDomain(count)
            }
            emit(playlists)
        }
    }

    override suspend fun createPlaylist(name: String): Long {
        val now = System.currentTimeMillis()
        val entity = PlaylistEntity(name = name, createdAt = now, updatedAt = now)
        return playlistDao.createPlaylist(entity)
    }

    override suspend fun updatePlaylist(playlist: Playlist) {
        playlistDao.updatePlaylist(playlist.toEntity().copy(updatedAt = System.currentTimeMillis()))
    }

    override suspend fun deletePlaylist(playlistId: Long) {
        // Delete cross_ref entries first to avoid FK issues
        playlistDao.deleteSongsFromPlaylist(playlistId)
        playlistDao.deletePlaylist(playlistId)
    }

    override suspend fun getPlaylistById(id: Long): Playlist? {
        val entity = playlistDao.getPlaylistById(id) ?: return null
        val songCount = crossRefDao.getSongCount(id)
        return entity.toDomain(songCount)
    }

    override fun getSongsInPlaylist(playlistId: Long): Flow<List<Song>> {
        return crossRefDao.getSongsInPlaylist(playlistId).map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun updatePlaylistInfo(id: Long, name: String, description: String?, coverArtUri: String?) {
        playlistDao.updatePlaylistInfo(id, name, description, coverArtUri, System.currentTimeMillis())
    }

    override suspend fun addSongToPlaylist(playlistId: Long, songId: Long) {
        val currentCount = crossRefDao.getSongCount(playlistId)
        val crossRef = PlaylistSongCrossRef(
            playlistId = playlistId,
            songId = songId,
            position = currentCount
        )
        crossRefDao.addSongToPlaylist(crossRef)
        // Update playlist modified time
        playlistDao.getPlaylistById(playlistId)?.let {
            playlistDao.updatePlaylist(it.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    override suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long) {
        crossRefDao.removeSongFromPlaylist(playlistId, songId)
    }

    override suspend fun reorderPlaylist(playlistId: Long, songIds: List<Long>) {
        crossRefDao.reorderPlaylist(playlistId, songIds)
    }
}
