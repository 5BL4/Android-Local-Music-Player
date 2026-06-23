package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.musicplayer.localmusicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

@Composable
fun PagingSongList(
    pagingFlow: Flow<PagingData<Song>>,
    onSongClick: (Song) -> Unit,
    onSongMenuClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp
) {
    @Suppress("DEPRECATION")
    val lazyItems: LazyPagingItems<Song> = pagingFlow.collectAsLazyPagingItems()

    when (val state = lazyItems.loadState.refresh) {
        is LoadState.Loading -> {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }
        else -> {}
    }

    LazyColumn(modifier = modifier.fillMaxSize()) {
        items(count = lazyItems.itemCount) { index ->
            val song = lazyItems[index]
            if (song != null) {
                SongItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    onMenuClick = onSongMenuClick?.let { { it(song) } },
                    horizontalPadding = horizontalPadding
                )
            }
        }

        when (val state = lazyItems.loadState.append) {
            is LoadState.Loading -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            }
            is LoadState.Error -> {
                item {
                    Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                        Text(state.error.message ?: "Load failed", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            else -> {}
        }
    }
}
