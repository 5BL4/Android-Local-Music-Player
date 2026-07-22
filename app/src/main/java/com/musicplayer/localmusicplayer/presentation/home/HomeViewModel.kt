package com.musicplayer.localmusicplayer.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.musicplayer.localmusicplayer.data.preference.AppPreferences
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.usecase.GetDailyRecommendationsUseCase
import com.musicplayer.localmusicplayer.domain.usecase.PlaySongUseCase
import com.musicplayer.localmusicplayer.util.LunarCalendar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

data class HomeUiState(
    val gregorianLine: String = "",
    val weekdayLine: String = "",
    val lunarLine: String = "",
    val checkedIn: Boolean = false,
    val checkInJustTriggered: Boolean = false,
    val recommendations: List<Song> = emptyList(),
    val isRecommendationsLoading: Boolean = true
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val appPreferences: AppPreferences,
    private val getDailyRecommendationsUseCase: GetDailyRecommendationsUseCase,
    private val playSongUseCase: PlaySongUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val today: LocalDate = LocalDate.now()
    private val todayIso: String = today.toString()

    init {
        computeDateLines()
        observeCheckIn()
        observeRecommendations()
    }

    private fun computeDateLines() {
        val locale = Locale.getDefault()
        val isZh = locale.language == "zh"
        val gregorianPattern = if (isZh) "M\u6708d\u65e5" else "MMMM d"
        val weekdayPattern = "EEEE"
        val gregorianFormatter = runCatching {
            DateTimeFormatter.ofPattern(gregorianPattern, locale)
        }.getOrDefault(DateTimeFormatter.ofPattern(gregorianPattern))
        val weekdayFormatter = runCatching {
            DateTimeFormatter.ofPattern(weekdayPattern, locale)
        }.getOrDefault(DateTimeFormatter.ofPattern(weekdayPattern))

        val gregorianLine = runCatching { today.format(gregorianFormatter) }
            .getOrDefault("${today.monthValue}/${today.dayOfMonth}")
        val weekdayLine = runCatching { today.format(weekdayFormatter) }
            .getOrDefault(today.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() })

        val lunarLine = runCatching { LunarCalendar.formatLunarDate(LunarCalendar.today()) }
            .getOrDefault("")

        _uiState.update {
            it.copy(gregorianLine = gregorianLine, weekdayLine = weekdayLine, lunarLine = lunarLine)
        }
    }

    private fun observeCheckIn() {
        viewModelScope.launch {
            appPreferences.lastCheckInDate.collect { lastDate ->
                _uiState.update { it.copy(checkedIn = lastDate == todayIso) }
            }
        }
    }

    private fun observeRecommendations() {
        viewModelScope.launch {
            getDailyRecommendationsUseCase().collect { songs ->
                _uiState.update {
                    it.copy(recommendations = songs, isRecommendationsLoading = false)
                }
            }
        }
    }

    fun onDateCardClick() {
        if (_uiState.value.checkedIn) return
        viewModelScope.launch {
            // Optimistic flip so the UI responds immediately.
            _uiState.update { it.copy(checkedIn = true, checkInJustTriggered = true) }
            appPreferences.setLastCheckInDate(todayIso)
        }
    }

    fun onCheckInAnimationFinished() {
        _uiState.update { it.copy(checkInJustTriggered = false) }
    }

    fun playSong(song: Song) {
        viewModelScope.launch {
            val queue = _uiState.value.recommendations
            playSongUseCase(song, queue.ifEmpty { listOf(song) })
        }
    }
}
