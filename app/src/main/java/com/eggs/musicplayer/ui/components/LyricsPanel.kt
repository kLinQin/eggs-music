package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.lyrics.LyricsMode
import com.eggs.musicplayer.lyrics.LyricsProvider
import com.eggs.musicplayer.lyrics.LyricsUiState
import com.eggs.musicplayer.lyrics.TranslateProvider
import com.eggs.musicplayer.ui.theme.*

@Composable
fun LyricsPanel(
    state: LyricsUiState,
    currentTrackTitle: String?,
    onSelectMode: (LyricsMode) -> Unit,
    onSetProvider: (LyricsProvider) -> Unit,
    onSetTranslateProvider: (TranslateProvider) -> Unit,
    onToggleProviderPopup: () -> Unit,
    onToggleTranslatePopup: () -> Unit,
    onOpenManual: () -> Unit,
    onSearch: (String) -> Unit,
    onApplySearch: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    var searchOpen by remember { mutableStateOf(false) }
    var searchText by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().background(BgElev)) {
        // Mode switcher
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            LyricsMode.entries.forEach { m ->
                val active = state.mode == m
                val label = when (m) {
                    LyricsMode.Plain -> "Plain"
                    LyricsMode.Indo -> "Indo"
                    LyricsMode.Inggris -> "Inggris"
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (active) Accent else BgPanel)
                        .clickable { onSelectMode(m) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(label, color = if (active) Color.Black else TextMute, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = { searchOpen = !searchOpen }, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Default.Search, "Cari lirik", tint = TextMute, modifier = Modifier.size(18.dp))
            }
            if (state.provider == LyricsProvider.Manual) {
                IconButton(onClick = onOpenManual, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Edit, "Edit lirik manual", tint = TextMute, modifier = Modifier.size(18.dp))
                }
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Border)

        if (searchOpen) {
            Row(
                Modifier.fillMaxWidth().padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    Modifier.weight(1f).height(36.dp)
                        .background(BgPanel, RoundedCornerShape(4.dp))
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    BasicTextField(
                        value = searchText,
                        onValueChange = { searchText = it; onSearchQueryChange(it) },
                        singleLine = true,
                        textStyle = TextStyle(color = TextMain, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        decorationBox = { inner ->
                            if (searchText.isEmpty()) Text("Cari judul/artist…", color = TextDim, fontSize = 13.sp)
                            inner()
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                TextButton(onClick = { onSearch(searchText) }) { Text("Cari", color = Accent, fontSize = 12.sp) }
                TextButton(onClick = { searchOpen = false }) { Text("X", color = TextMute, fontSize = 12.sp) }
            }
            if (state.searching) {
                LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 220.dp)) {
                itemsIndexed(state.searchResults) { _, item ->
                    Column(
                        Modifier.fillMaxWidth()
                            .clickable { onApplySearch(item.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(item.trackName, color = TextMain, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("${item.artistName}${if (item.albumName.isNotBlank()) " • ${item.albumName}" else ""}", color = TextMute, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            when {
                                item.hasSynced -> "SYNC"
                                item.hasPlain -> "PLAIN"
                                else -> "—"
                            },
                            color = Accent, fontSize = 9.sp
                        )
                    }
                }
            }
            HorizontalDivider(thickness = 1.dp, color = Border)
        }

        // Body lirik
        val listState = rememberLazyListState()
        LaunchedEffect(state.activeLineIndex) {
            if (state.activeLineIndex >= 0) {
                runCatching { listState.animateScrollToItem(state.activeLineIndex) }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.trackId == null -> CenterText("Pilih lagu untuk melihat lirik")
                state.loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    CircularProgressIndicator(color = Accent)
                    Spacer(Modifier.height(8.dp))
                    Text("Memuat lirik…", color = TextMute, fontSize = 12.sp)
                }
                state.provider == null -> CenterText("Pilih sumber lirik di bawah")
                state.result == null -> CenterText(state.error ?: "Lirik tidak tersedia")
                else -> {
                    val res = state.result
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                        itemsIndexed(res.lines) { idx, line ->
                            val active = res.synced && idx == state.activeLineIndex
                            Column(Modifier.padding(vertical = 6.dp)) {
                                Text(
                                    line.text,
                                    color = if (active) TextMain else TextMute,
                                    fontSize = if (active) 16.sp else 14.sp,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal
                                )
                                val tr = state.translations.getOrNull(idx).orEmpty()
                                if (state.mode != LyricsMode.Plain && tr.isNotBlank()) {
                                    Text(tr, color = Accent, fontSize = 12.sp)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                    if (state.translating) {
                        LinearProgressIndicator(
                            progress = { state.translateProgress / 100f },
                            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                            color = Accent
                        )
                    }
                }
            }
        }

        // Bottom bar
        HorizontalDivider(thickness = 1.dp, color = Border)
        Row(
            Modifier.fillMaxWidth().background(BgPanel).padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                AssistChip(
                    onClick = onToggleProviderPopup,
                    label = {
                        Text(
                            when (state.provider) {
                                LyricsProvider.Lrclib -> "LRCLIB"
                                LyricsProvider.LyricsOvh -> "lyrics.ovh"
                                LyricsProvider.Manual -> "Manual"
                                null -> "Pilih sumber"
                            },
                            fontSize = 11.sp
                        )
                    },
                    leadingIcon = { Icon(Icons.Default.MoreVert, null, tint = TextMute, modifier = Modifier.size(14.dp)) }
                )
                DropdownMenu(expanded = state.showProviderPopup, onDismissRequest = onToggleProviderPopup) {
                    DropdownMenuItem(text = { Text("LRCLIB", fontSize = 12.sp) }, onClick = { onSetProvider(LyricsProvider.Lrclib) })
                    DropdownMenuItem(text = { Text("lyrics.ovh", fontSize = 12.sp) }, onClick = { onSetProvider(LyricsProvider.LyricsOvh) })
                    DropdownMenuItem(text = { Text("Manual", fontSize = 12.sp) }, onClick = { onSetProvider(LyricsProvider.Manual) })
                }
            }
            Box {
                AssistChip(
                    onClick = onToggleTranslatePopup,
                    label = {
                        Text(
                            when (state.translateProvider) {
                                TranslateProvider.Google -> "Google"
                                TranslateProvider.MyMemory -> "MyMemory"
                            },
                            fontSize = 11.sp
                        )
                    }
                )
                DropdownMenu(expanded = state.showTranslatePopup, onDismissRequest = onToggleTranslatePopup) {
                    DropdownMenuItem(text = { Text("Google", fontSize = 12.sp) }, onClick = { onSetTranslateProvider(TranslateProvider.Google) })
                    DropdownMenuItem(text = { Text("MyMemory", fontSize = 12.sp) }, onClick = { onSetTranslateProvider(TranslateProvider.MyMemory) })
                }
            }
            Spacer(Modifier.weight(1f))
            Text(currentTrackTitle ?: "—", color = TextDim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun CenterText(t: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(t, color = TextMute, fontSize = 13.sp)
    }
}
