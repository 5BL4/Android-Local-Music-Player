package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.LyricLine
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.LyricsLocalDataSource
import javax.inject.Inject

class ParseLyricsUseCase @Inject constructor(
    private val lyricsLocalDataSource: LyricsLocalDataSource
) {
    suspend operator fun invoke(song: Song): Result<List<LyricLine>> {
        return lyricsLocalDataSource.findAndParse(song)
    }
}
