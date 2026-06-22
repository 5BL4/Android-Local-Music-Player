package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import javax.inject.Inject

class ScanMusicFilesUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    suspend operator fun invoke() {
        musicRepository.scanMusicFiles()
    }
}
