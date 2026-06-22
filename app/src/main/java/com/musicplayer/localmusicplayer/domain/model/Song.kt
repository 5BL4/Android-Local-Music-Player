package com.musicplayer.localmusicplayer.domain.model

data class Song(
    val id: Long = 0,
    val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumId: Long,
    val durationMs: Long,
    val filePath: String?,
    val contentUri: String?,
    val albumArtUri: String?,
    val year: Int?,
    val trackNumber: Int?,
    val discNumber: Int?,
    val genre: String?
)
