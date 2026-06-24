package com.musicplayer.localmusicplayer.di

import com.musicplayer.localmusicplayer.audio.EqualizerAudioProcessor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the [EqualizerAudioProcessor] as a Singleton so that both
 * [MusicPlaybackService] (for ExoPlayer integration) and
 * [EqualizerRepositoryImpl] (for gain control) share the same instance.
 */
@Module
@InstallIn(SingletonComponent::class)
object AudioModule {

    @Provides
    @Singleton
    fun provideEqualizerAudioProcessor(): EqualizerAudioProcessor {
        return EqualizerAudioProcessor()
    }
}
