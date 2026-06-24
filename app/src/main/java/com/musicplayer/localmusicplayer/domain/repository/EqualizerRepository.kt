package com.musicplayer.localmusicplayer.domain.repository

import com.musicplayer.localmusicplayer.domain.model.EqualizerPreset

interface EqualizerRepository {
    val isEnabled: Boolean
    val numberOfBands: Int
    val bandFrequencies: List<Int>
    val bandLevels: List<Int>
    val presets: List<EqualizerPreset>
    val currentPresetIndex: Int

    fun setEnabled(enabled: Boolean)
    fun setBandLevel(band: Int, level: Int)
    fun usePreset(presetIndex: Int)
    fun release()
}
