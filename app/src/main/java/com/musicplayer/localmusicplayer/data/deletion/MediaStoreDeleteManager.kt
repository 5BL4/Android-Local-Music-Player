package com.musicplayer.localmusicplayer.data.deletion

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.os.Build
import android.provider.MediaStore
import com.musicplayer.localmusicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDeleteManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed class AttemptResult {
        object Done : AttemptResult()
        data class NeedsConfirm(val requestId: Long) : AttemptResult()
        object Error : AttemptResult()
    }

    private data class PendingDelete(
        val songIds: List<Long>,
        val mediaStoreIds: List<Long>,
        val intentSenders: List<IntentSender>,
        var currentSenderIndex: Int = 0
    )

    private val pending = ConcurrentHashMap<Long, PendingDelete>()
    private val nextId = AtomicLong(1L)

    suspend fun attemptDelete(
        songs: List<Song>,
        onDirectlyDeleted: suspend (List<Long>) -> Unit
    ): AttemptResult = withContext(Dispatchers.IO) {
        if (songs.isEmpty()) return@withContext AttemptResult.Done

        val directlyDeletedIds = mutableListOf<Long>()
        val failedUris = mutableListOf<android.net.Uri>()
        val failedSongIds = mutableListOf<Long>()
        val pendingIntents = mutableListOf<IntentSender>()

        for (song in songs) {
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaStoreId
            )
            try {
                val rows = context.contentResolver.delete(uri, null, null)
                if (rows > 0) {
                    directlyDeletedIds.add(song.id)
                } else {
                    // API>=30: 可能需要确认；API<=28: 文件可能已不存在
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        failedUris.add(uri)
                        failedSongIds.add(song.id)
                    }
                }
            } catch (e: RecoverableSecurityException) {
                // API 29+: 需要用户确认
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    // API>=30: 收集 URI，稍后批量 createDeleteRequest
                    failedUris.add(uri)
                    failedSongIds.add(song.id)
                } else {
                    // API 29: 收集 userAction.pendingIntent，逐个确认
                    pendingIntents.add(e.userAction.actionIntent.intentSender)
                    failedSongIds.add(song.id)
                }
            } catch (e: SecurityException) {
                // API<=28: 权限不足，返回 Error（UI 层会先请求写权限再重试）
                if (directlyDeletedIds.isNotEmpty()) {
                    onDirectlyDeleted(directlyDeletedIds)
                }
                return@withContext AttemptResult.Error
            } catch (e: Exception) {
                // 其他异常（文件不存在等），跳过
            }
        }

        // 立即回调已直接删除的 songIds
        if (directlyDeletedIds.isNotEmpty()) {
            onDirectlyDeleted(directlyDeletedIds)
        }

        // 检查是否有需要确认的文件
        if (failedSongIds.isEmpty() && pendingIntents.isEmpty()) {
            return@withContext AttemptResult.Done
        }

        val requestId = nextId.getAndIncrement()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // API>=30: 批量 createDeleteRequest
            val intentSender = if (failedUris.isNotEmpty()) {
                MediaStore.createDeleteRequest(context.contentResolver, failedUris).intentSender
            } else null
            pending[requestId] = PendingDelete(
                songIds = failedSongIds,
                mediaStoreIds = emptyList(),
                intentSenders = listOfNotNull(intentSender),
                currentSenderIndex = 0
            )
        } else {
            // API 29: 逐个 IntentSender
            pending[requestId] = PendingDelete(
                songIds = failedSongIds,
                mediaStoreIds = emptyList(),
                intentSenders = pendingIntents,
                currentSenderIndex = 0
            )
        }

        return@withContext AttemptResult.NeedsConfirm(requestId)
    }

    fun intentSenderFor(requestId: Long): IntentSender? {
        val pd = pending[requestId] ?: return null
        return pd.intentSenders.getOrNull(pd.currentSenderIndex)
    }

    fun onConfirmed(requestId: Long): List<Long>? {
        val pd = pending[requestId] ?: return null
        pd.currentSenderIndex++
        // API>=30: 单个批量 sender，确认后返回所有 songIds
        // API29: 逐个确认，全部确认后返回所有 songIds
        if (pd.currentSenderIndex >= pd.intentSenders.size) {
            return pd.songIds
        }
        // API29 还有更多 sender 需要确认，返回空列表表示"继续确认下一个"
        return emptyList()
    }

    fun hasMoreSenders(requestId: Long): Boolean {
        val pd = pending[requestId] ?: return false
        return pd.currentSenderIndex < pd.intentSenders.size
    }

    fun cancel(requestId: Long) {
        pending.remove(requestId)
    }

    fun clear(requestId: Long) {
        pending.remove(requestId)
    }
}
