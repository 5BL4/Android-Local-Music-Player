package com.musicplayer.localmusicplayer.data.repository

import app.cash.turbine.test
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistDao
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistSongCrossRefDao
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistEntity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class PlaylistRepositoryImplTest {

    private val playlistDao: PlaylistDao = mockk(relaxed = true)
    private val crossRefDao: PlaylistSongCrossRefDao = mockk(relaxed = true)

    private lateinit var repository: PlaylistRepositoryImpl

    @Before
    fun setUp() {
        repository = PlaylistRepositoryImpl(playlistDao, crossRefDao)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `playlists flow emits with song counts`() = runTest {
        val entity = PlaylistEntity(
            id = 1L,
            name = "My List",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        every { playlistDao.getAllPlaylists() } returns flowOf(listOf(entity))
        coEvery { crossRefDao.getSongCount(1L) } returns 5

        repository.playlists.test {
            val result = awaitItem()

            assertEquals(1, result.size)
            assertEquals(5, result[0].songCount)
            assertEquals("My List", result[0].name)

            awaitComplete()
        }
    }

    @Test
    fun `createPlaylist returns dao result and delegates`() = runTest {
        coEvery { playlistDao.createPlaylist(any()) } returns 42L

        val result = repository.createPlaylist("New Playlist")

        assertEquals(42L, result)
        coVerify { playlistDao.createPlaylist(any()) }
    }

    @Test
    fun `deletePlaylist deletes cross refs then playlist`() = runTest {
        val playlistId = 1L

        repository.deletePlaylist(playlistId)

        coVerifyOrder {
            playlistDao.deleteSongsFromPlaylist(playlistId)
            playlistDao.deletePlaylist(playlistId)
        }
    }

    @Test
    fun `getPlaylistById returns null when not found`() = runTest {
        coEvery { playlistDao.getPlaylistById(99L) } returns null

        val result = repository.getPlaylistById(99L)

        assertNull(result)
        coVerify(exactly = 0) { crossRefDao.getSongCount(any()) }
    }

    @Test
    fun `getPlaylistById returns with song count`() = runTest {
        val entity = PlaylistEntity(
            id = 1L,
            name = "Test",
            createdAt = 1000L,
            updatedAt = 2000L
        )
        coEvery { playlistDao.getPlaylistById(1L) } returns entity
        coEvery { crossRefDao.getSongCount(1L) } returns 7

        val result = repository.getPlaylistById(1L)

        assertNotNull(result)
        assertEquals(1L, result!!.id)
        assertEquals("Test", result.name)
        assertEquals(7, result.songCount)
    }

    @Test
    fun `addSongToPlaylist uses current count as position`() = runTest {
        val playlistId = 1L
        val songId = 42L
        coEvery { crossRefDao.getSongCount(playlistId) } returns 3
        // Return an entity so the updatePlaylist path is taken
        coEvery { playlistDao.getPlaylistById(playlistId) } returns PlaylistEntity(
            id = playlistId,
            name = "Test",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        repository.addSongToPlaylist(playlistId, songId)

        coVerify {
            crossRefDao.addSongToPlaylist(match { crossRef ->
                crossRef.playlistId == playlistId &&
                        crossRef.songId == songId &&
                        crossRef.position == 3
            })
        }
    }

    @Test
    fun `removeSongFromPlaylist delegates to dao`() = runTest {
        val playlistId = 1L
        val songId = 99L

        repository.removeSongFromPlaylist(playlistId, songId)

        coVerify { crossRefDao.removeSongFromPlaylist(playlistId, songId) }
    }
}
