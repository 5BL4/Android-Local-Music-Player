package com.musicplayer.localmusicplayer.util

data class LrcLine(
    val timeMs: Long,
    val text: String
)

object LrcParser {
    private val regex = Regex("""\[(\d{2}):(\d{2})\.(\d{2,3})](.*)""")

    fun parse(lrc: String): List<LrcLine> {
        return lrc.lines().mapNotNull { line ->
            regex.find(line)?.let { match ->
                val min = match.groupValues[1].toInt()
                val sec = match.groupValues[2].toInt()
                val ms = match.groupValues[3].toInt().let {
                    if (it < 100) it * 10 else it // normalize 2-digit to ms
                }
                val text = match.groupValues[4].trim()
                if (text.isNotEmpty()) LrcLine(min * 60000L + sec * 1000L + ms, text)
                else null
            }
        }.sortedBy { it.timeMs }
    }

    fun parseSyncedPair(lyric: String, tlyric: String?): List<LrcLine> {
        val orig = parse(lyric)
        if (tlyric == null) return orig
        val trans = parse(tlyric).associateBy { it.timeMs }
        return orig.map { line ->
            trans[line.timeMs]?.let { line.copy(text = line.text + "\n" + it.text) } ?: line
        }
    }
}
