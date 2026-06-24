package com.musicplayer.localmusicplayer.presentation.equalizer

import androidx.lifecycle.ViewModel
import com.musicplayer.localmusicplayer.domain.model.EqualizerPreset
import com.musicplayer.localmusicplayer.domain.repository.EqualizerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

data class EqualizerUiState(
    val isEnabled: Boolean = false,
    val bands: List<BandState> = emptyList(),
    val presets: List<EqualizerPreset> = emptyList(),
    val currentPresetIndex: Int = 0,
    val isInitialized: Boolean = false
)

data class BandState(
    val index: Int,
    val frequencyHz: Int,
    val levelDb: Int
)

@HiltViewModel
class EqualizerViewModel @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EqualizerUiState())
    val uiState: StateFlow<EqualizerUiState> = _uiState.asStateFlow()

    init {
        // Software EQ is always available — no need to wait for an audio session.
        refreshState()
        _uiState.update { it.copy(isInitialized = true) }
    }

    private fun refreshState() {
        _uiState.update {
            it.copy(
                isEnabled = equalizerRepository.isEnabled,
                bands = equalizerRepository.bandFrequencies.mapIndexed { index, freq ->
                    BandState(
                        index = index,
                        frequencyHz = freq,
                        levelDb = equalizerRepository.bandLevels.getOrElse(index) { 0 }
                    )
                },
                presets = equalizerRepository.presets,
                currentPresetIndex = equalizerRepository.currentPresetIndex
            )
        }
    }

    fun toggleEnabled() {
        val newState = !_uiState.value.isEnabled
        equalizerRepository.setEnabled(newState)
        _uiState.update { it.copy(isEnabled = newState) }
    }

    fun onBandLevelChanged(band: Int, level: Int) {
        equalizerRepository.setBandLevel(band, level)
        _uiState.update { state ->
            state.copy(
                bands = state.bands.map {
                    if (it.index == band) it.copy(levelDb = level) else it
                },
                currentPresetIndex = equalizerRepository.currentPresetIndex
            )
        }
    }

    fun onPresetSelected(presetIndex: Int) {
        equalizerRepository.usePreset(presetIndex)
        refreshState()
    }
}
