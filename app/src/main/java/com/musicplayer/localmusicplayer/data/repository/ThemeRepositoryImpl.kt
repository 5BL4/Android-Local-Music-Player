package com.musicplayer.localmusicplayer.data.repository

import com.musicplayer.localmusicplayer.data.preference.AppPreferences
import com.musicplayer.localmusicplayer.domain.model.Language
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.model.ThemeColor
import com.musicplayer.localmusicplayer.domain.model.ThemeMode
import com.musicplayer.localmusicplayer.domain.model.WaveformStyle
import com.musicplayer.localmusicplayer.domain.repository.ThemeRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    private val appPreferences: AppPreferences
) : ThemeRepository {

    override val themeMode: Flow<ThemeMode> = appPreferences.themeMode
    override val useDynamicColor: Flow<Boolean> = appPreferences.useDynamicColor
    override val defaultSortOption: Flow<SortOption> = appPreferences.defaultSortOption
    override val language: Flow<Language> = appPreferences.language
    override val waveformStyle: Flow<WaveformStyle> = appPreferences.waveformStyle

    override suspend fun setThemeMode(mode: ThemeMode) = appPreferences.setThemeMode(mode)
    override suspend fun setUseDynamicColor(useDynamic: Boolean) = appPreferences.setUseDynamicColor(useDynamic)
    override suspend fun setDefaultSortOption(option: SortOption) = appPreferences.setDefaultSortOption(option)
    override val themeColor: Flow<ThemeColor> = appPreferences.themeColor

    override suspend fun setLanguage(language: Language) = appPreferences.setLanguage(language)
    override suspend fun setWaveformStyle(style: WaveformStyle) = appPreferences.setWaveformStyle(style)
    override suspend fun setThemeColor(color: ThemeColor) = appPreferences.setThemeColor(color)
}
