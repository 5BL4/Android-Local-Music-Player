package com.musicplayer.localmusicplayer.data.local.datasource

import android.content.Context
import com.musicplayer.localmusicplayer.domain.model.LyricLine
import com.musicplayer.localmusicplayer.domain.model.Song
import com.musicplayer.localmusicplayer.domain.repository.LyricsLocalDataSource
import com.musicplayer.localmusicplayer.util.LyricParser
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LyricsFileDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) : LyricsLocalDataSource {

    override fun findAndParse(song: Song): Result<List<LyricLine>> {
        return try {
            // Strategy 1: Look for .lrc file in same directory as audio file
            val audioFile = File(song.filePath)
            val lrcFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".lrc")
            if (lrcFile.exists()) {
                val content = lrcFile.readText(Charsets.UTF_8)
                    .let { if (it.contains("[")) it else lrcFile.readText(Charset.forName("GBK")) }
                return Result.success(LyricParser.parse(content))
            }

            // Strategy 2: Try alternative .txt extension
            val txtFile = File(audioFile.parent, audioFile.nameWithoutExtension + ".txt")
            if (txtFile.exists()) {
                val content = txtFile.readText(Charsets.UTF_8)
                return Result.success(LyricParser.parse(content))
            }

            Result.failure(Exception("No lyrics file found"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
