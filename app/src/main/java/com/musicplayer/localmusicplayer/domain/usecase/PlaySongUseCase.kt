package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class PlaySongUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(song: Song, queue: List<Song>) {
        musicRepository.play(song, queue)
    }
}
