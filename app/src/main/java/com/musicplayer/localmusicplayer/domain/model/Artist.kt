package com.musicplayer.localmusicplayer.domain.model

data class Artist(
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val albumArtUri: String? = null
)
