package com.musicplayer.localmusicplayer.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.musicplayer.localmusicplayer.presentation.albumdetail.AlbumDetailScreen
import com.musicplayer.localmusicplayer.presentation.artistdetail.ArtistDetailScreen
import com.musicplayer.localmusicplayer.presentation.equalizer.EqualizerScreen
import com.musicplayer.localmusicplayer.presentation.library.LibraryScreen
import com.musicplayer.localmusicplayer.presentation.lyrics.LyricsScreen
import com.musicplayer.localmusicplayer.presentation.player.PlayerScreen
import com.musicplayer.localmusicplayer.presentation.playlist.PlaylistScreen
import com.musicplayer.localmusicplayer.presentation.playlistdetail.PlaylistDetailScreen
import com.musicplayer.localmusicplayer.presentation.settings.SettingsScreen

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Library.route
    ) {
        composable(Screen.Library.route) {
            LibraryScreen(
                onAlbumClick = { albumId ->
                    navController.navigate(Screen.AlbumDetail(albumId).route)
                },
                onArtistClick = { artistName ->
                    navController.navigate(Screen.ArtistDetail(artistName).route)
                },
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail(playlistId).route)
                },
                onPlayerOpen = {
                    navController.navigate(Screen.Player.route)
                }
            )
        }

        composable(Screen.Player.route) {
            PlayerScreen(
                onLyricsClick = { songId ->
                    navController.navigate(Screen.Lyrics(songId).route)
                },
                onEqualizerClick = {
                    navController.navigate(Screen.Equalizer.route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.AlbumDetail.ROUTE,
            arguments = listOf(navArgument("albumId") { type = NavType.LongType })
        ) { backStackEntry ->
            val albumId = backStackEntry.arguments?.getLong("albumId")
            if (albumId != null) {
                AlbumDetailScreen(
                    albumId = albumId,
                    onSongClick = { navController.navigate(Screen.Player.route) },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(
            route = Screen.ArtistDetail.ROUTE,
            arguments = listOf(navArgument("artistName") { type = NavType.StringType })
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("artistName")?.let {
                Screen.ArtistDetail.decodeName(it)
            }
            if (name != null) {
                ArtistDetailScreen(
                    artistName = name,
                    onAlbumClick = { albumId ->
                        navController.navigate(Screen.AlbumDetail(albumId).route)
                    },
                    onBack = { navController.popBackStack() }
                )
            }
        }

        composable(Screen.Playlists.route) {
            PlaylistScreen(
                onPlaylistClick = { playlistId ->
                    navController.navigate(Screen.PlaylistDetail(playlistId).route)
                },
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.PlaylistDetail.ROUTE,
            arguments = listOf(navArgument("playlistId") { type = NavType.LongType })
        ) { backStackEntry ->
            val pid = backStackEntry.arguments?.getLong("playlistId")
            if (pid != null) {
                PlaylistDetailScreen(playlistId = pid, onBack = { navController.popBackStack() })
            }
        }

        composable(
            route = Screen.Lyrics.ROUTE,
            arguments = listOf(navArgument("songId") { type = NavType.LongType })
        ) { backStackEntry ->
            val sid = backStackEntry.arguments?.getLong("songId")
            if (sid != null) {
                LyricsScreen(songId = sid, onBack = { navController.popBackStack() })
            }
        }

        composable(Screen.Equalizer.route) {
            EqualizerScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
