package com.musicplayer.localmusicplayer.di

import com.musicplayer.localmusicplayer.data.repository.EqualizerRepositoryImpl
import com.musicplayer.localmusicplayer.data.repository.MusicRepositoryImpl
import com.musicplayer.localmusicplayer.data.repository.PlaylistRepositoryImpl
import com.musicplayer.localmusicplayer.data.repository.ThemeRepositoryImpl
import com.musicplayer.localmusicplayer.domain.repository.EqualizerRepository
import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import com.musicplayer.localmusicplayer.domain.repository.PlaylistRepository
import com.musicplayer.localmusicplayer.domain.repository.ThemeRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    @Binds
    @Singleton
    abstract fun bindPlaylistRepository(impl: PlaylistRepositoryImpl): PlaylistRepository

    @Binds
    @Singleton
    abstract fun bindThemeRepository(impl: ThemeRepositoryImpl): ThemeRepository

    @Binds
    @Singleton
    abstract fun bindEqualizerRepository(impl: EqualizerRepositoryImpl): EqualizerRepository
}
