package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Song

@Composable
fun EditSongDialog(
    song: Song,
    onSave: (edited: Song) -> Unit,
    onDismiss: () -> Unit,
    onPickAlbumArt: () -> Unit,
    pickedAlbumArtUri: String?
) {
    var title by remember { mutableStateOf(song.title) }
    var artist by remember { mutableStateOf(song.artist) }
    var album by remember { mutableStateOf(song.album) }
    var yearText by remember { mutableStateOf(song.year?.toString() ?: "") }
    var trackText by remember { mutableStateOf(song.trackNumber?.toString() ?: "") }
    var discText by remember { mutableStateOf(song.discNumber?.toString() ?: "") }
    var genreText by remember { mutableStateOf(song.genre ?: "") }
    val displayArtUri = pickedAlbumArtUri ?: song.albumArtUri

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_song_info)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Album art preview
                Surface(
                    modifier = Modifier.size(120.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (displayArtUri != null) {
                        AsyncImage(
                            model = displayArtUri,
                            contentDescription = stringResource(R.string.song_album_art),
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.padding(24.dp).fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                TextButton(onClick = onPickAlbumArt) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.choose_album_art))
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.song_title_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = artist,
                    onValueChange = { artist = it },
                    label = { Text(stringResource(R.string.song_artist_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = album,
                    onValueChange = { album = it },
                    label = { Text(stringResource(R.string.song_album_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter { c -> c.isDigit() }.take(4) },
                        label = { Text(stringResource(R.string.song_year_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = trackText,
                        onValueChange = { trackText = it.filter { c -> c.isDigit() }.take(5) },
                        label = { Text(stringResource(R.string.song_track_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = discText,
                        onValueChange = { discText = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text(stringResource(R.string.song_disc_label)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = genreText,
                    onValueChange = { genreText = it },
                    label = { Text(stringResource(R.string.song_genre_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val edited = song.copy(
                    title = title.ifBlank { song.title },
                    artist = artist.ifBlank { song.artist },
                    album = album.ifBlank { song.album },
                    year = yearText.toIntOrNull(),
                    trackNumber = trackText.toIntOrNull(),
                    discNumber = discText.toIntOrNull(),
                    genre = genreText.ifBlank { null },
                    albumArtUri = pickedAlbumArtUri ?: song.albumArtUri
                )
                onSave(edited)
            }) {
                Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}
