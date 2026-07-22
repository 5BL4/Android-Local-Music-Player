package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject
import kotlin.random.Random

/**
 * Returns a deterministic daily subset of the library.
 *
 * The default seed is [LocalDate.now().toEpochDay()][LocalDate.toEpochDay] so the same songs
 * are recommended all day. Pass an explicit [seed] to reproduce a specific ordering (e.g. for
 * testing). If the underlying songs [Flow] re‑emits (e.g. after a library rescan) the list
 * reshuffles with the same seed against the new input — accepted v1 behaviour.
 *
 * @param count how many songs to return; must be >= 0.
 * @param seed passed to [kotlin.random.Random] for deterministic shuffling.
 */
class GetDailyRecommendationsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(count: Int = 10, seed: Long = LocalDate.now().toEpochDay()): Flow<List<Song>> {
        require(count >= 0) { "count must be >= 0" }
        return musicRepository.songs.map { songs ->
            songs.shuffled(Random(seed)).take(count)
        }
    }
}
