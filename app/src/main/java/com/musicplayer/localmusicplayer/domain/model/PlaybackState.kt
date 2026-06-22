package com.musicplayer.localmusicplayer.domain.model

sealed class PlaybackState {
    data object Idle : PlaybackState()
    data class Playing(
        val currentSong: Song,
        val currentIndex: Int,
        val queueSize: Int,
        val currentPositionMs: Long,
        val durationMs: Long,
        val isShuffleEnabled: Boolean,
        val repeatMode: RepeatMode
    ) : PlaybackState()
    data class Paused(
        val currentSong: Song,
        val currentIndex: Int,
        val queueSize: Int,
        val currentPositionMs: Long,
        val durationMs: Long,
        val isShuffleEnabled: Boolean,
        val repeatMode: RepeatMode
    ) : PlaybackState()
    data object Buffering : PlaybackState()
    data class Error(val message: String) : PlaybackState()
}
