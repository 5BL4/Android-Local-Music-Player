package com.musicplayer.localmusicplayer.domain.repository

import com.musicplayer.localmusicplayer.domain.model.LyricLine
import com.musicplayer.localmusicplayer.domain.model.Song

/**
 * Domain-layer contract for reading local lyrics files.
 *
 * Defined in the domain layer so [ParseLyricsUseCase] can depend on this
 * abstraction instead of the concrete data-layer [LyricsFileDataSource],
 * keeping the dependency rule of Clean Architecture (domain must not import
 * data) and allowing the use case to run in a pure JVM unit test with a
 * mocked implementation.
 */
interface LyricsLocalDataSource {
    fun findAndParse(song: Song): Result<List<LyricLine>>
}
