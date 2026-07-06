package com.musicplayer.localmusicplayer.presentation.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R
import com.musicplayer.localmusicplayer.util.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsScreen(
    songId: Long,
    onBack: () -> Unit,
    viewModel: LyricsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lyrics)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.setShowSearchPanel(!uiState.showSearchPanel) }) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search_lyrics))
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.showSearchPanel) {
            // Search Panel
            Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
                var sourceExpanded by remember { mutableStateOf(false) }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { viewModel.onSearchQueryChanged(it) },
                        label = { Text(stringResource(R.string.search_lyrics)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.search() }, enabled = !uiState.isSearching) {
                        Text(stringResource(R.string.search))
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Source selector
                ExposedDropdownMenuBox(expanded = sourceExpanded, onExpandedChange = { sourceExpanded = !sourceExpanded }) {
                    OutlinedTextField(
                        value = SOURCES.find { it.first == uiState.searchSource }?.second ?: uiState.searchSource,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.source)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sourceExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = sourceExpanded, onDismissRequest = { sourceExpanded = false }) {
                        SOURCES.forEach { (key, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = { viewModel.onSourceChanged(key); sourceExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                when {
                    uiState.isSearching -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                    uiState.searchError != null -> {
                        Text(uiState.searchError!!, color = MaterialTheme.colorScheme.error)
                    }
                    uiState.searchResults.isNotEmpty() -> {
                        Text(stringResource(R.string.search_results_count, uiState.searchResults.size), style = MaterialTheme.typography.bodySmall)
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            itemsIndexed(uiState.searchResults) { _, item ->
                                ListItem(
                                    headlineContent = { Text(item.name, maxLines = 1) },
                                    supportingContent = { Text("${item.getArtist()} - ${item.album}") },
                                    modifier = Modifier.clickable { viewModel.onResultClicked(item) }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Lyrics display
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize().padding(padding)) { CircularProgressIndicator(Modifier.wrapContentSize()) }
                }
                uiState.error != null && uiState.lyrics.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(uiState.error!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(8.dp))
                            TextButton(onClick = { viewModel.setShowSearchPanel(true) }) {
                                Text(stringResource(R.string.search_online_hint))
                            }
                        }
                    }
                }
                else -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
                        val listState = rememberLazyListState()
                        val currentIndex = uiState.currentLineIndex
                        val isScrubbing = uiState.isScrubbing
                        val scrubLineIndex = uiState.scrubLineIndex

                        // Observe user drag-scrolls (NestedScrollSource.Drag) to enter
                        // scrub mode. This fires ONLY for touch drags — programmatic
                        // animateScrollToItem does NOT trigger it, so no race with the
                        // auto-scroll LaunchedEffect below.
                        val nestedScrollConnection = remember {
                            object : NestedScrollConnection {
                                override fun onPreScroll(
                                    available: Offset,
                                    source: NestedScrollSource
                                ): Offset {
                                    if (source == NestedScrollSource.UserInput) {
                                        viewModel.enterScrubMode(listState.firstVisibleItemIndex)
                                    }
                                    return Offset.Zero
                                }
                            }
                        }

                        // Suppress auto-scroll while the user is scrubbing so the
                        // pill stays anchored to the line they targeted.
                        LaunchedEffect(currentIndex, isScrubbing) {
                            if (currentIndex > 0 && !isScrubbing) {
                                listState.animateScrollToItem(currentIndex)
                            }
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(nestedScrollConnection),
                            // Half-viewport top/bottom padding centers the scrolled-to line
                            // vertically instead of pinning it to the top.
                            contentPadding = PaddingValues(vertical = maxHeight / 2),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            itemsIndexed(uiState.lyrics) { index, line ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 12.dp)
                                ) {
                                    Text(
                                        text = line.text,
                                        style = if (index == currentIndex) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                        color = if (index == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 32.dp)
                                    )
                                    if (isScrubbing && scrubLineIndex == index) {
                                        SeekPill(
                                            timestampMs = line.timestampMs,
                                            onClick = { viewModel.seekTo(line.timestampMs) },
                                            modifier = Modifier
                                                .align(Alignment.CenterEnd)
                                                .padding(end = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Preview Bottom Sheet
    if (uiState.showPreview && uiState.previewLyric != null) {
        ModalBottomSheet(onDismissRequest = { viewModel.hidePreview() }) {
            Column(modifier = Modifier.padding(16.dp).padding(bottom = 32.dp)) {
                Text(stringResource(R.string.lyrics_preview), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                val preview = uiState.previewLyric!!
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    item {
                        Text(
                            text = preview.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}]"), ""),
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    if (uiState.previewTlyric != null) {
                        item {
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(stringResource(R.string.translated_lyrics), style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = uiState.previewTlyric!!.replace(Regex("\\[\\d{2}:\\d{2}\\.\\d{2,3}]"), ""),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = { viewModel.embedLyrics() }, enabled = !uiState.isEmbedding) {
                        Text(stringResource(R.string.embed_local))
                    }
                    OutlinedButton(onClick = { viewModel.hidePreview() }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
                if (uiState.embedMessage != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        uiState.embedMessage!!,
                        color = if (uiState.embedSuccess) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Compact seek-pill that appears to the right of a lyric line the user is
 * scrubbing. Tapping it seeks playback to that line's timestamp.
 *
 * Uses an opaque `surfaceVariant` background (R6) so long lyric text behind
 * the pill never bleeds through. The full-pill shape is a 50% rounded rect
 * (capsule).
 */
@Composable
private fun SeekPill(
    timestampMs: Long,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceVariant,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        tonalElevation = 4.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = stringResource(R.string.seek_to, formatDuration(timestampMs)),
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = formatDuration(timestampMs),
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
