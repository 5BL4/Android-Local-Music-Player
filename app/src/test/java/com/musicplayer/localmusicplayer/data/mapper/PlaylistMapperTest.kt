package com.musicplayer.localmusicplayer.data.mapper

import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PlaylistMapperTest {

    @Test
    fun `default songCount is zero`() {
        val entity = PlaylistEntity(
            id = 1L,
            name = "My Playlist",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val playlist = entity.toDomain()

        assertEquals(0, playlist.songCount)
    }

    @Test
    fun `custom songCount is passed through`() {
        val entity = PlaylistEntity(
            id = 1L,
            name = "My Playlist",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val playlist = entity.toDomain(songCount = 42)

        assertEquals(42, playlist.songCount)
    }

    @Test
    fun `all fields map correctly`() {
        val entity = PlaylistEntity(
            id = 5L,
            name = "Favorites",
            description = "My favorite songs",
            coverArtUri = "content://cover/5",
            createdAt = 1_700_000_000_000L,
            updatedAt = 1_700_100_000_000L
        )

        val playlist = entity.toDomain(songCount = 15)

        assertEquals(5L, playlist.id)
        assertEquals("Favorites", playlist.name)
        assertEquals(15, playlist.songCount)
        assertEquals("My favorite songs", playlist.description)
        assertEquals("content://cover/5", playlist.coverArtUri)
        assertEquals(1_700_000_000_000L, playlist.createdAt)
        assertEquals(1_700_100_000_000L, playlist.updatedAt)
    }

    @Test
    fun `nullable fields with null values map correctly`() {
        val entity = PlaylistEntity(
            id = 1L,
            name = "Simple List",
            description = null,
            coverArtUri = null,
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val playlist = entity.toDomain()

        assertNull(playlist.description)
        assertNull(playlist.coverArtUri)
    }

    @Test
    fun `nullable fields with non-null values map correctly`() {
        val entity = PlaylistEntity(
            id = 2L,
            name = "Rich List",
            description = "A description",
            coverArtUri = "content://art/2",
            createdAt = 1000L,
            updatedAt = 2000L
        )

        val playlist = entity.toDomain()

        assertEquals("A description", playlist.description)
        assertEquals("content://art/2", playlist.coverArtUri)
    }
}
