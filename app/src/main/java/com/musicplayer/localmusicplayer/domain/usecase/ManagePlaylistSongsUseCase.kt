package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import javax.inject.Inject

class ManagePlaylistSongsUseCase @Inject constructor(
    private val playlistRepository: PlaylistRepository
) {
    suspend fun addSong(playlistId: Long, songId: Long) {
        playlistRepository.addSongToPlaylist(playlistId, songId)
    }

    suspend fun removeSong(playlistId: Long, songId: Long) {
        playlistRepository.removeSongFromPlaylist(playlistId, songId)
    }

    suspend fun reorder(playlistId: Long, songIds: List<Long>) {
        playlistRepository.reorderPlaylist(playlistId, songIds)
    }
}
