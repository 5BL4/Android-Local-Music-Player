package com.musicplayer.localmusicplayer.domain.usecase

import com.musicplayer.localmusicplayer.domain.repository.MusicRepository
import kotlinx.coroutines.*
import javax.inject.Inject

class SleepTimerUseCase @Inject constructor(
    private val musicRepository: MusicRepository
) {
    private var timerJob: Job? = null

    fun startTimer(minutes: Int, onFinished: () -> Unit) {
        timerJob?.cancel()
        timerJob = CoroutineScope(Dispatchers.Default).launch {
            delay(minutes * 60 * 1000L)
            musicRepository.togglePlayPause() // will pause if playing
            withContext(Dispatchers.Main) {
                onFinished()
            }
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    val isTimerActive: Boolean
        get() = timerJob?.isActive == true
}
