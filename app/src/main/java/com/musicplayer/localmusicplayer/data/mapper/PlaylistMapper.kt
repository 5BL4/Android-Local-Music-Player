package com.musicplayer.localmusicplayer.data.mapper

import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistEntity
import com.musicplayer.localmusicplayer.domain.model.Playlist

fun PlaylistEntity.toDomain(songCount: Int = 0): Playlist = Playlist(
    id = id,
    name = name,
    songCount = songCount,
    description = description,
    coverArtUri = coverArtUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)

fun Playlist.toEntity(): PlaylistEntity = PlaylistEntity(
    id = id,
    name = name,
    description = description,
    coverArtUri = coverArtUri,
    createdAt = createdAt,
    updatedAt = updatedAt
)
