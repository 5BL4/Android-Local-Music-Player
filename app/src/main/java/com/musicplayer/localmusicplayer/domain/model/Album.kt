package com.musicplayer.localmusicplayer.domain.model

data class Album(
    val albumId: Long,
    val name: String,
    val artist: String,
    val albumArtUri: String?,
    val songCount: Int
)
