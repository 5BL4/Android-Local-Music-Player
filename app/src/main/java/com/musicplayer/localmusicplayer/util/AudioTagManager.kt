package com.musicplayer.localmusicplayer.util

import android.content.Context
import android.util.Log
import com.musicplayer.localmusicplayer.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AudioTagManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Save lyrics as .lrc file next to the audio file.
     * If filePath is null/empty, saves to cache directory.
     */
    fun embedLyrics(song: Song, lrcContent: String): Boolean {
        return try {
            val audioFile = song.filePath?.let { File(it) }
            val lrcFile = if (audioFile != null && audioFile.exists()) {
                val base = audioFile.absolutePath.removeSuffix(audioFile.extension)
                File("${base}lrc")
            } else {
                val safeName = song.title.replace(Regex("[/\\\\:*?\"<>|]"), "_")
                File(context.cacheDir, "${safeName}.lrc")
            }
            lrcFile.writeText(lrcContent)
            Log.d("AudioTagManager", "LRC saved to ${lrcFile.absolutePath}")
            true
        } catch (e: Exception) {
            Log.e("AudioTagManager", "Failed to save LRC", e)
            false
        }
    }
}
