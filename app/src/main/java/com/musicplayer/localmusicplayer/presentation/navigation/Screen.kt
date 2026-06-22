package com.musicplayer.localmusicplayer.presentation.navigation

import java.net.URLDecoder
import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Library : Screen("library")
    data object Player : Screen("player")
    data object Playlists : Screen("playlists")
    data object Settings : Screen("settings")
    data object Equalizer : Screen("equalizer")

    data class AlbumDetail(val albumId: Long) : Screen("album/$albumId") {
        companion object {
            const val ROUTE = "album/{albumId}"
        }
    }

    data class ArtistDetail(val artistName: String) : Screen("artist/${URLEncoder.encode(artistName, "UTF-8")}") {
        companion object {
            const val ROUTE = "artist/{artistName}"
            fun decodeName(encoded: String): String = URLDecoder.decode(encoded, "UTF-8")
        }
    }

    data class PlaylistDetail(val playlistId: Long) : Screen("playlist/$playlistId") {
        companion object {
            const val ROUTE = "playlist/{playlistId}"
        }
    }

    data class Lyrics(val songId: Long) : Screen("lyrics/$songId") {
        companion object {
            const val ROUTE = "lyrics/{songId}"
        }
    }
}
