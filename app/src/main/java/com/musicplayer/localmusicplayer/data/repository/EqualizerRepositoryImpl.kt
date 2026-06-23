package com.musicplayer.localmusicplayer.data.repository

import android.media.audiofx.Equalizer
import com.musicplayer.localmusicplayer.domain.model.EqualizerPreset
import com.musicplayer.localmusicplayer.domain.repository.EqualizerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EqualizerRepositoryImpl @Inject constructor() : EqualizerRepository {

    private var equalizer: Equalizer? = null
    private var audioSessionId: Int = 0

    override val isEnabled: Boolean
        get() = equalizer?.enabled ?: false

    override val numberOfBands: Int
        get() = equalizer?.numberOfBands?.toInt() ?: 0

    override val bandFrequencies: List<Int>
        get() = equalizer?.let { eq ->
            (0 until eq.numberOfBands).map { eq.getCenterFreq(it.toShort()).toInt() }
        } ?: emptyList()

    override val bandLevels: List<Int>
        get() = equalizer?.let { eq ->
            (0 until eq.numberOfBands).map { eq.getBandLevel(it.toShort()).toInt() }
        } ?: emptyList()

    override val presets: List<EqualizerPreset>
        get() = equalizer?.let { eq ->
            (0 until eq.numberOfPresets).map { i ->
                EqualizerPreset(index = i, name = eq.getPresetName(i.toShort()))
            }
        } ?: emptyList()

    override val currentPresetIndex: Int
        get() = equalizer?.currentPreset?.toInt() ?: 0

    override fun setEnabled(enabled: Boolean) {
        equalizer?.enabled = enabled
    }

    override fun setBandLevel(band: Int, level: Int) {
        equalizer?.setBandLevel(band.toShort(), level.toShort())
    }

    override fun usePreset(presetIndex: Int) {
        equalizer?.usePreset(presetIndex.toShort())
    }

    override fun setAudioSessionId(sessionId: Int) {
        if (sessionId != audioSessionId && sessionId != 0) {
            release()
            audioSessionId = sessionId
            try {
                equalizer = Equalizer(0, sessionId).apply {
                    enabled = false
                }
            } catch (e: Exception) {
                equalizer = null
            }
        }
    }

    override fun release() {
        equalizer?.release()
        equalizer = null
        audioSessionId = 0
    }
}
