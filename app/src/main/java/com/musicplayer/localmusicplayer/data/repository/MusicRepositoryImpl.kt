package com.musicplayer.localmusicplayer.data.repository

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.map
import com.musicplayer.localmusicplayer.data.deletion.MediaStoreDeleteManager
import com.musicplayer.localmusicplayer.data.edit.MediaStoreEditManager
import com.musicplayer.localmusicplayer.data.local.db.dao.AlbumResult
import com.musicplayer.localmusicplayer.data.local.db.dao.ArtistResult
import com.musicplayer.localmusicplayer.data.local.db.dao.SongDao
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import com.musicplayer.localmusicplayer.data.local.datasource.MediaStoreDataSource
import dagger.hilt.android.qualifiers.ApplicationContext
import com.musicplayer.localmusicplayer.data.mapper.toDomain
import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.model.Artist
import com.musicplayer.localmusicplayer.domain.model.DeleteResult
import com.musicplayer.localmusicplayer.domain.model.EditResult
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.service.PlaybackManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MusicRepositoryImpl @Inject constructor(
    private val songDao: SongDao,
    private val mediaStoreDataSource: MediaStoreDataSource,
    private val playbackManager: PlaybackManager,
    private val deleteManager: MediaStoreDeleteManager,
    private val editManager: MediaStoreEditManager,
    @ApplicationContext private val context: Context
) : MusicRepository {

    override val songs: Flow<List<Song>> = songDao.getAllSongs().map { it.toDomain() }

    // Paging 3 API
    private val pagingConfig = PagingConfig(pageSize = 50, enablePlaceholders = false)

    override fun getSongsPaged(sort: SortOption, query: String): Flow<PagingData<Song>> {
        // Factory lambda must create a NEW PagingSource on each invalidation.
        // Capturing a single source outside the lambda returns an already-invalidated
        // instance after DB writes, which crashes Paging 3.
        return Pager(config = pagingConfig) {
            when {
                query.isNotBlank() -> songDao.pagingSourceBySearch(query)
                else -> when (sort) {
                    SortOption.Title     -> songDao.pagingSourceByTitle()
                    SortOption.Artist    -> songDao.pagingSourceByArtist()
                    SortOption.Album     -> songDao.pagingSourceByAlbum()
                    SortOption.DateAdded -> songDao.pagingSourceByDateAdded()
                    SortOption.Duration  -> songDao.pagingSourceByDuration()
                }
            }
        }
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
        val scanned = withContext(Dispatchers.IO) {
            mediaStoreDataSource.scanAudioFiles()
        }
        val scannedMsIds = scanned.map { it.mediaStoreId }

        // Remove songs that are no longer in MediaStore. The CASCADE foreign key on
        // playlist_song_cross_ref will drop their playlist memberships — intended,
        // since the underlying file is gone.
        if (scannedMsIds.isNotEmpty()) {
            songDao.deleteMissingSongs(scannedMsIds)
        }

        // Preserve existing row identity (the auto-generated `id`) across rescans.
        // playlist_song_cross_ref.song_id references songs.id via an ON DELETE CASCADE
        // foreign key, so deleting/re-inserting songs here would wipe every playlist
        // association on each scan. Reuse the existing id for songs already in the DB
        // (upsert updates the row in place) and only auto-generate ids for genuinely
        // new songs.
        val existingIdByMsId: Map<Long, Long> = if (scannedMsIds.isNotEmpty()) {
            songDao.getSongsByMediaStoreIds(scannedMsIds).associate { it.mediaStoreId to it.id }
        } else emptyMap()

        if (scanned.isNotEmpty()) {
            songDao.upsertAll(
                scanned.map { entity ->
                    val existingId = existingIdByMsId[entity.mediaStoreId]
                    if (existingId != null) entity.copy(id = existingId) else entity.copy(id = 0)
                }
            )
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

    override suspend fun getSortedSongs(sort: SortOption, query: String): List<Song> {
        val entities = if (query.isNotBlank()) {
            songDao.searchAll(query)
        } else {
            when (sort) {
                SortOption.Title     -> songDao.getAllByTitle()
                SortOption.Artist    -> songDao.getAllByArtist()
                SortOption.Album     -> songDao.getAllByAlbum()
                SortOption.DateAdded -> songDao.getAllByDateAdded()
                SortOption.Duration  -> songDao.getAllByDuration()
            }
        }
        return entities.toDomain()
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

    override suspend fun updateAlbumInfo(albumId: Long, newAlbum: String, newArtist: String): EditResult = withContext(Dispatchers.IO) {
        val entities = songDao.getSongsByAlbumOnce(albumId)
        if (entities.isEmpty()) {
            songDao.updateAlbumInfo(albumId, newAlbum, newArtist)
            return@withContext EditResult.Success
        }
        val songs = entities.toDomain()
        val payloads = songs.map { MediaStoreEditManager.EditPayload(song = it, newAlbum = newAlbum, newArtist = newArtist) }
        val result = editManager.attemptEdit(payloads) { payload ->
            songDao.updateSong(
                id = payload.song.id,
                title = payload.song.title,
                artist = newArtist,
                album = newAlbum,
                albumId = payload.song.albumId,
                year = payload.song.year,
                track = payload.song.trackNumber,
                disc = payload.song.discNumber,
                genre = payload.song.genre,
                albumArtUri = payload.song.albumArtUri
            )
        }
        when (result) {
            is MediaStoreEditManager.AttemptResult.Done -> {
                // Per-song onTagWritten already updated each successfully-edited song's
                // album/artist in the DB. Do NOT run a bulk UPDATE here — that would also
                // overwrite songs whose file edit failed (skipped in the loop), creating a
                // DB/file mismatch that surfaces as duplicate albums after the next rescan.
                EditResult.Success
            }
            is MediaStoreEditManager.AttemptResult.NeedsConfirm -> EditResult.NeedsConfirmation(result.requestId)
            MediaStoreEditManager.AttemptResult.Error -> EditResult.Error
        }
    }

    override suspend fun deleteAlbumSongs(albumId: Long): DeleteResult = withContext(Dispatchers.IO) {
        val entities = songDao.getSongsByAlbumOnce(albumId)
        if (entities.isEmpty()) {
            songDao.deleteByAlbumId(albumId)
            return@withContext DeleteResult.Success
        }
        val songs = entities.toDomain()
        val result = deleteManager.attemptDelete(songs) { ids ->
            songDao.deleteSongsByIds(ids)
            playbackManager.removeDeletedSongs(ids)
        }
        when (result) {
            is MediaStoreDeleteManager.AttemptResult.Done -> {
                songDao.deleteByAlbumId(albumId)
                DeleteResult.Success
            }
            is MediaStoreDeleteManager.AttemptResult.NeedsConfirm -> {
                DeleteResult.NeedsConfirmation(result.requestId)
            }
            MediaStoreDeleteManager.AttemptResult.Error -> DeleteResult.Error
        }
    }

    override suspend fun deleteSongFile(song: Song): DeleteResult = withContext(Dispatchers.IO) {
        val result = deleteManager.attemptDelete(listOf(song)) { ids ->
            songDao.deleteSongsByIds(ids)
            playbackManager.removeDeletedSongs(ids)
        }
        when (result) {
            is MediaStoreDeleteManager.AttemptResult.Done -> DeleteResult.Success
            is MediaStoreDeleteManager.AttemptResult.NeedsConfirm -> DeleteResult.NeedsConfirmation(result.requestId)
            MediaStoreDeleteManager.AttemptResult.Error -> DeleteResult.Error
        }
    }

    override suspend fun commitDelete(requestId: Long): Boolean = withContext(Dispatchers.IO) {
        val songIds = deleteManager.onConfirmed(requestId) ?: return@withContext false
        if (songIds.isNotEmpty()) {
            songDao.deleteSongsByIds(songIds)
            playbackManager.removeDeletedSongs(songIds)
        }
        // 如果还有更多 sender 需要确认（API29 多文件），不 clear
        if (!deleteManager.hasMoreSenders(requestId)) {
            deleteManager.clear(requestId)
        }
        true
    }

    override suspend fun cancelDelete(requestId: Long) = withContext(Dispatchers.IO) {
        deleteManager.cancel(requestId)
    }

    override suspend fun updateSongMetadata(song: Song): EditResult = withContext(Dispatchers.IO) {
        val payload = MediaStoreEditManager.EditPayload(song = song, newAlbum = null, newArtist = null)
        val result = editManager.attemptEdit(listOf(payload)) { p ->
            songDao.updateSong(
                id = p.song.id,
                title = p.song.title,
                artist = p.song.artist,
                album = p.song.album,
                albumId = p.song.albumId,
                year = p.song.year,
                track = p.song.trackNumber,
                disc = p.song.discNumber,
                genre = p.song.genre,
                albumArtUri = p.song.albumArtUri
            )
        }
        when (result) {
            is MediaStoreEditManager.AttemptResult.Done -> EditResult.Success
            is MediaStoreEditManager.AttemptResult.NeedsConfirm -> EditResult.NeedsConfirmation(result.requestId)
            MediaStoreEditManager.AttemptResult.Error -> EditResult.Error
        }
    }

    override suspend fun commitEdit(requestId: Long): Boolean = withContext(Dispatchers.IO) {
        val payloads = editManager.onConfirmed(requestId) ?: return@withContext false
        if (payloads.isEmpty() && editManager.hasMoreSenders(requestId)) {
            return@withContext true
        }
        val success = editManager.retryWriteTags(requestId) { payload ->
            if (payload.newAlbum != null || payload.newArtist != null) {
                songDao.updateSong(
                    id = payload.song.id,
                    title = payload.song.title,
                    artist = payload.newArtist ?: payload.song.artist,
                    album = payload.newAlbum ?: payload.song.album,
                    albumId = payload.song.albumId,
                    year = payload.song.year,
                    track = payload.song.trackNumber,
                    disc = payload.song.discNumber,
                    genre = payload.song.genre,
                    albumArtUri = payload.song.albumArtUri
                )
            } else {
                songDao.updateSong(
                    id = payload.song.id,
                    title = payload.song.title,
                    artist = payload.song.artist,
                    album = payload.song.album,
                    albumId = payload.song.albumId,
                    year = payload.song.year,
                    track = payload.song.trackNumber,
                    disc = payload.song.discNumber,
                    genre = payload.song.genre,
                    albumArtUri = payload.song.albumArtUri
                )
            }
        }
        if (!editManager.hasMoreSenders(requestId)) {
            editManager.clear(requestId)
        }
        success
    }

    override suspend fun cancelEdit(requestId: Long) = withContext(Dispatchers.IO) {
        editManager.cancel(requestId)
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
