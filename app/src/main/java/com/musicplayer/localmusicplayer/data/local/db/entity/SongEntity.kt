package com.musicplayer.localmusicplayer.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "songs",
    indices = [
        Index(value = ["media_store_id"], unique = true),
        Index(value = ["album_id"]),
        Index(value = ["artist"]),
        Index(value = ["title"])
    ]
)
data class SongEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "media_store_id") val mediaStoreId: Long,
    val title: String,
    val artist: String,
    val album: String,
    @ColumnInfo(name = "album_id") val albumId: Long,
    val duration: Long,
    @ColumnInfo(name = "file_path") val filePath: String?,
    @ColumnInfo(name = "content_uri") val contentUri: String?,
    @ColumnInfo(name = "album_art_uri") val albumArtUri: String?,
    @ColumnInfo(name = "date_added") val dateAdded: Long,
    val year: Int?,
    @ColumnInfo(name = "track_number") val trackNumber: Int?,
    @ColumnInfo(name = "disc_number") val discNumber: Int?,
    val genre: String?,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    val size: Long
)
