package com.eggs.musicplayer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eggs.musicplayer.MainViewModel
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.ui.components.Header
import com.eggs.musicplayer.ui.components.MainView
import com.eggs.musicplayer.ui.components.LyricsPanel
import com.eggs.musicplayer.ui.components.ManualLyricsDialog
import com.eggs.musicplayer.ui.components.PlayerBar
import com.eggs.musicplayer.ui.components.SettingsSheet
import com.eggs.musicplayer.ui.components.RepeatMode
import com.eggs.musicplayer.ui.components.Sidebar
import com.eggs.musicplayer.ui.theme.Bg
import com.eggs.musicplayer.ui.theme.EggMusicTheme

@Composable
fun EggMusicApp(
    vm: MainViewModel,
    onPickFolder: () -> Unit,
    onPickFiles: () -> Unit
) {
    EggMusicTheme {
        Surface(color = Bg, modifier = Modifier.fillMaxSize()) {
            val tracks by vm.tracks.collectAsStateWithLifecycle()
            val playlists by vm.playlists.collectAsStateWithLifecycle()
            val progress by vm.progress.collectAsStateWithLifecycle()

            var currentPlaylistId by remember { mutableStateOf("library") }
            val playerTrack by vm.playerController.currentTrack.collectAsStateWithLifecycle()
            val isPlaying by vm.playerController.isPlaying.collectAsStateWithLifecycle()
            val positionMs by vm.playerController.positionMs.collectAsStateWithLifecycle()
            val durationMs by vm.playerController.durationMs.collectAsStateWithLifecycle()
            val currentTrackId = playerTrack?.id
            LaunchedEffect(currentTrackId) {
                val t = tracks.firstOrNull { it.id == currentTrackId }
                vm.onTrackChangedForLyrics(currentTrackId, t?.title, t?.artist, t?.album, t?.durationMs ?: 0L)
            }
            LaunchedEffect(positionMs, currentTrackId) {
                if (currentTrackId != null) vm.updateActiveLine(positionMs)
            }
            val lyricsState by vm.lyrics.collectAsStateWithLifecycle()
            val appSettings by vm.appSettings.collectAsStateWithLifecycle()
            var showSettings by remember { mutableStateOf(false) }
            var shuffle by remember { mutableStateOf(false) }
            var repeat by remember { mutableStateOf(RepeatMode.Off) }
            var volume by remember { mutableStateOf(1f) }
            var isMuted by remember { mutableStateOf(false) }

            val currentPlaylist = playlists.firstOrNull { it.id == currentPlaylistId }
                ?: playlists.firstOrNull()
            val filtered = if (currentPlaylistId == "library" || currentPlaylist == null) tracks
                else tracks.filter { it.folderId == currentPlaylist.id }

            Column(modifier = Modifier.fillMaxSize()) {
                Header(
                    progress = progress,
                    onAddFiles = onPickFiles,
                    onAddFolder = onPickFolder,
                    onOpenSettings = { showSettings = true }
                )
                val isWide = LocalConfiguration.current.screenWidthDp > 700
                Row(modifier = Modifier.weight(1f).padding(4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (isWide) {
                        Box(modifier = Modifier.width(300.dp)) {
                            Sidebar(
                                playlists = playlists,
                                currentId = currentPlaylistId,
                                onSelect = { currentPlaylistId = it },
                                onRename = { pl, name -> vm.renamePlaylist(pl, name) },
                                onDelete = { pl -> vm.deletePlaylist(pl) }
                            )
                        }
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        MainView(
                            playlist = currentPlaylist,
                            playlists = playlists,
                            tracks = filtered,
                            currentTrackId = currentTrackId,
                            isPlaying = isPlaying,
                            isMobile = !isWide,
                            onSelectPlaylist = { currentPlaylistId = it },
                            onRenamePlaylist = { pl, name -> vm.renamePlaylist(pl, name) },
                            onDeletePlaylist = { pl -> vm.deletePlaylist(pl) },
                            onPlay = { t -> vm.playerController.play(t) },
                                lyricsState = lyricsState,
                                onLyricsMode = vm::setLyricsMode,
                                onLyricsProvider = vm::setLyricsProvider,
                                onTranslateProvider = vm::setTranslateProvider,
                                onToggleProviderPopup = vm::toggleProviderPopup,
                                onToggleTranslatePopup = vm::toggleTranslatePopup,
                                onOpenManualLyrics = vm::openManualDialog,
                                onSearchLyrics = { _ -> vm.searchLyricsNow() },
                                onApplySearch = { id -> vm.applySearchResult(com.eggs.musicplayer.lyrics.LyricsSearchItem(id, "", "", "", 0, false, false)) },
                                onSearchQueryChange = vm::updateSearchQuery
                        )
                    }
                }
                if (showSettings) {
                    SettingsSheet(
                        settings = appSettings,
                        onChange = vm::updateAppSettings,
                        onDismiss = { showSettings = false }
                    )
                }
                if (lyricsState.manualDialogOpen) {
                    ManualLyricsDialog(
                        text = lyricsState.manualText,
                        hasManual = lyricsState.hasManual,
                        onChange = vm::updateManualText,
                        onSave = vm::saveManualLyrics,
                        onDelete = vm::deleteManualLyrics,
                        onDismiss = vm::closeManualDialog
                    )
                }
                PlayerBar(
                    track = playerTrack,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = if (durationMs > 0) durationMs else (playerTrack?.durationMs ?: 0L),
                    shuffle = shuffle,
                    repeat = repeat,
                    volume = volume,
                    isMuted = isMuted,
                    onToggle = { vm.playerController.toggle() },
                    onPrev = {
                        val i = filtered.indexOfFirst { it.id == currentTrackId }
                        if (i > 0) vm.playerController.play(filtered[i - 1])
                    },
                    onNext = {
                        val i = filtered.indexOfFirst { it.id == currentTrackId }
                        if (i >= 0 && i < filtered.lastIndex) vm.playerController.play(filtered[i + 1])
                    },
                    onSeek = { vm.playerController.seek(it) },
                    onToggleShuffle = { shuffle = !shuffle },
                    onCycleRepeat = {
                        repeat = when (repeat) {
                            RepeatMode.Off -> RepeatMode.All
                            RepeatMode.All -> RepeatMode.One
                            RepeatMode.One -> RepeatMode.Off
                        }
                    },
                    onVolume = { volume = it; isMuted = false },
                    onToggleMute = { isMuted = !isMuted },
                    onOpenEq = {}
                )
            }
        }
    }
}
