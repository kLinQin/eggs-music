package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.lyrics.LyricsMode
import com.eggs.musicplayer.lyrics.LyricsProvider
import com.eggs.musicplayer.lyrics.LyricsUiState
import com.eggs.musicplayer.lyrics.TranslateProvider
import com.eggs.musicplayer.model.Track
import com.eggs.musicplayer.ui.theme.*

enum class MainTab { Playlist, Tracks, Lyrics, Tips }

@Composable
fun MainView(
    playlist: Playlist?,
    playlists: List<Playlist>,
    tracks: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    isMobile: Boolean,
    onSelectPlaylist: (String) -> Unit,
    onRenamePlaylist: (Playlist, String) -> Unit,
    onDeletePlaylist: (Playlist) -> Unit,
    onPlay: (Track) -> Unit,
    lyricsState: LyricsUiState,
    onLyricsMode: (LyricsMode) -> Unit,
    onLyricsProvider: (LyricsProvider) -> Unit,
    onTranslateProvider: (TranslateProvider) -> Unit,
    onToggleProviderPopup: () -> Unit,
    onToggleTranslatePopup: () -> Unit,
    onOpenManualLyrics: () -> Unit,
    onSearchLyrics: (String) -> Unit,
    onApplySearch: (Long) -> Unit,
    onSearchQueryChange: (String) -> Unit
) {
    var tab by remember { mutableStateOf(if (isMobile) MainTab.Playlist else MainTab.Tracks) }
    LaunchedEffect(isMobile) {
        if (!isMobile && tab == MainTab.Playlist) tab = MainTab.Tracks
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(8.dp))
            .background(Brush.verticalGradient(listOf(Color(0xFF1F1F1F), BgElev)))
    ) {
        // Header / hero
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF2A2A2A), Color(0xFF1F1F1F))))
                .padding(16.dp)
        ) {
            when (tab) {
                MainTab.Playlist -> HeroBox("Daftar", "Playlist", "${playlists.size} playlist", Icons.Default.QueueMusic)
                MainTab.Lyrics -> {
                    val cur = tracks.firstOrNull { it.id == currentTrackId }
                    HeroBox("Lirik", cur?.title ?: "Pilih lagu", cur?.artist ?: "—", Icons.Default.MusicNote)
                }
                MainTab.Tips -> HeroBox("Bantuan", "Tips & Panduan", "Panduan lengkap Egg Music", Icons.Default.Lightbulb)
                MainTab.Tracks -> HeroBox(
                    "Playlist",
                    playlist?.name ?: "Library",
                    "${tracks.size} lagu" + (if (tracks.isNotEmpty()) " • ${formatMs(tracks.sumOf { it.durationMs })}" else ""),
                    Icons.Default.MusicNote
                )
            }
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
        // Tabs
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isMobile) TabBtn("Playlist", tab == MainTab.Playlist, Modifier.weight(1f)) { tab = MainTab.Playlist }
            TabBtn("Lagu", tab == MainTab.Tracks, Modifier.weight(1f)) { tab = MainTab.Tracks }
            TabBtn("Lirik", tab == MainTab.Lyrics, Modifier.weight(1f)) { tab = MainTab.Lyrics }
            TabBtn("Tips", tab == MainTab.Tips, Modifier.weight(1f)) { tab = MainTab.Tips }
        }
        HorizontalDivider(thickness = 1.dp, color = Border)
        // Content
        when (tab) {
            MainTab.Playlist -> PlaylistTab(playlists, playlist?.id, onSelectPlaylist, onRenamePlaylist, onDeletePlaylist) {
                tab = MainTab.Tracks
            }
            MainTab.Tracks -> TracksTab(tracks, currentTrackId, isPlaying, onPlay)
            MainTab.Lyrics -> LyricsPanel(
                state = lyricsState,
                currentTrackTitle = tracks.firstOrNull { it.id == currentTrackId }?.title,
                onSelectMode = onLyricsMode,
                onSetProvider = onLyricsProvider,
                onSetTranslateProvider = onTranslateProvider,
                onToggleProviderPopup = onToggleProviderPopup,
                onToggleTranslatePopup = onToggleTranslatePopup,
                onOpenManual = onOpenManualLyrics,
                onSearch = onSearchLyrics,
                onApplySearch = onApplySearch,
                onSearchQueryChange = onSearchQueryChange
            )
            MainTab.Tips -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tips — coming soon", color = TextMute, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun HeroBox(label: String, title: String, sub: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(
            modifier = Modifier.size(88.dp).clip(RoundedCornerShape(8.dp))
                .background(Brush.linearGradient(listOf(Accent, Color(0xFF0D6E30)))),
            contentAlignment = Alignment.Center
        ) { Icon(icon, null, tint = Color(0x80000000), modifier = Modifier.size(40.dp)) }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
            Text(label.uppercase(), color = TextMute, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Text(title, color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(sub, color = TextMute, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun TabBtn(label: String, active: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp), modifier = Modifier.fillMaxWidth()) {
            Text(label, color = if (active) TextMain else TextMute, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
        Box(
            Modifier.height(2.dp).fillMaxWidth()
                .background(if (active) Accent else Color.Transparent)
        )
    }
}

@Composable
private fun PlaylistTab(
    playlists: List<Playlist>, currentId: String?,
    onSelect: (String) -> Unit, onRename: (Playlist, String) -> Unit, onDelete: (Playlist) -> Unit,
    onPick: () -> Unit
) {
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var text by remember { mutableStateOf("") }
    if (playlists.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Belum ada playlist", color = TextDim) }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        items(playlists.size) { i ->
            val pl = playlists[i]
            val current = pl.id == currentId
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(if (current) BgActive else Color.Transparent)
                    .clickable { onSelect(pl.id); onPick() }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    if (pl.isSystem) Icons.Default.LibraryMusic else Icons.Default.QueueMusic,
                    null, tint = if (current) Accent else TextMute, modifier = Modifier.size(16.dp)
                )
                if (editing?.id == pl.id) {
                    androidx.compose.foundation.text.BasicTextField(
                        value = text, onValueChange = { text = it }, singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 13.sp),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
                        modifier = Modifier.weight(1f).clip(RoundedCornerShape(4.dp)).background(BgActive).padding(6.dp)
                    )
                } else {
                    Text(pl.name, color = if (current) TextMain else TextMute, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                        maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                }
                if (!pl.isSystem && editing?.id != pl.id) {
                    IconButton(onClick = { editing = pl; text = pl.name }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Edit, "Rename", tint = TextMute, modifier = Modifier.size(14.dp))
                    }
                    IconButton(onClick = { onDelete(pl) }, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Delete, "Hapus", tint = TextMute, modifier = Modifier.size(14.dp))
                    }
                }
                if (editing?.id == pl.id) {
                    TextButton(onClick = {
                        if (text.isNotBlank()) onRename(pl, text.trim())
                        editing = null
                    }) { Text("OK", color = Accent, fontSize = 12.sp) }
                }
            }
        }
    }
}

@Composable
private fun TracksTab(tracks: List<Track>, currentId: String?, isPlaying: Boolean, onPlay: (Track) -> Unit) {
    if (tracks.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Playlist ini masih kosong.", color = TextDim, fontSize = 13.sp)
        }
        return
    }
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(tracks, key = { _, t -> t.id }) { idx, t ->
            val active = t.id == currentId
            Row(
                modifier = Modifier.fillMaxWidth().clickable { onPlay(t) }
                    .background(if (active) BgHover else Color.Transparent)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
                    if (active && isPlaying) {
                        Box(Modifier.size(10.dp).clip(CircleShape).background(Accent))
                    } else {
                        Text("${idx + 1}", color = if (active) Accent else TextMute, fontSize = 13.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(t.title, color = if (active) Accent else TextMain, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (!t.artist.isNullOrBlank()) Text(t.artist, color = TextMute, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(formatMs(t.durationMs), color = TextMute, fontSize = 12.sp)
            }
        }
    }
}

fun formatMs(ms: Long): String {
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}
