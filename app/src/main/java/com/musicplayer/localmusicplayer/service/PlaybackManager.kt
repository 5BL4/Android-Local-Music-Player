package com.musicplayer.localmusicplayer.service

import android.content.ComponentName
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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
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
    private val managerScope = CoroutineScope(Main + SupervisorJob())
    private var svc: MusicPlaybackService? = null
    private var currentQueue: List<Song> = emptyList()
    private var pendingPlay: Pair<Song, List<Song>>? = null

    init {
        // Start the MusicPlaybackService by connecting a MediaController via a
        // ComponentName-based SessionToken. The framework starts the service, whose
        // onCreate() then calls bindSessionToken() to wire up `svc` and state collection.
        // Without this, the service is never created and all playback calls are no-ops.
        managerScope.launch {
            try {
                val sessionToken = SessionToken(
                    context,
                    ComponentName(context, MusicPlaybackService::class.java)
                )
                val future: ListenableFuture<MediaController> =
                    MediaController.Builder(context, sessionToken).buildAsync()
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
                Log.d(TAG, "MediaController ready, service started")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to build MediaController / start service", e)
            }
        }
    }

    fun bindSessionToken(token: SessionToken, service: MusicPlaybackService) {
        svc = service
        _audioSessionId.value = service.svcAudioSessionId()

        managerScope.launch {
            try {
                service.svcGetState().collect { state ->
                    _playbackState.value = state
                }
            } catch (e: Exception) {
                Log.e(TAG, "State collection failed", e)
            }
        }

        managerScope.launch {
            try {
                service.svcGetPosition().collect { pos ->
                    _currentPosition.value = pos
                }
            } catch (e: Exception) {
                Log.e(TAG, "Position collection failed", e)
            }
        }

        currentQueue = service.svcGetQueue()
        Log.d(TAG, "Service bound")

        // Execute any play request that arrived before the service was bound.
        pendingPlay?.let { (song, queue) ->
            currentQueue = queue
            service.svcPlay(song, queue)
            pendingPlay = null
        }
    }

    fun play(song: Song, queue: List<Song>) {
        currentQueue = queue
        val s = svc
        if (s != null) {
            s.svcPlay(song, queue)
        } else {
            // Service not yet bound — queue the request for when bindSessionToken completes.
            pendingPlay = song to queue
        }
    }

    fun togglePlayPause() { svc?.svcTogglePlayPause() }
    fun skipToNext() { svc?.svcSkipToNext() }
    fun skipToPrevious() { svc?.svcSkipToPrevious() }
    fun seekTo(positionMs: Long) { svc?.svcSeekTo(positionMs) }
    fun setRepeatMode(mode: RepeatMode) { svc?.svcSetRepeatMode(mode) }
    fun toggleShuffle() { svc?.svcToggleShuffle() }
    fun getCurrentQueue(): List<Song> = svc?.svcGetQueue() ?: currentQueue

    fun removeDeletedSongs(songIds: List<Long>) {
        svc?.svcRemoveDeletedSongs(songIds)
    }

    fun updateAudioSessionId(sessionId: Int) {
        _audioSessionId.value = sessionId
    }

    fun release() {
        managerScope.cancel()
        mediaController?.release()
        mediaController = null
    }
}
