package com.musicplayer.localmusicplayer.data.mapper

import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity
import com.musicplayer.localmusicplayer.domain.model.Song

fun SongEntity.toDomain(): Song = Song(
    id = id,
    mediaStoreId = mediaStoreId,
    title = title,
    artist = artist,
    album = album,
    albumId = albumId,
    durationMs = duration,
    filePath = filePath,
    contentUri = contentUri,
    albumArtUri = albumArtUri,
    year = year,
    trackNumber = trackNumber,
    discNumber = discNumber,
    genre = genre
)

fun List<SongEntity>.toDomain(): List<Song> = map { it.toDomain() }
