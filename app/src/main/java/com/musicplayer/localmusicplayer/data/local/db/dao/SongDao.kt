package com.musicplayer.localmusicplayer.data.local.db.dao

import androidx.paging.PagingSource
import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import kotlinx.coroutines.flow.Flow

data class AlbumResult(
    val album: String,
    val albumId: Long,
    val artist: String,
    val albumArtUri: String?
)

data class ArtistResult(
    val artist: String,
    val songCount: Int,
    val albumCount: Int,
    val albumArtUri: String?
)

@Dao
interface SongDao {

    // ─── Paging 3 source (replaces getAllSongs) ────────────────

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun pagingSourceByTitle(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY artist ASC, title ASC")
    fun pagingSourceByArtist(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY album ASC, title ASC")
    fun pagingSourceByAlbum(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY date_added DESC")
    fun pagingSourceByDateAdded(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs ORDER BY duration ASC")
    fun pagingSourceByDuration(): PagingSource<Int, SongEntity>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    fun pagingSourceBySearch(query: String): PagingSource<Int, SongEntity>

    // ─── One-shot sorted queries (for play queue) ──────────────

    @Query("SELECT * FROM songs ORDER BY title ASC")
    suspend fun getAllByTitle(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY artist ASC, title ASC")
    suspend fun getAllByArtist(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY album ASC, title ASC")
    suspend fun getAllByAlbum(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY date_added DESC")
    suspend fun getAllByDateAdded(): List<SongEntity>

    @Query("SELECT * FROM songs ORDER BY duration ASC")
    suspend fun getAllByDuration(): List<SongEntity>

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%' ORDER BY title ASC")
    suspend fun searchAll(query: String): List<SongEntity>

    // ─── Legacy list queries (kept for albums/artists/playlists) ─

    @Query("SELECT * FROM songs ORDER BY title ASC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Query("SELECT * FROM songs WHERE id IN (:ids)")
    suspend fun getSongsByIds(ids: List<Long>): List<SongEntity>

    @Query("SELECT DISTINCT album, album_id AS albumId, artist, album_art_uri AS albumArtUri FROM songs ORDER BY album ASC")
    fun getAlbums(): Flow<List<AlbumResult>>

    @Query("SELECT artist, COUNT(*) AS songCount, COUNT(DISTINCT album_id) AS albumCount, (SELECT album_art_uri FROM songs s2 WHERE s2.artist = songs.artist AND s2.album_art_uri IS NOT NULL LIMIT 1) AS albumArtUri FROM songs GROUP BY artist ORDER BY artist ASC")
    fun getArtists(): Flow<List<ArtistResult>>

    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC, title ASC")
    fun getSongsByAlbum(albumId: Long): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE album_id = :albumId ORDER BY track_number ASC, title ASC")
    suspend fun getSongsByAlbumOnce(albumId: Long): List<SongEntity>

    @Query("SELECT * FROM songs WHERE artist = :artist ORDER BY album ASC, track_number ASC")
    fun getSongsByArtist(artist: String): Flow<List<SongEntity>>

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE songs SET album=:newAlbum, artist=:newArtist WHERE album_id=:albumId")
    suspend fun updateAlbumInfo(albumId: Long, newAlbum: String, newArtist: String)

    @Query("DELETE FROM songs WHERE album_id = :albumId")
    suspend fun deleteByAlbumId(albumId: Long)

    @Query("SELECT * FROM songs WHERE media_store_id IN (:msIds)")
    suspend fun getSongsByMediaStoreIds(msIds: List<Long>): List<SongEntity>

    @Upsert
    suspend fun upsertAll(songs: List<SongEntity>)

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("DELETE FROM songs WHERE media_store_id NOT IN (:existingMediaStoreIds)")
    suspend fun deleteMissingSongs(existingMediaStoreIds: List<Long>)

    @Query("SELECT * FROM songs WHERE title LIKE '%' || :query || '%' OR artist LIKE '%' || :query || '%' OR album LIKE '%' || :query || '%'")
    fun searchSongs(query: String): Flow<List<SongEntity>>

    @Query("UPDATE songs SET title=:title, artist=:artist, album=:album, album_id=:albumId, year=:year, track_number=:track, disc_number=:disc, genre=:genre, album_art_uri=:albumArtUri WHERE id=:id")
    suspend fun updateSong(
        id: Long, title: String, artist: String, album: String,
        albumId: Long, year: Int?, track: Int?, disc: Int?, genre: String?, albumArtUri: String?
    )
}
