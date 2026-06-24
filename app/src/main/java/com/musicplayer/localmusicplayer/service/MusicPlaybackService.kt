package com.musicplayer.localmusicplayer.service

import android.content.Intent
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.musicplayer.localmusicplayer.audio.EqualizerAudioProcessor
import com.musicplayer.localmusicplayer.audio.EqualizerRenderersFactory
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.domain.model.RepeatMode
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.util.Constants
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MediaSessionService — the SOLE owner of ExoPlayer.
 *
 * PlaybackManager (the MediaController wrapper) and ViewModels interact with playback
 * exclusively through this Service's public methods and StateFlows.
 */
@AndroidEntryPoint
class MusicPlaybackService : MediaSessionService() {

    @Inject lateinit var audioFocusManager: AudioFocusManager
    @Inject lateinit var mediaNotificationManager: MediaNotificationManager
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var equalizerAudioProcessor: EqualizerAudioProcessor

    private var mediaSession: MediaSession? = null
    private var exoPlayer: ExoPlayer? = null
    private var wasInForeground = false

    // Queue & mode state
    private var currentQueue: List<Song> = emptyList()
    private var originalQueue: List<Song> = emptyList()
    private var originalIndex: Int = -1
    private var currentIndex: Int = -1
    private var isShuffleEnabled: Boolean = false
    private var repeatMode: RepeatMode = RepeatMode.All
    private var positionUpdateJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var endedNormally: Boolean = false

    // State emitted to PlaybackManager / ViewModels
    private val _playbackState = MutableStateFlow<PlaybackState>(PlaybackState.Idle)
    private val _currentPosition = MutableStateFlow(0L)

    // ─── Lifecycle ──────────────────────────────────────────────

    override fun onCreate() {
        super.onCreate()
        try {
            val player = ExoPlayer.Builder(
                this,
                EqualizerRenderersFactory(this, equalizerAudioProcessor)
            ).build().apply {
                repeatMode = Player.REPEAT_MODE_OFF
                addListener(playbackListener)
            }
            exoPlayer = player

            // Audio focus
            audioFocusManager.onFocusLoss          = { Log.d("Svc", "Focus LOSS");        player.playWhenReady = false }
            audioFocusManager.onFocusLossTransient = { Log.d("Svc", "Focus LOSS_TRANS");  player.playWhenReady = false }
            audioFocusManager.onFocusGain          = { Log.d("Svc", "Focus GAIN");        if (player.mediaItemCount > 0) player.playWhenReady = true }
            audioFocusManager.onFocusDuck          = { Log.d("Svc", "Focus DUCK");        player.volume = 0.3f }
            audioFocusManager.onFocusGainAfterTransient = { Log.d("Svc", "Focus GAIN+"); player.volume = 1f; player.playWhenReady = true }

            // MediaSession bound to this Service's player
            mediaSession = MediaSession.Builder(this, player).build()

            // Hand the session token to PlaybackManager so UI can bind via MediaController
            val session = mediaSession
            if (session != null) {
                playbackManager.bindSessionToken(session.token, this)
            } else {
                Log.e("Svc", "MediaSession build returned null")
                stopSelf()
                return
            }

            Log.d("Svc", "Service created, session ready")
        } catch (e: Exception) {
            Log.e("Svc", "Failed to initialize playback service", e)
            exoPlayer?.release()
            exoPlayer = null
            mediaSession?.release()
            mediaSession = null
            stopSelf()
        }
    }

