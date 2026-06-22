package com.musicplayer.localmusicplayer.di

import android.content.Context
import androidx.room.Room
import com.musicplayer.localmusicplayer.data.local.db.AppDatabase
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistDao
import com.musicplayer.localmusicplayer.data.local.db.dao.PlaylistSongCrossRefDao
import com.musicplayer.localmusicplayer.data.local.db.dao.SongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "localmusicplayer.db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun provideSongDao(database: AppDatabase): SongDao = database.songDao()

    @Provides
    fun providePlaylistDao(database: AppDatabase): PlaylistDao = database.playlistDao()

    @Provides
    fun providePlaylistSongCrossRefDao(database: AppDatabase): PlaylistSongCrossRefDao =
        database.playlistSongCrossRefDao()
}
