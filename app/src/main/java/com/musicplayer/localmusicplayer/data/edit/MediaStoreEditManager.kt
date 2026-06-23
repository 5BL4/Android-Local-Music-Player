package com.musicplayer.localmusicplayer.data.edit

import android.app.RecoverableSecurityException
import android.content.ContentUris
import android.content.Context
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.musicplayer.localmusicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import org.jaudiotagger.tag.Tag
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages audio metadata (tag) editing with Android version-aware write permission handling.
 *
 * Flow:
 *  1. Copy the audio file from MediaStore to a cache temp file (needs only READ permission).
 *  2. Edit the temp file's tags with JAudioTagger (cache dir is always writable).
 *  3. Write the temp file back to MediaStore via openOutputStream (needs WRITE permission on API 10+).
 *     - If this throws RecoverableSecurityException (API 29) / SecurityException (API 30+),
 *       collect the URI and temp file, then request permission via createWriteRequest.
 *  4. After user confirms, retry step 3 using the saved temp file.
 */
@Singleton
class MediaStoreEditManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    sealed class AttemptResult {
        object Done : AttemptResult()
        data class NeedsConfirm(val requestId: Long) : AttemptResult()
        object Error : AttemptResult()
    }

    data class EditPayload(
        val song: Song,
        val newAlbum: String?,
        val newArtist: String?
    )

    private data class PendingEdit(
        val payloads: List<EditPayload>,
        val tempFiles: List<File>,
        val uris: List<Uri>,
        val intentSenders: List<IntentSender>,
        var currentSenderIndex: Int = 0
    )

    private val pending = ConcurrentHashMap<Long, PendingEdit>()
    private val nextId = AtomicLong(1L)

    suspend fun attemptEdit(
        payloads: List<EditPayload>,
        onTagWritten: suspend (EditPayload) -> Unit
    ): AttemptResult = withContext(Dispatchers.IO) {
        if (payloads.isEmpty()) return@withContext AttemptResult.Done

        val needConfirmPayloads = mutableListOf<EditPayload>()
        val needConfirmTempFiles = mutableListOf<File>()
        val needConfirmUris = mutableListOf<Uri>()
        val needConfirmSenders = mutableListOf<IntentSender>()  // API 29 per-file senders
        var anySuccess = false

        for (payload in payloads) {
            val song = payload.song
            val uri = ContentUris.withAppendedId(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, song.mediaStoreId
            )

            try {
                // Step 1: Copy to temp file (needs only read permission)
                val tempFile = copyToTemp(uri, song.mediaStoreId, song.filePath)

                // Step 2: Edit temp file tags with JAudioTagger (cache dir is writable)
                editTempFileTags(tempFile, song, payload)

                // Step 3: Try writing back to MediaStore.
                // On Android 10+ scoped storage, openOutputStream() on a non-owned file throws
                // FileNotFoundException (IOException subclass), NOT SecurityException. So we catch
                // all exceptions here — if copy+edit already succeeded, a write failure is almost
                // certainly a permission issue, and we route it through createWriteRequest.
                try {
                    writeBackToMediaStore(uri, tempFile)
                    onTagWritten(payload)
                    anySuccess = true
                    context.contentResolver.notifyChange(uri, null)
                    tempFile.delete()
                } catch (e: RecoverableSecurityException) {
                    // API 29: openOutputStream/openFileDescriptor may throw this directly
                    needConfirmPayloads.add(payload)
                    needConfirmTempFiles.add(tempFile)
                    needConfirmUris.add(uri)
                    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        needConfirmSenders.add(e.userAction.actionIntent.intentSender)
                    }
                } catch (e: Exception) {
                    // Write-back failed (FileNotFoundException with EACCES on API 30+, or other).
                    // Since copy+edit succeeded, treat as a permission issue.
                    // Re-throw cancellation first — CancellationException is a RuntimeException
                    // (subclass of Exception), so without this guard it would be misrouted into the
                    // permission-confirm path instead of cancelling the coroutine.
                    if (e is CancellationException) throw e
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        // API 30+: collect URI, will batch via createWriteRequest below
                        needConfirmPayloads.add(payload)
                        needConfirmTempFiles.add(tempFile)
                        needConfirmUris.add(uri)
                    } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                        // API 29: openOutputStream didn't throw RecoverableSecurityException,
                        // so probe with openFileDescriptor to obtain the per-file IntentSender.
                        needConfirmPayloads.add(payload)
                        needConfirmTempFiles.add(tempFile)
                        needConfirmUris.add(uri)
                        val sender = probeWritePermissionSender(uri)
                        if (sender != null) {
                            needConfirmSenders.add(sender)
                        }
                    } else {
                        // API <=28: genuine write failure (WRITE_EXTERNAL_STORAGE should have been
                        // requested by the UI layer); cannot proceed.
                        tempFile.delete()
                    }
                } catch (e: Throwable) {
                    // Catch any Error (e.g. OOM) from onTagWritten/notifyChange so a single
                    // song can't crash the whole batch. Re-throw cancellation to keep coroutine
                    // cancellation semantics intact.
                    if (e is CancellationException) throw e
                    Log.w("MediaStoreEditManager", "Write-back/DB update failed for song ${song.title} (id=${song.mediaStoreId})", e)
                    tempFile.delete()
                }
            } catch (e: Throwable) {
                // Read/copy/edit phase failed (file format unsupported, IO error, OOM, etc.)
                // Catch Throwable (not just Exception) because JAudioTagger can throw
                // OutOfMemoryError when reading large embedded artwork — an Error that
                // would otherwise escape and crash the app.
                if (e is CancellationException) throw e
                Log.w("MediaStoreEditManager", "Failed to edit tags for song ${song.title} (id=${song.mediaStoreId})", e)
            }
        }

        if (needConfirmPayloads.isEmpty()) {
            return@withContext if (anySuccess) AttemptResult.Done else AttemptResult.Error
        }

        // API 29: if we collected URIs but no IntentSenders (probe succeeded = already have
        // permission), retry the write-back directly instead of requesting user confirmation.
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && needConfirmSenders.isEmpty()) {
            // Already have write permission (probe succeeded); retry write-back now.
            var retrySuccess = false
            for (i in needConfirmPayloads.indices) {
                try {
                    writeBackToMediaStore(needConfirmUris[i], needConfirmTempFiles[i])
                    onTagWritten(needConfirmPayloads[i])
                    context.contentResolver.notifyChange(needConfirmUris[i], null)
                    needConfirmTempFiles[i].delete()
                    retrySuccess = true
                } catch (e: Throwable) {
                    if (e is CancellationException) throw e
                    needConfirmTempFiles[i].delete()
                }
            }
            return@withContext if (retrySuccess) AttemptResult.Done else AttemptResult.Error
        }

        val requestId = nextId.getAndIncrement()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // API 30+: single batch createWriteRequest
                val intentSender = if (needConfirmUris.isNotEmpty()) {
                    MediaStore.createWriteRequest(context.contentResolver, needConfirmUris).intentSender
                } else null
                pending[requestId] = PendingEdit(
                    payloads = needConfirmPayloads,
                    tempFiles = needConfirmTempFiles,
                    uris = needConfirmUris,
                    intentSenders = listOfNotNull(intentSender),
                    currentSenderIndex = 0
                )
            } else if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
                // API 29: per-file IntentSenders from RecoverableSecurityException
                pending[requestId] = PendingEdit(
                    payloads = needConfirmPayloads,
                    tempFiles = needConfirmTempFiles,
                    uris = needConfirmUris,
                    intentSenders = needConfirmSenders,
                    currentSenderIndex = 0
                )
            }

            return@withContext AttemptResult.NeedsConfirm(requestId)
        } catch (e: Throwable) {
            if (e is CancellationException) throw e
            // createWriteRequest or pending setup failed — clean up temp files and bail.
            Log.e("MediaStoreEditManager", "Failed to request write permission", e)
            needConfirmTempFiles.forEach { it.delete() }
            return@withContext if (anySuccess) AttemptResult.Done else AttemptResult.Error
        }
    }

    /**
     * After user confirms write permission, retry writing temp files back to MediaStore.
     */
    suspend fun retryWriteTags(requestId: Long, onTagWritten: suspend (EditPayload) -> Unit): Boolean = withContext(Dispatchers.IO) {
        val pd = pending[requestId] ?: return@withContext false
        var allSuccess = true
        for (i in pd.payloads.indices) {
            val payload = pd.payloads[i]
            val tempFile = pd.tempFiles[i]
            val uri = pd.uris[i]
            try {
                writeBackToMediaStore(uri, tempFile)
                onTagWritten(payload)
                context.contentResolver.notifyChange(uri, null)
                tempFile.delete()
            } catch (e: Throwable) {
                if (e is CancellationException) throw e
                allSuccess = false
                tempFile.delete()
            }
        }
        allSuccess
    }

    fun intentSenderFor(requestId: Long): IntentSender? {
        val pd = pending[requestId] ?: return null
        return pd.intentSenders.getOrNull(pd.currentSenderIndex)
    }

    fun onConfirmed(requestId: Long): List<EditPayload>? {
        val pd = pending[requestId] ?: return null
        pd.currentSenderIndex++
        if (pd.currentSenderIndex >= pd.intentSenders.size) {
            return pd.payloads
        }
        return emptyList()
    }

    fun hasMoreSenders(requestId: Long): Boolean {
        val pd = pending[requestId] ?: return false
        return pd.currentSenderIndex < pd.intentSenders.size
    }

    fun cancel(requestId: Long) {
        val pd = pending.remove(requestId)
        pd?.tempFiles?.forEach { it.delete() }
    }

    fun clear(requestId: Long) {
        val pd = pending.remove(requestId)
        pd?.tempFiles?.forEach { it.delete() }
    }

    // ─── Internal helpers ───────────────────────────────────────

    /** Copy audio file from MediaStore to a cache temp file. */
    private fun copyToTemp(uri: Uri, mediaStoreId: Long, filePath: String? = null): File {
        // JAudioTagger's AudioFileIO.read() selects the format reader by file extension.
        // A temp file without an extension causes CannotReadException, so we must preserve
        // the original extension (derived from filePath, falling back to a MediaStore query).
        val ext = filePath?.substringAfterLast('.', "")?.takeIf { it.isNotEmpty() }
            ?: queryDisplayName(uri)?.substringAfterLast('.', "")
        val suffix = ext?.takeIf { it.isNotEmpty() }?.let { ".$it" } ?: ""
        val tempFile = File(context.cacheDir, "edit_${mediaStoreId}_${System.nanoTime()}${suffix}")
        try {
            context.contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            } ?: throw java.io.IOException("Cannot open input stream for $uri")
            return tempFile
        } catch (e: Exception) {
            tempFile.delete()
            throw e
        }
    }

    /** Query MediaStore for the file's display name, used to derive the extension when filePath is null. */
    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(uri, arrayOf(MediaStore.MediaColumns.DISPLAY_NAME), null, null, null)?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** Apply tag fields to the temp file using JAudioTagger. Cache dir is writable, commit won't fail. */
    private fun editTempFileTags(tempFile: File, song: Song, payload: EditPayload) {
        val audioFile = AudioFileIO.read(tempFile)
        val tag: Tag = audioFile.tag ?: audioFile.createDefaultTag()

        if (payload.newAlbum != null || payload.newArtist != null) {
            // Album edit: only update album/artist fields
            payload.newAlbum?.let { tag.setField(FieldKey.ALBUM, it) }
            payload.newArtist?.let { tag.setField(FieldKey.ARTIST, it) }
        } else {
            // Single song full edit: write all fields from the edited song
            tag.setField(FieldKey.TITLE, song.title)
            tag.setField(FieldKey.ARTIST, song.artist)
            tag.setField(FieldKey.ALBUM, song.album)
            song.year?.toString()?.let { tag.setField(FieldKey.YEAR, it) }
            song.trackNumber?.toString()?.let { tag.setField(FieldKey.TRACK, it) }
            song.discNumber?.toString()?.let { tag.setField(FieldKey.DISC_NO, it) }
            song.genre?.let { tag.setField(FieldKey.GENRE, it) }
        }

        audioFile.commit()
    }

    /** Write the edited temp file back to MediaStore. May throw on API 10+ for non-owned files. */
    private fun writeBackToMediaStore(uri: Uri, tempFile: File) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { output ->
            tempFile.inputStream().use { input -> input.copyTo(output) }
        } ?: throw java.io.IOException("Cannot open output stream for $uri")
    }

    /**
     * On API 29, probe write permission by opening a file descriptor for writing.
     * If the file is not owned by the app, this throws RecoverableSecurityException
     * whose userAction.pendingIntent.intentSender can be used to request user consent.
     * Returns the IntentSender, or null if the probe succeeded (already have permission)
     * or failed with a non-recoverable exception.
     */
    private fun probeWritePermissionSender(uri: Uri): IntentSender? {
        return try {
            context.contentResolver.openFileDescriptor(uri, "wt")?.use { /* fd opened ok, we have permission */ }
            null
        } catch (e: RecoverableSecurityException) {
            e.userAction.actionIntent.intentSender
        } catch (e: Exception) {
            null
        }
    }
}
