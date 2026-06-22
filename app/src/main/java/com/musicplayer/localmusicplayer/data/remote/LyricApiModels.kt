package com.musicplayer.localmusicplayer.data.remote

data class SearchResult(
    val id: String = "",
    val name: String = "",
    val artist: Any? = "",
    val album: String = "",
    val duration: Long = 0,
    val source: String = ""
) {
    fun getArtist(): String = when (artist) {
        is String -> artist as String
        is List<*> -> (artist as List<*>).joinToString(", ") { it.toString() }
        else -> ""
    }
}

data class LyricResponse(
    val code: Int = 0,
    val lyric: String? = null,
    val tlyric: String? = null
)
