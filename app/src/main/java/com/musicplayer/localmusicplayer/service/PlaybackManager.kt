package com.musicplayer.localmusicplayer.service

import android.content.Context
import android.util.Log
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class PlaybackManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "PlaybackManager"
    }

    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    val playbackState: StateFlow<PlaybackState> = _playbackState.asStateFlow()

    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()

    private val _audioSessionId = MutableStateFlow(0)
    val audioSessionId: Int get() = _audioSessionId.value

    private var mediaController: MediaController? = null
    private var svc: MusicPlaybackService? = null
    private var currentQueue: List<Song> = emptyList()

    fun bindSessionToken(token: SessionToken, service: MusicPlaybackService) {
        svc = service
        _audioSessionId.value = service.svcAudioSessionId()

        CoroutineScope(Main).launch {
            service.svcGetState().collect { state ->
                _playbackState.value = state
            }
        }

        CoroutineScope(Main).launch {
            service.svcGetPosition().collect { pos ->
                _currentPosition.value = pos
            }
        }

        // Build controller async for cross-process compliance
        CoroutineScope(Main).launch {
            try {
                val future: ListenableFuture<MediaController> =
                    MediaController.Builder(context, token).buildAsync()
                val controller = suspendCoroutine<MediaController> { cont ->
                    future.addListener({
                        try {
                            cont.resume(future.get())
                        } catch (e: Exception) {
                            cont.resumeWithException(e)
                        }
                    }, Executors.newSingleThreadExecutor())
                }
                mediaController = controller
                currentQueue = service.svcGetQueue()
                Log.d(TAG, "MediaController ready")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build MediaController", e)
            }
        }
    }

    fun play(song: Song, queue: List<Song>) {
        currentQueue = queue
        svc?.svcPlay(song, queue)
    }

    fun togglePlayPause() { svc?.svcTogglePlayPause() }
    fun skipToNext() { svc?.svcSkipToNext() }
    fun skipToPrevious() { svc?.svcSkipToPrevious() }
    fun seekTo(positionMs: Long) { svc?.svcSeekTo(positionMs) }
    fun setRepeatMode(mode: RepeatMode) { svc?.svcSetRepeatMode(mode) }
    fun toggleShuffle() { svc?.svcToggleShuffle() }
    fun getCurrentQueue(): List<Song> = svc?.svcGetQueue() ?: currentQueue

    fun release() {
        mediaController?.release()
        mediaController = null
    }
}
