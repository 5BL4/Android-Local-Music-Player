package com.musicplayer.localmusicplayer.presentation.lyrics

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.musicplayer.localmusicplayer.R

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
                actions = {
                    IconButton(onClick = { viewModel.setShowSearchPanel(!uiState.showSearchPanel) }) {
                        Icon(Icons.Default.Search, contentDescription = "Search Lyrics")
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
                        label = { Text("搜索歌词") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = { viewModel.search() }, enabled = !uiState.isSearching) {
                        Text("搜索")
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Source selector
                ExposedDropdownMenuBox(expanded = sourceExpanded, onExpandedChange = { sourceExpanded = !sourceExpanded }) {
                    OutlinedTextField(
                        value = SOURCES.find { it.first == uiState.searchSource }?.second ?: uiState.searchSource,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("音源") },
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
                        Text("共 ${uiState.searchResults.size} 条结果", style = MaterialTheme.typography.bodySmall)
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
                                Text("搜索在线歌词")
                            }
                        }
                    }
                }
                else -> {
                    val listState = rememberLazyListState()
                    val currentIndex = uiState.currentLineIndex
                    LaunchedEffect(currentIndex) {
                        if (currentIndex > 0) listState.animateScrollToItem(currentIndex)
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentPadding = PaddingValues(vertical = 64.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        itemsIndexed(uiState.lyrics) { index, line ->
                            Text(
                                text = line.text,
                                style = if (index == currentIndex) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                                color = if (index == currentIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 12.dp)
                            )
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
                Text("歌词预览", style = MaterialTheme.typography.titleMedium)
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
                            Text("翻译歌词", style = MaterialTheme.typography.titleSmall)
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
                        Text("嵌入到本地")
                    }
                    OutlinedButton(onClick = { viewModel.hidePreview() }) {
                        Text("取消")
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
