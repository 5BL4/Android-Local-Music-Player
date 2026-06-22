package com.musicplayer.localmusicplayer.data.mapper

import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SongMapperTest {

    @Test
    fun `single entity maps all fields correctly including duration to durationMs`() {
        val entity = SongEntity(
            id = 1L,
            mediaStoreId = 100L,
            title = "Test Song",
            artist = "Test Artist",
            album = "Test Album",
            albumId = 10L,
            duration = 300000L,
            filePath = "/music/test.mp3",
            contentUri = "content://test",
            albumArtUri = "content://art",
            dateAdded = 123456789L,
            year = 2024,
            trackNumber = 5,
            discNumber = 1,
            genre = "Rock",
            mimeType = "audio/mpeg",
            size = 5_000_000L
        )

        val song = entity.toDomain()

        assertEquals(1L, song.id)
        assertEquals(100L, song.mediaStoreId)
        assertEquals("Test Song", song.title)
        assertEquals("Test Artist", song.artist)
        assertEquals("Test Album", song.album)
        assertEquals(10L, song.albumId)
        assertEquals(300000L, song.durationMs) // key: duration maps to durationMs
        assertEquals("/music/test.mp3", song.filePath)
        assertEquals("content://test", song.contentUri)
        assertEquals("content://art", song.albumArtUri)
        assertEquals(2024, song.year)
        assertEquals(5, song.trackNumber)
        assertEquals(1, song.discNumber)
        assertEquals("Rock", song.genre)
    }

    @Test
    fun `fields not in Song dateAdded mimeType size are dropped`() {
        val entity = SongEntity(
            id = 1L,
            mediaStoreId = 100L,
            title = "Test",
            artist = "Artist",
            album = "Album",
            albumId = 10L,
            duration = 1000L,
            filePath = null,
            contentUri = null,
            albumArtUri = null,
            dateAdded = 999999L,
            year = null,
            trackNumber = null,
            discNumber = null,
            genre = null,
            mimeType = "audio/flac",
            size = 123456L
        )

        val song = entity.toDomain()

        // Verify entity still holds the fields Song does not carry
        assertEquals(999999L, entity.dateAdded)
        assertEquals("audio/flac", entity.mimeType)
        assertEquals(123456L, entity.size)

        // Song has no dateAdded / mimeType / size — verify mapping produced a valid Song
        assertEquals(entity.id, song.id)
        assertEquals(entity.duration, song.durationMs)
    }

    @Test
    fun `nullable fields with null values map correctly`() {
        val entity = SongEntity(
            id = 1L,
            mediaStoreId = 100L,
            title = "Test",
            artist = "Artist",
            album = "Album",
            albumId = 10L,
            duration = 1000L,
            filePath = null,
            contentUri = null,
            albumArtUri = null,
            dateAdded = 0L,
            year = null,
            trackNumber = null,
            discNumber = null,
            genre = null,
            mimeType = "",
            size = 0L
        )

        val song = entity.toDomain()

        assertNull(song.filePath)
        assertNull(song.contentUri)
        assertNull(song.albumArtUri)
        assertNull(song.year)
        assertNull(song.trackNumber)
        assertNull(song.discNumber)
        assertNull(song.genre)
    }

    @Test
    fun `list extension maps all items`() {
        val entities = listOf(
            SongEntity(
                id = 1L, mediaStoreId = 100L, title = "Song 1", artist = "A1",
                album = "Album", albumId = 10L, duration = 1000L,
                filePath = null, contentUri = null, albumArtUri = null,
                dateAdded = 0L, year = null, trackNumber = null,
                discNumber = null, genre = null, mimeType = "", size = 0L
            ),
            SongEntity(
                id = 2L, mediaStoreId = 200L, title = "Song 2", artist = "A2",
                album = "Album", albumId = 10L, duration = 2000L,
                filePath = null, contentUri = null, albumArtUri = null,
                dateAdded = 0L, year = null, trackNumber = null,
                discNumber = null, genre = null, mimeType = "", size = 0L
            ),
            SongEntity(
                id = 3L, mediaStoreId = 300L, title = "Song 3", artist = "A3",
                album = "Album", albumId = 10L, duration = 3000L,
                filePath = null, contentUri = null, albumArtUri = null,
                dateAdded = 0L, year = null, trackNumber = null,
                discNumber = null, genre = null, mimeType = "", size = 0L
            )
        )

        val songs = entities.toDomain()

        assertEquals(3, songs.size)
        assertEquals(1L, songs[0].id)
        assertEquals("Song 1", songs[0].title)
        assertEquals(1000L, songs[0].durationMs)
        assertEquals(2L, songs[1].id)
        assertEquals("Song 2", songs[1].title)
        assertEquals(2000L, songs[1].durationMs)
        assertEquals(3L, songs[2].id)
        assertEquals("Song 3", songs[2].title)
        assertEquals(3000L, songs[2].durationMs)
    }
}
