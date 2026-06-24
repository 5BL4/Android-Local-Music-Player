package com.musicplayer.localmusicplayer.data.repository

import com.musicplayer.localmusicplayer.audio.EqualizerAudioProcessor
import com.musicplayer.localmusicplayer.domain.model.EqualizerPreset
import com.musicplayer.localmusicplayer.domain.repository.EqualizerRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Software-based equalizer repository backed by [EqualizerAudioProcessor].
 *
 * Unlike the previous implementation (which wrapped Android's system-level
 * `android.media.audiofx.Equalizer` with device-dependent band counts and
 * system-defined presets), this repository uses a fixed 10-band configuration
 * with app-defined presets. No audio session ID is required — the
 * [EqualizerAudioProcessor] is inserted directly into ExoPlayer's audio
 * pipeline via [EqualizerRenderersFactory].
 *
 * Band levels are stored in centi-dB (0.01 dB) units to match the UI slider
 * range of -1500..1500 (= -15 dB..+15 dB). Preset gains are defined in
 * integer dB and converted on apply.
 */
@Singleton
class EqualizerRepositoryImpl @Inject constructor(
    private val equalizerProcessor: EqualizerAudioProcessor
) : EqualizerRepository {

    companion object {
        private const val NUM_BANDS = 10
        private const val CUSTOM_PRESET_INDEX = 11
    }

    private var _isEnabled = false
    private var currentGains = IntArray(NUM_BANDS) { 0 } // centi-dB
    private var _currentPresetIndex = 0

    // ── Presets (gains in integer dB, order: 32,100,250,500,1k,2k,4k,8k,12k,16k) ──

    private val presetList = listOf(
        EqualizerPreset(0, "Pop", listOf(-2, 0, 1, 3, 3, 2, 1, -1, -1, -1)),
        EqualizerPreset(1, "Rock", listOf(5, 4, 3, -2, -1, 1, 2, 4, 4, 4)),
        EqualizerPreset(2, "Jazz", listOf(4, 2, 4, -3, -2, 0, 2, 4, 4, 5)),
        EqualizerPreset(3, "Classical", listOf(3, 2, 1, 0, 0, 1, 2, 2, 2, 2)),
        EqualizerPreset(4, "Dance", listOf(6, 4, 1, -1, -3, -2, 3, 5, 5, 4)),
        EqualizerPreset(5, "Hip Hop", listOf(5, 4, 3, -1, 0, 1, 2, 2, 2, 2)),
        EqualizerPreset(6, "R&B", listOf(2, 4, 3, -2, -1, 2, 2, 3, 3, 3)),
        EqualizerPreset(7, "Country", listOf(1, 2, 3, 3, 3, 2, 2, 2, 2, 1)),
        EqualizerPreset(8, "Heavy Metal", listOf(3, 2, 1, 4, 7, 5, 3, 2, 1, 0)),
        EqualizerPreset(9, "Folk", listOf(2, 1, 0, 0, 0, 1, 2, 1, 0, 0)),
        EqualizerPreset(10, "Latin", listOf(2, 1, 2, 3, 3, 4, 5, 5, 5, 5)),
        EqualizerPreset(11, "Custom", listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 0))
    )

    // ── Repository interface ──

    override val isEnabled: Boolean
        get() = _isEnabled

    override val numberOfBands: Int
        get() = NUM_BANDS

    override val bandFrequencies: List<Int>
        get() = EqualizerAudioProcessor.BAND_FREQUENCIES.map { it.toInt() }

    override val bandLevels: List<Int>
        get() = currentGains.toList()

    override val presets: List<EqualizerPreset>
        get() = presetList

    override val currentPresetIndex: Int
        get() = _currentPresetIndex

    override fun setEnabled(enabled: Boolean) {
        _isEnabled = enabled
        equalizerProcessor.setEnabled(enabled)
    }

    override fun setBandLevel(band: Int, level: Int) {
        if (band in 0 until NUM_BANDS) {
            currentGains[band] = level
            _currentPresetIndex = CUSTOM_PRESET_INDEX
            equalizerProcessor.setBandGain(band, level / 100f)
        }
    }

    override fun usePreset(presetIndex: Int) {
        val preset = presetList.getOrNull(presetIndex) ?: return
        _currentPresetIndex = presetIndex
        preset.gains.forEachIndexed { i, gainDb ->
            currentGains[i] = gainDb * 100
        }
        equalizerProcessor.setAllGains(preset.gains.map { it.toFloat() }.toFloatArray())
    }

    override fun release() {
        // AudioProcessor lifecycle is managed by ExoPlayer; nothing to do here.
    }
}
