package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class ControlPlaybackUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    fun togglePlayPause() = musicRepository.togglePlayPause()
    fun skipToNext() = musicRepository.skipToNext()
    fun skipToPrevious() = musicRepository.skipToPrevious()
    fun seekTo(positionMs: Long) = musicRepository.seekTo(positionMs)
    fun setRepeatMode(mode: RepeatMode) = musicRepository.setRepeatMode(mode)
    fun toggleShuffle() = musicRepository.toggleShuffle()
}
