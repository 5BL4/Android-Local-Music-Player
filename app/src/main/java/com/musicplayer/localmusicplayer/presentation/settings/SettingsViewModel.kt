package com.musicplayer.localmusicplayer.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.data.preference.AppPreferences
import com.musicplayer.localmusicplayer.domain.model.Language
import com.musicplayer.localmusicplayer.domain.model.SortOption
import com.musicplayer.localmusicplayer.domain.model.ThemeColor
import com.musicplayer.localmusicplayer.domain.model.ThemeMode
import com.musicplayer.localmusicplayer.domain.repository.ThemeRepository
import com.musicplayer.localmusicplayer.domain.usecase.ScanMusicFilesUseCase
import com.musicplayer.localmusicplayer.domain.usecase.SleepTimerUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.System,
    val themeColor: ThemeColor = ThemeColor.Blue,
    val useDynamicColor: Boolean = true,
    val defaultSortOption: SortOption = SortOption.Title,
    val language: Language = Language.English,
    val isSleepTimerActive: Boolean = false,
    val sleepTimerRemainingMinutes: Int = 0,
    val showSleepTimerDialog: Boolean = false,
    val isScanning: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val themeRepository: ThemeRepository,
    private val scanMusicFilesUseCase: ScanMusicFilesUseCase,
    private val sleepTimerUseCase: SleepTimerUseCase,
    private val appPreferences: AppPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            themeRepository.themeMode.collect { mode ->
                _uiState.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            themeRepository.useDynamicColor.collect { useDynamic ->
                _uiState.update { it.copy(useDynamicColor = useDynamic) }
            }
        }
        viewModelScope.launch {
            themeRepository.defaultSortOption.collect { sort ->
                _uiState.update { it.copy(defaultSortOption = sort) }
            }
        }
        viewModelScope.launch {
            themeRepository.language.collect { lang ->
                _uiState.update { it.copy(language = lang) }
            }
        }
        viewModelScope.launch {
            themeRepository.themeColor.collect { color ->
                _uiState.update { it.copy(themeColor = color) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { themeRepository.setThemeMode(mode) }
    }

    fun setThemeColor(color: ThemeColor) {
        viewModelScope.launch { themeRepository.setThemeColor(color) }
    }

    fun setUseDynamicColor(useDynamic: Boolean) {
        viewModelScope.launch { themeRepository.setUseDynamicColor(useDynamic) }
    }

    fun setDefaultSortOption(option: SortOption) {
        viewModelScope.launch { themeRepository.setDefaultSortOption(option) }
    }

    fun setLanguage(language: Language) {
        viewModelScope.launch { themeRepository.setLanguage(language) }
    }

    fun rescanMusic() {
        viewModelScope.launch {
            _uiState.update { it.copy(isScanning = true) }
            scanMusicFilesUseCase()
            _uiState.update { it.copy(isScanning = false) }
        }
    }

    fun showSleepTimerDialog() {
        _uiState.update { it.copy(
            showSleepTimerDialog = true,
            isSleepTimerActive = sleepTimerUseCase.isTimerActive
        ) }
    }

    fun hideSleepTimerDialog() {
        _uiState.update { it.copy(showSleepTimerDialog = false) }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerUseCase.startTimer(minutes) {
            _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerRemainingMinutes = 0) }
        }
        _uiState.update { it.copy(
            isSleepTimerActive = true,
            sleepTimerRemainingMinutes = minutes,
            showSleepTimerDialog = false
        ) }
    }

    fun cancelSleepTimer() {
        sleepTimerUseCase.cancelTimer()
        _uiState.update { it.copy(isSleepTimerActive = false, sleepTimerRemainingMinutes = 0) }
    }

    fun dumpLogs() {
        appPreferences.logAllPrefs()
    }
}
