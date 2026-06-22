package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.Playlist
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class UpdatePlaylistUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend operator fun invoke(playlist: Playlist) {
        playlistRepository.updatePlaylist(playlist)
    }
}
