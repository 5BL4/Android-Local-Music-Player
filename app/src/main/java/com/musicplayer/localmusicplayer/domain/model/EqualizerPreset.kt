package com.musicplayer.localmusicplayer.domain.model

data class EqualizerPreset(
    val index: Int,
    val name: String,
    val gains: List<Int>
)
