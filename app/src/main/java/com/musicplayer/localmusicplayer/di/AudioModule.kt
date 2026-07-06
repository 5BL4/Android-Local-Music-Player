package com.musicplayer.localmusicplayer.di

import androidx.media3.exoplayer.audio.WaveformAudioBufferSink
import com.musicplayer.localmusicplayer.audio.EqualizerAudioProcessor
import com.musicplayer.localmusicplayer.service.PlaybackManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the [EqualizerAudioProcessor] and [WaveformAudioBufferSink] as
 * Singletons so that both [MusicPlaybackService] (for ExoPlayer integration) and
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

    @Provides
    @Singleton
    fun provideWaveformSink(playbackManager: PlaybackManager): WaveformAudioBufferSink {
        return WaveformAudioBufferSink(
            40,     // barsPerSecond
            1,      // outputChannelCount (mono mix for display)
            object : WaveformAudioBufferSink.Listener {
                override fun onNewWaveformBar(
                    channelIndex: Int,
                    waveformBar: WaveformAudioBufferSink.WaveformBar
                ) {
                    playbackManager.onNewAmplitude(waveformBar.maxSampleValue)
                }
            }
        )
    }
}
