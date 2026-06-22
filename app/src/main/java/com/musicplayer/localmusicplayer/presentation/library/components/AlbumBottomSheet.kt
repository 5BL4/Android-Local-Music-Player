package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Album

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumBottomSheet(
    album: Album,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            Text(album.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp))
            Text(album.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 24.dp, vertical = 0.dp))
            Divider(modifier = Modifier.padding(top = 8.dp))
            ListItem(
                headlineContent = { Text(stringResource(R.string.edit_info_menu)) },
                modifier = Modifier.clickable { onEdit(); onDismiss() }
            )
            ListItem(
                headlineContent = { Text(stringResource(R.string.delete_menu), color = MaterialTheme.colorScheme.error) },
                modifier = Modifier.clickable { onDelete(); onDismiss() }
            )
            Spacer(Modifier.height(16.dp))
        }
    }
}
