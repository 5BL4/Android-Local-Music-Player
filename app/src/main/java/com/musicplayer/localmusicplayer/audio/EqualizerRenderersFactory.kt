package com.musicplayer.localmusicplayer.audio

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink

/**
 * Custom [DefaultRenderersFactory] that injects the [EqualizerAudioProcessor]
 * into ExoPlayer's audio processing pipeline.
 *
 * `setEnableFloatOutput(false)` is critical — if float output is enabled,
 * Media3 bypasses all user-supplied AudioProcessors.
 */
@UnstableApi
class EqualizerRenderersFactory(
    context: Context,
    private val equalizerProcessor: EqualizerAudioProcessor
) : DefaultRenderersFactory(context) {

    override fun buildAudioSink(
        context: Context,
        enableFloatOutput: Boolean,
        enableAudioTrackPlaybackParams: Boolean
    ): AudioSink {
        return DefaultAudioSink.Builder(context)
            .setEnableFloatOutput(false)
            .setAudioProcessors(arrayOf(equalizerProcessor))
            .build()
    }
}