    override fun onGetSession(ci: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val p = exoPlayer
        if (p == null || !p.playWhenReady || p.mediaItemCount == 0) stopSelf()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "STOP_SERVICE") { stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY }
        launchNotificationObserver()
        return START_STICKY
    }

    override fun onDestroy() {
        stopPositionUpdates()
        serviceScope.cancel()
        mediaSession?.release(); mediaSession = null
        audioFocusManager.abandonFocus()
        exoPlayer?.release(); exoPlayer = null
        super.onDestroy()
    }

    // ─── Controls (called by PlaybackManager) ───────────────────

    fun svcPlay(song: Song, queue: List<Song>) {
        val p = exoPlayer ?: return
        // Guard against an empty queue (e.g. Paging source can't materialise the full list):
        // fall back to a single-item queue so playback actually starts.
        val effectiveQueue = queue.ifEmpty { listOf(song) }
        originalQueue = effectiveQueue; originalIndex = effectiveQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        if (isShuffleEnabled) {
            currentQueue = listOf(song) + effectiveQueue.filter { it.id != song.id }.shuffled()
            currentIndex = 0
        } else {
            currentQueue = effectiveQueue; currentIndex = originalIndex
        }
        p.setMediaItems(currentQueue.map { it.toMediaItem() }, currentIndex, 0L)
        p.playWhenReady = true; p.prepare()
    }

    fun svcTogglePlayPause() {
        val p = exoPlayer ?: return
        when (_playbackState.value) {
            is PlaybackState.Playing -> p.playWhenReady = false
            is PlaybackState.Paused  -> p.playWhenReady = true
            is PlaybackState.Idle    -> { if (currentQueue.isNotEmpty()) svcPlay(currentQueue.first(), currentQueue) }
            else -> {}
        }
    }

    fun svcSkipToNext() {
        val p = exoPlayer ?: return
        if (currentQueue.isEmpty()) return
        when (repeatMode) {
            RepeatMode.One -> { p.seekTo(0); p.playWhenReady = true }
            else -> {
                val next = if (currentIndex < currentQueue.size - 1) {
                    currentIndex + 1
                } else if (repeatMode == RepeatMode.All) {
                    0
                } else {
                    return // RepeatMode.Off at last item: do nothing
                }
                if (next in currentQueue.indices) {
                    currentIndex = next
                    p.seekToDefaultPosition(next)
                    p.playWhenReady = true
                }
            }
        }
    }

    fun svcSkipToPrevious() {
        val p = exoPlayer ?: return
        if (p.currentPosition > 3000) { p.seekTo(0) }
        else {
            val prev = if (currentIndex > 0) currentIndex - 1 else currentQueue.size - 1
            if (prev in currentQueue.indices) { currentIndex = prev; p.seekToDefaultPosition(prev); p.playWhenReady = true }
        }
    }

    fun svcSeekTo(posMs: Long) { exoPlayer?.seekTo(posMs) }
    fun svcSetRepeatMode(mode: RepeatMode) { repeatMode = mode; emitState() }
    fun svcToggleShuffle() {
        val p = exoPlayer ?: return
        isShuffleEnabled = !isShuffleEnabled
        if (currentQueue.isEmpty()) { emitState(); return }
        val song = currentQueue.getOrNull(currentIndex) ?: return
        if (isShuffleEnabled) {
            originalQueue = currentQueue.toList(); originalIndex = currentIndex
            currentQueue = listOf(song) + originalQueue.filter { it.id != song.id }.shuffled()
            currentIndex = 0
        } else {
            currentQueue = originalQueue
            currentIndex = originalQueue.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
        }
        p.setMediaItems(currentQueue.map { it.toMediaItem() }, currentIndex, p.currentPosition)
        emitState()
    }

    fun svcGetQueue(): List<Song> = currentQueue

    fun svcRemoveDeletedSongs(songIds: List<Long>) {
        if (songIds.isEmpty()) return
        val p = exoPlayer ?: return
        val wasPlayingCurrent = currentQueue.getOrNull(currentIndex)?.id in songIds

        // 从队列中移除被删歌曲
        val newQueue = currentQueue.filter { it.id !in songIds }
        val newOriginalQueue = originalQueue.filter { it.id !in songIds }

        if (newQueue.isEmpty()) {
            currentQueue = emptyList()
            originalQueue = emptyList()
            currentIndex = -1
            originalIndex = -1
            p.clearMediaItems()
            emitState()
            return
        }

        // 计算新的当前索引
        val currentSong = currentQueue.getOrNull(currentIndex)
        val newIndex = if (currentSong != null) {
            newQueue.indexOfFirst { it.id == currentSong.id }
        } else 0

        currentQueue = newQueue
        originalQueue = newOriginalQueue
        currentIndex = newIndex.coerceAtLeast(0)

        if (wasPlayingCurrent) {
            // 当前播放项被删，跳到新索引（即下一首）
            p.setMediaItems(currentQueue.map { it.toMediaItem() }, currentIndex, 0L)
            p.playWhenReady = true
            p.prepare()
        } else {
            // 当前播放项未删，保持播放位置
            val pos = p.currentPosition
            p.setMediaItems(currentQueue.map { it.toMediaItem() }, currentIndex, pos)
        }
        emitState()
    }

    fun svcAudioSessionId(): Int = exoPlayer?.audioSessionId ?: 0
    fun svcGetState(): StateFlow<PlaybackState> = _playbackState
    fun svcGetPosition(): StateFlow<Long> = _currentPosition

    // ─── Internal ───────────────────────────────────────────────

    private val playbackListener = object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            emitState()
            when (state) {
                Player.STATE_READY -> { if (exoPlayer?.playWhenReady == true) startPositionUpdates(); endedNormally = false }
                Player.STATE_ENDED -> {
                    stopPositionUpdates()
                    if (!endedNormally) {
                        endedNormally = true
                        when (repeatMode) {
                            RepeatMode.All  -> { exoPlayer?.seekToDefaultPosition(0); exoPlayer?.playWhenReady = true }
                            RepeatMode.One  -> { exoPlayer?.seekTo(0);              exoPlayer?.playWhenReady = true }
                            RepeatMode.Off  -> { exoPlayer?.playWhenReady = false;  emitState() }
                        }
                    }
                }
                else -> stopPositionUpdates()
            }
        }
        override fun onIsPlayingChanged(isPlaying: Boolean) { emitState(); if (isPlaying) startPositionUpdates() else stopPositionUpdates() }
        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
            val idx = exoPlayer?.currentMediaItemIndex ?: -1
            if (idx != currentIndex && idx in currentQueue.indices) { currentIndex = idx; emitState() }
        }
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            Log.e("Svc", "Player error, skipping to next", error)
            // 播放错误（如文件已被删除），跳到下一首
            svcSkipToNext()
        }
    }

    private fun emitState() {
        val p = exoPlayer
        // Sync audio session id to PlaybackManager — ExoPlayer assigns the real
        // session id after prepare, so the value read at bindSessionToken time
        // (service onCreate, before any media is loaded) is 0.
        playbackManager.updateAudioSessionId(p?.audioSessionId ?: 0)
        val song = currentQueue.getOrNull(currentIndex)
        if (song == null) { _playbackState.value = PlaybackState.Idle; return }
        _playbackState.value = when {
            p == null               -> PlaybackState.Idle
            !p.playWhenReady        -> PlaybackState.Paused(song, currentIndex, currentQueue.size, p.currentPosition, p.duration.takeIf { it > 0 } ?: song.durationMs, isShuffleEnabled, repeatMode)
            p.isPlaying            -> PlaybackState.Playing(song, currentIndex, currentQueue.size, p.currentPosition, p.duration.takeIf { it > 0 } ?: song.durationMs, isShuffleEnabled, repeatMode)
            else                   -> PlaybackState.Buffering
        }
    }

    private fun startPositionUpdates() {
        positionUpdateJob?.cancel()
        positionUpdateJob = serviceScope.launch {
            while (isActive) { exoPlayer?.let { _currentPosition.value = it.currentPosition }; delay(Constants.POSITION_UPDATE_INTERVAL_MS) }
        }
    }
    private fun stopPositionUpdates() { positionUpdateJob?.cancel(); positionUpdateJob = null }

    private fun launchNotificationObserver() {
        serviceScope.launch {
            try {
                _playbackState.collect { state ->
                    when (state) {
                        is PlaybackState.Playing -> {
                            if (!wasInForeground) audioFocusManager.requestFocus()
                            startForeground(Constants.NOTIFICATION_ID, mediaNotificationManager.buildNotification(state.currentSong, true))
                            wasInForeground = true
                        }
                        is PlaybackState.Paused -> {
                            mediaNotificationManager.notify(mediaNotificationManager.buildNotification(state.currentSong, false))
                            stopForeground(false); wasInForeground = false
                        }
                        is PlaybackState.Idle -> {
                            stopForeground(STOP_FOREGROUND_REMOVE); mediaNotificationManager.cancel()
                            audioFocusManager.abandonFocus(); wasInForeground = false; stopSelf()
                        }
                        else -> {}
                    }
                }
            } catch (e: Exception) {
                Log.e("Svc", "Notification observer failed", e)
            }
        }
    }
}

// MediaItem converter shared with PlaybackManager
internal fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId(id.toString())
    .setUri(filePath?.takeIf { it.isNotBlank() } ?: contentUri ?: "")
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title).setArtist(artist).setAlbumTitle(album)
            .apply { albumArtUri?.let { setArtworkUri(android.net.Uri.parse(it)) } }
            .build()
    ).build()
