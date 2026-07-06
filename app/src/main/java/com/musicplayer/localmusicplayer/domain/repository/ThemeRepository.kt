package com.musicplayer.localmusicplayer.domain.repository

import com.musicplayer.localmusicplayer.domain.model.Language
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.model.ThemeColor
import com.musicplayer.localmusicplayer.domain.model.ThemeMode
import com.musicplayer.localmusicplayer.domain.model.WaveformStyle
import kotlinx.coroutines.flow.Flow

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>
    val useDynamicColor: Flow<Boolean>
    val defaultSortOption: Flow<SortOption>
    val language: Flow<Language>
    val themeColor: Flow<ThemeColor>
    val waveformStyle: Flow<WaveformStyle>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setThemeColor(color: ThemeColor)
    suspend fun setUseDynamicColor(useDynamic: Boolean)
    suspend fun setDefaultSortOption(option: SortOption)
    suspend fun setLanguage(language: Language)
    suspend fun setWaveformStyle(style: WaveformStyle)
}
