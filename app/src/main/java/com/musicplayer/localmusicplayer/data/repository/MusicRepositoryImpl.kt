package com.musicplayer.localmusicplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import com.musicplayer.localmusicplayer.data.local.db.dao.AlbumResult
import com.musicplayer.localmusicplayer.data.local.db.dao.ArtistResult
import com.musicplayer.localmusicplayer.data.local.db.dao.SongDao
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import com.musicplayer.localmusicplayer.data.local.datasource.MediaStoreDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import com.musicplayer.localmusicplayer.data.mapper.toDomain
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Artist
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.service.PlaybackManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val playbackManager: PlaybackManager,
    @ApplicationContext private val context: Context
) : MusicRepository {

    override val songs: Flow<List<Song>> = songDao.getAllSongs().map { it.toDomain() }

    // Paging 3 API
    private val pagingConfig = PagingConfig(pageSize = 50, enablePlaceholders = false)

    override fun getSongsPaged(sort: SortOption, query: String): Flow<PagingData<Song>> {
        val source: PagingSource<Int, SongEntity> = when {
            query.isNotBlank() -> songDao.pagingSourceBySearch(query)
            else -> when (sort) {
                SortOption.Title     -> songDao.pagingSourceByTitle()
                SortOption.Artist    -> songDao.pagingSourceByArtist()
                SortOption.Album     -> songDao.pagingSourceByAlbum()
                SortOption.DateAdded -> songDao.pagingSourceByDateAdded()
                SortOption.Duration  -> songDao.pagingSourceByDuration()
            }
        }
        return Pager(config = pagingConfig) { source }
            .flow
            .map { pagingData -> pagingData.map { entity -> entity.toDomain() } }
    }

    override val albums: Flow<List<Album>> = songDao.getAlbums().map { list ->
        list.map { it.toAlbum() }
    }

    override val artists: Flow<List<Artist>> = songDao.getArtists().map { list ->
        list.map { it.toArtist() }
    }

    override val playbackState: Flow<PlaybackState> = playbackManager.playbackState
    override val currentPosition: Flow<Long> = playbackManager.currentPosition

    override suspend fun scanMusicFiles() {
        val scanned = mediaStoreDataSource.scanAudioFiles()
        val scannedMsIds = scanned.map { it.mediaStoreId }

        // Fast delete+insert: delete existing rows that will be replaced
        if (scannedMsIds.isNotEmpty()) {
            songDao.deleteMissingSongs(scannedMsIds) // deletes songs NOT in scanned list
        }
        // Delete matching old entries, then insert fresh
        val existingToReplace = if (scannedMsIds.isNotEmpty()) {
            songDao.getSongsByMediaStoreIds(scannedMsIds)
        } else emptyList()
        if (existingToReplace.isNotEmpty()) {
            songDao.deleteSongsByIds(existingToReplace.map { it.id })
        }
        // Insert all scanned songs as new rows
        if (scanned.isNotEmpty()) {
            songDao.upsertAll(scanned.map { it.copy(id = 0) })
        }
    }

    override suspend fun getSongsByIds(ids: List<Long>): List<Song> {
        return songDao.getSongsByIds(ids).toDomain()
    }

    override suspend fun getSongById(id: Long): Song? {
        return songDao.getSongById(id)?.toDomain()
    }

    override fun getSongsByAlbum(albumId: Long): Flow<List<Song>> {
        return songDao.getSongsByAlbum(albumId).map { it.toDomain() }
    }

    override fun getSongsByArtist(artist: String): Flow<List<Song>> {
        return songDao.getSongsByArtist(artist).map { it.toDomain() }
    }

    override fun searchSongs(query: String): Flow<List<Song>> {
        return songDao.searchSongs(query).map { it.toDomain() }
    }

    override fun play(song: Song, queue: List<Song>) {
        playbackManager.play(song, queue)
    }

    override fun togglePlayPause() {
        playbackManager.togglePlayPause()
    }

    override fun skipToNext() {
        playbackManager.skipToNext()
    }

    override fun skipToPrevious() {
        playbackManager.skipToPrevious()
    }

    override fun seekTo(positionMs: Long) {
        playbackManager.seekTo(positionMs)
    }

    override fun setRepeatMode(mode: RepeatMode) {
        playbackManager.setRepeatMode(mode)
    }

    override fun toggleShuffle() {
        playbackManager.toggleShuffle()
    }

    override fun getCurrentQueue(): List<Song> = playbackManager.getCurrentQueue()

    override val audioSessionId: Int
        get() = playbackManager.audioSessionId

    override suspend fun updateAlbumInfo(albumId: Long, newAlbum: String, newArtist: String) {
        songDao.updateAlbumInfo(albumId, newAlbum, newArtist)
    }

    override suspend fun deleteAlbumSongs(albumId: Long, songs: List<Song>) {
        songs.forEach { song ->
            try {
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaStoreId)
                context.contentResolver.delete(uri, null, null)
            } catch (_: Exception) {}
        }
        songDao.deleteByAlbumId(albumId)
    }

    override suspend fun deleteSongFile(song: Song): Boolean {
        return try {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                song.mediaStoreId
            )
            val rows = context.contentResolver.delete(uri, null, null)
            if (rows > 0) {
                songDao.deleteById(song.id)
                true
            } else false
        } catch (e: Exception) { false }
    }

    override suspend fun updateSongMetadata(song: Song) {
        songDao.updateSong(
            id = song.id,
            title = song.title,
            artist = song.artist,
            album = song.album,
            albumId = song.albumId,
            year = song.year,
            track = song.trackNumber,
            disc = song.discNumber,
            genre = song.genre,
            albumArtUri = song.albumArtUri
        )
    }

    private fun AlbumResult.toAlbum() = Album(
        albumId = albumId,
        name = album,
        artist = artist,
        albumArtUri = albumArtUri,
        songCount = 0
    )

    private fun ArtistResult.toArtist() = Artist(
        name = artist,
        songCount = songCount,
        albumCount = albumCount,
        albumArtUri = albumArtUri
    )
}
