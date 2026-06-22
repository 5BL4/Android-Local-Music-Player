package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.Album
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAlbumsUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    operator fun invoke(): Flow<List<Album>> = musicRepository.albums
}
