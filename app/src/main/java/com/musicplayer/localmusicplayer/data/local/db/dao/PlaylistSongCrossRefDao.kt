package com.musicplayer.localmusicplayer.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistSongCrossRef
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistSongCrossRefDao {
    @Query("""
        SELECT s.* FROM songs s
        INNER JOIN playlist_song_cross_ref ref ON s.id = ref.song_id
        WHERE ref.playlist_id = :playlistId
        ORDER BY ref.position ASC
    """)
    fun getSongsInPlaylist(playlistId: Long): Flow<List<SongEntity>>

    @Query("SELECT COUNT(*) FROM playlist_song_cross_ref WHERE playlist_id = :playlistId")
    suspend fun getSongCount(playlistId: Long): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addSongToPlaylist(crossRef: PlaylistSongCrossRef)

    @Query("DELETE FROM playlist_song_cross_ref WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun removeSongFromPlaylist(playlistId: Long, songId: Long)

    @Query("UPDATE playlist_song_cross_ref SET position = :position WHERE playlist_id = :playlistId AND song_id = :songId")
    suspend fun updateSongPosition(playlistId: Long, songId: Long, position: Int)

    @Transaction
    suspend fun reorderPlaylist(playlistId: Long, songIds: List<Long>) {
        songIds.forEachIndexed { index, songId ->
            updateSongPosition(playlistId, songId, index)
        }
    }
}
