package com.musicplayer.localmusicplayer.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.musicplayer.localmusicplayer.data.local.db.AppDatabase
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SongDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: SongDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.songDao()
    }

    @After
    fun teardown() { db.close() }

    private fun createSongEntity(
        mediaStoreId: Long = 1L,
        title: String = "Test Song",
        artist: String = "Test Artist",
        album: String = "Test Album",
        albumId: Long = 1L,
        duration: Long = 180000L
    ) = SongEntity(
        mediaStoreId = mediaStoreId,
        title = title,
        artist = artist,
        album = album,
        albumId = albumId,
        duration = duration,
        filePath = "/storage/test.mp3",
        contentUri = "content://media/external/audio/$mediaStoreId",
        albumArtUri = null,
        dateAdded = System.currentTimeMillis() / 1000,
        year = 2024,
        trackNumber = 1,
        discNumber = 1,
        genre = "Pop",
        mimeType = "audio/mpeg",
        size = 5000000L
    )

    @Test
    fun upsertAll_insertsNewSongs() = runTest {
        val songs = listOf(createSongEntity(mediaStoreId = 1), createSongEntity(mediaStoreId = 2))
        dao.upsertAll(songs)
        val result = dao.getSongsByMediaStoreIds(listOf(1, 2))
        assertEquals(2, result.size)
    }

    @Test
    fun getSongById_returnsCorrectSong() = runTest {
        val song = createSongEntity(mediaStoreId = 100, title = "Find Me")
        dao.upsertAll(listOf(song))
        val allSongs = dao.getSongsByMediaStoreIds(listOf(100))
        assertEquals(1, allSongs.size)
        assertEquals("Find Me", allSongs[0].title)
    }

    @Test
    fun deleteById_removesSong() = runTest {
        val song = createSongEntity(mediaStoreId = 200)
        dao.upsertAll(listOf(song))
        val inserted = dao.getSongsByMediaStoreIds(listOf(200))
        assertEquals(1, inserted.size)
        dao.deleteById(inserted[0].id)
        val after = dao.getSongsByMediaStoreIds(listOf(200))
        assertEquals(0, after.size)
    }

    @Test
    fun deleteMissingSongs_removesSongsNotInList() = runTest {
        val songs = listOf(
            createSongEntity(mediaStoreId = 1),
            createSongEntity(mediaStoreId = 2),
            createSongEntity(mediaStoreId = 3)
        )
        dao.upsertAll(songs)
        // Keep only mediaStoreId 1 and 2
        dao.deleteMissingSongs(listOf(1, 2))
        val remaining = dao.getSongsByMediaStoreIds(listOf(1, 2, 3))
        assertEquals(2, remaining.size)
    }
}
