package com.musicplayer.localmusicplayer.di

import com.musicplayer.localmusicplayer.data.local.datasource.LyricsFileDataSource
import com.musicplayer.localmusicplayer.domain.repository.LyricsLocalDataSource
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {

    @Binds
    @Singleton
    abstract fun bindLyricsLocalDataSource(impl: LyricsFileDataSource): LyricsLocalDataSource
}
