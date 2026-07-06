package com.musicplayer.localmusicplayer.presentation.library.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.paging.LoadState
import androidx.paging.PagingData
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.domain.model.Song
import kotlinx.coroutines.flow.Flow

@Composable
fun PagingSongList(
    pagingFlow: Flow<PagingData<Song>>,
    onSongClick: (Song) -> Unit,
    onSongMenuClick: ((Song) -> Unit)? = null,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 16.dp,
    listState: LazyListState = rememberLazyListState(),
    currentSongId: Long? = null
) {
    @Suppress("DEPRECATION")
    val lazyItems: LazyPagingItems<Song> = pagingFlow.collectAsLazyPagingItems()

    // Only show the full-screen spinner on the initial load when there are no
    // cached items yet. Once items exist (e.g. returning from the Player screen
    // with cached PagingData), keep the LazyColumn composed so the saved scroll
    // position is restored instead of being clamped to 0 by an empty list.
    if (lazyItems.itemCount == 0 && lazyItems.loadState.refresh is LoadState.Loading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    if (lazyItems.itemCount == 0 && lazyItems.loadState.refresh is LoadState.NotLoading) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = stringResource(R.string.no_music_found),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize()
    ) {
        items(
            count = lazyItems.itemCount,
            key = { index -> lazyItems[index]?.id ?: index }
        ) { index ->
            val song = lazyItems[index]
            if (song != null) {
                SongItem(
                    song = song,
                    onClick = { onSongClick(song) },
                    onMenuClick = onSongMenuClick?.let { { it(song) } },
                    horizontalPadding = horizontalPadding,
                    currentSongId = currentSongId
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
                        Text(state.error.message ?: stringResource(R.string.load_failed), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
            else -> {}
        }
    }
}
