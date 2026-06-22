package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.model.EqualizerPreset
import com.musicplayer.localmusicplayer.domain.repository.EqualizerRepository
import javax.inject.Inject

class GetEqualizerPresetsUseCase @Inject constructor(
    private val equalizerRepository: EqualizerRepository
) {
    operator fun invoke(): List<EqualizerPreset> = equalizerRepository.presets
}
