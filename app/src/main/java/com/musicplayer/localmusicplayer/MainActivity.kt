package com.musicplayer.localmusicplayer

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.PlaybackState
import com.musicplayer.localmusicplayer.presentation.navigation.NavGraph
import com.musicplayer.localmusicplayer.presentation.navigation.Screen
import com.musicplayer.localmusicplayer.presentation.player.PlayerViewModel
import com.musicplayer.localmusicplayer.presentation.player.components.NowPlayingBar
import com.musicplayer.localmusicplayer.presentation.theme.MusicPlayerTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun attachBaseContext(newBase: Context) {
        val language = MusicPlayerApplication.savedLanguage
        super.attachBaseContext(
            com.musicplayer.localmusicplayer.util.LocaleHelper.applyLocale(newBase, language)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MusicPlayerTheme {
                MainContent()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainContent() {
    val context = LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_AUDIO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
        if (!granted) {
            Toast.makeText(context, context.getString(R.string.permission_denied), Toast.LENGTH_LONG).show()
        }
    }

    val playerViewModel: PlayerViewModel = viewModel()

    val bottomNavScreens = setOf(Screen.Library.route, Screen.Equalizer.route, Screen.Settings.route)
    val showBottomNav = currentRoute in bottomNavScreens

    val playerScreens = setOf(Screen.Player.route, Screen.Lyrics.ROUTE)
    val showNowPlayingBar = currentRoute !in playerScreens

    val playbackState by playerViewModel.uiState.collectAsState()
    val nowPlayingSong = when (val state = playbackState.playbackState) {
        is PlaybackState.Playing -> state.currentSong
        is PlaybackState.Paused -> state.currentSong
        else -> null
    }
    val isPlaying = playbackState.playbackState is PlaybackState.Playing

    if (!hasPermission) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.permission_title), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.permission_message), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { permissionLauncher.launch(permission) }) {
                    Text(stringResource(R.string.grant_permission))
                }
            }
        }
        return
    }

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            Surface(modifier = Modifier.weight(1f)) {
                NavGraph(navController = navController)
            }

            if (showNowPlayingBar && nowPlayingSong != null) {
                NowPlayingBar(
                    isPlaying = isPlaying,
                    title = nowPlayingSong.title,
                    artist = nowPlayingSong.artist,
                    albumArtUri = nowPlayingSong.albumArtUri,
                    onPlayPauseClick = { playerViewModel.togglePlayPause() },
                    onBarClick = { navController.navigate(Screen.Player.route) }
                )
            }

            if (showBottomNav) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.LibraryMusic, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_library)) },
                        selected = currentRoute == Screen.Library.route,
                        onClick = {
                            if (currentRoute != Screen.Library.route) {
                                navController.navigate(Screen.Library.route) {
                                    popUpTo(Screen.Library.route) { inclusive = true }
                                }
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Equalizer, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_equalizer)) },
                        selected = currentRoute == Screen.Equalizer.route,
                        onClick = {
                            if (currentRoute != Screen.Equalizer.route) {
                                navController.navigate(Screen.Equalizer.route)
                            }
                        }
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                        label = { Text(stringResource(R.string.tab_settings)) },
                        selected = currentRoute == Screen.Settings.route,
                        onClick = {
                            if (currentRoute != Screen.Settings.route) {
                                navController.navigate(Screen.Settings.route)
                            }
                        }
                    )
                }
            }
        }
    }
}
