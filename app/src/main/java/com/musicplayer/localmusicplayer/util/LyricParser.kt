package com.musicplayer.localmusicplayer.util

import com.musicplayer.localmusicplayer.domain.model.LyricLine

object LyricParser {
    private val LRC_LINE_REGEX = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    fun parse(lrcContent: String): List<LyricLine> {
        return lrcContent.lines()
            .mapNotNull { line ->
                LRC_LINE_REGEX.matchEntire(line.trim())?.let { match ->
                    val minutes = match.groupValues[1].toInt()
                    val seconds = match.groupValues[2].toInt()
                    val millis = match.groupValues[3]
                        .padEnd(3, '0')
                        .take(3)
                        .toInt()
                    val timestampMs = (minutes * 60 + seconds) * 1000L + millis
                    val text = match.groupValues[4].trim()
                    if (text.isNotEmpty()) LyricLine(timestampMs, text) else null
                }
            }
            .sortedBy { it.timestampMs }
    }
}
