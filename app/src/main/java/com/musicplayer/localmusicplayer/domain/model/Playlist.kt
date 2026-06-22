package com.musicplayer.localmusicplayer.domain.model

data class Playlist(
    val id: Long = 0,
    val name: String,
    val songCount: Int = 0,
    val description: String? = null,
    val coverArtUri: String? = null,
    val createdAt: Long,
    val updatedAt: Long
)
