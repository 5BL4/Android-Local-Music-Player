package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GetDailyRecommendationsUseCaseTest {

    private fun createSong(id: Long) = Song(
        id = id,
        mediaStoreId = id,
        title = "Song $id",
        artist = "Artist",
        album = "Album",
        albumId = 0,
        durationMs = 200_000L,
        filePath = null,
        contentUri = null,
        albumArtUri = null,
        year = null,
        trackNumber = null,
        discNumber = null,
        genre = null
    )

    private fun createSongs(count: Int): List<Song> = (1L..count).map { createSong(it) }

    @Test
    fun `returns at most count items`() = runTest {
        val library = createSongs(20)
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(library)
        val useCase = GetDailyRecommendationsUseCase(repo)

        val result = useCase(count = 5).first()
        assertEquals(5, result.size)
    }

    @Test
    fun `result is subset of the library`() = runTest {
        val library = createSongs(20)
        val librarySet = library.toSet()
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(library)
        val useCase = GetDailyRecommendationsUseCase(repo)

        val result = useCase(count = 10).first()
        assertEquals(10, result.size)
        for (song in result) {
            assertTrue("Result song $song should be in the library", song in librarySet)
        }
    }

    @Test
    fun `deterministic — same seed twice yields identical lists`() = runTest {
        val library = createSongs(30)
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(library)
        val useCase = GetDailyRecommendationsUseCase(repo)

        val seed = 12345L
        val first = useCase(count = 10, seed = seed).first()
        val second = useCase(count = 10, seed = seed).first()
        assertEquals(first, second)
    }

    @Test
    fun `different seeds yield different orderings`() = runTest {
        // Build a library of 25 songs so accidental same-order collision is
        // vanishingly unlikely (shuffled permutations for n=25 are enormous).
        val library = createSongs(25)
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(library)
        val useCase = GetDailyRecommendationsUseCase(repo)

        val seedA = 1L
        val seedB = 2L
        var seedC = 3L

        val resultA = useCase(count = 10, seed = seedA).first()
        val resultB = useCase(count = 10, seed = seedB).first()

        if (resultA != resultB) {
            // Happy path — different seeds produce different lists.
            return@runTest
        }

        // Freak collision: try a third seed.
        val resultC = useCase(count = 10, seed = seedC).first()
        assertNotEquals(
            "Even seeds $seedA, $seedB, $seedC all produced the same list — extremely unlikely",
            resultA,
            resultC
        )
    }

    @Test
    fun `empty library returns empty list`() = runTest {
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(emptyList())
        val useCase = GetDailyRecommendationsUseCase(repo)

        val result = useCase(count = 10).first()
        assertTrue(result.isEmpty())
    }

    @Test
    fun `count greater than library size returns all songs`() = runTest {
        val library = createSongs(5)
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(library)
        val useCase = GetDailyRecommendationsUseCase(repo)

        val result = useCase(count = 100).first()
        assertEquals(5, result.size)
    }

    @Test
    fun `count zero returns empty list`() = runTest {
        val library = createSongs(20)
        val repo = mockk<MusicRepository>()
        every { repo.songs } returns MutableStateFlow(library)
        val useCase = GetDailyRecommendationsUseCase(repo)

        val result = useCase(count = 0).first()
        assertTrue(result.isEmpty())
    }
}
