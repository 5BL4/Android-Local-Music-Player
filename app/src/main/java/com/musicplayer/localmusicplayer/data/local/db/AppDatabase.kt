package com.musicplayer.localmusicplayer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistDao
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistSongCrossRefDao
import com.musicplayer.localmusicplayer.data.local.db.dao.SongDao
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistEntity
import com.musicplayer.localmusicplayer.data.local.db.entity.PlaylistSongCrossRef
import com.musicplayer.localmusicplayer.data.local.db.entity.SongEntity

@Database(
    entities = [
        SongEntity::class,
        PlaylistEntity::class,
        PlaylistSongCrossRef::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun playlistSongCrossRefDao(): PlaylistSongCrossRefDao
}
