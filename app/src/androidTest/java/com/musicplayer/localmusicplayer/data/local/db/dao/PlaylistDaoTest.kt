package com.musicplayer.localmusicplayer.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.musicplayer.localmusicplayer.data.local.db.AppDatabase
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: PlaylistDao

    @Before
    fun setup() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()
        dao = db.playlistDao()
    }

    @After
    fun teardown() { db.close() }

    @Test
    fun createPlaylist_returnsGeneratedId() = runTest {
        val entity = PlaylistEntity(name = "My Playlist", createdAt = 1000L, updatedAt = 1000L)
        val id = dao.createPlaylist(entity)
        assert(id > 0)
    }

    @Test
    fun getPlaylistById_returnsCorrectPlaylist() = runTest {
        val id = dao.createPlaylist(PlaylistEntity(name = "Rock", createdAt = 1000L, updatedAt = 1000L))
        val result = dao.getPlaylistById(id)
        assertNotNull(result)
        assertEquals("Rock", result?.name)
    }

    @Test
    fun getPlaylistById_returnsNullForMissing() = runTest {
        val result = dao.getPlaylistById(999L)
        assertNull(result)
    }

    @Test
    fun deletePlaylist_removesPlaylist() = runTest {
        val id = dao.createPlaylist(PlaylistEntity(name = "ToDelete", createdAt = 1000L, updatedAt = 1000L))
        dao.deletePlaylist(id)
        assertNull(dao.getPlaylistById(id))
    }

    @Test
    fun updatePlaylistInfo_changesFields() = runTest {
        val id = dao.createPlaylist(PlaylistEntity(name = "Old Name", createdAt = 1000L, updatedAt = 1000L))
        dao.updatePlaylistInfo(id, "New Name", "A description", null, 2000L)
        val updated = dao.getPlaylistById(id)
        assertEquals("New Name", updated?.name)
        assertEquals("A description", updated?.description)
        assertEquals(2000L, updated?.updatedAt)
    }
}
