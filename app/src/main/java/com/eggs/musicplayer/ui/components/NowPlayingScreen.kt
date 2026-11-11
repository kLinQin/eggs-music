package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.material3.SheetValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.eggs.musicplayer.lyrics.LyricsMode
import com.eggs.musicplayer.lyrics.LyricsProvider
import com.eggs.musicplayer.lyrics.LyricsUiState
import com.eggs.musicplayer.lyrics.TranslateProvider
import com.eggs.musicplayer.model.Track
import com.eggs.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    track: Track?,
    lyricsState: LyricsUiState,
    onSelectMode: (LyricsMode) -> Unit,
    onSetProvider: (LyricsProvider) -> Unit,
    onSetTranslateProvider: (TranslateProvider) -> Unit,
    onToggleProviderPopup: () -> Unit,
    onToggleTranslatePopup: () -> Unit,
    onOpenManual: () -> Unit,
    onClose: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Color.Transparent,
        scrimColor = Color.Black.copy(alpha = 0.6f),
        dragHandle = null,
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = true)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .background(Bg)
        ) {
            val coverPath = track?.coverUri
            if (!coverPath.isNullOrBlank()) {
                val ctx = LocalContext.current
                val req = remember(coverPath) {
                    ImageRequest.Builder(ctx).data(java.io.File(coverPath)).crossfade(true).build()
                }
                AsyncImage(
                    model = req,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().blur(40.dp),
                    contentScale = ContentScale.Crop
                )
            }

            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                BgElev.copy(alpha = 0.2f),
                                BgElev.copy(alpha = 0.7f),
                                BgElev
                            )
                        )
                    )
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(32.dp))

                Text(
                    track?.title ?: "Tidak ada lagu",
                    color = TextMain, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    track?.artist ?: "—",
                    color = TextMute, fontSize = 14.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(12.dp))

                Box(Modifier.weight(1f).fillMaxWidth()) {
                    if (lyricsState.loading) {
                        CircularProgressIndicator(color = Accent, modifier = Modifier.align(Alignment.Center))
                    } else if (lyricsState.result == null) {
                        Text(
                            lyricsState.error ?: "Lirik tidak tersedia",
                            color = TextMute, modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        val res = lyricsState.result
                        val listState = rememberLazyListState()
                        LaunchedEffect(lyricsState.activeLineIndex) {
                            if (lyricsState.activeLineIndex >= 0) {
                                runCatching { listState.animateScrollToItem(lyricsState.activeLineIndex) }
                            }
                        }
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            item { Spacer(Modifier.height(8.dp)) }
                            itemsIndexed(res.lines) { idx, line ->
                                val active = res.synced && idx == lyricsState.activeLineIndex
                                Column(Modifier.padding(vertical = 6.dp)) {
                                    Text(
                                        line.text,
                                        color = if (active) TextMain else TextMute,
                                        fontSize = if (active) 18.sp else 15.sp,
                                        fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                    val tr = lyricsState.translations.getOrNull(idx).orEmpty()
                                    if (lyricsState.mode != LyricsMode.Plain && tr.isNotBlank()) {
                                        Text(tr, color = Accent, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            item { Spacer(Modifier.height(24.dp)) }
                        }
                    }
                }

                if (lyricsState.translating) {
                    LinearProgressIndicator(
                        progress = { lyricsState.translateProgress / 100f },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        color = Accent,
                        trackColor = BgActive
                    )
                    Spacer(Modifier.height(8.dp))
                } else {
                    Spacer(Modifier.height(12.dp))
                }

                LyricsBottomBar(
                    state = lyricsState,
                    onSelectMode = onSelectMode,
                    onSetProvider = onSetProvider,
                    onSetTranslateProvider = onSetTranslateProvider,
                    onToggleProviderPopup = onToggleProviderPopup,
                    onToggleTranslatePopup = onToggleTranslatePopup
                )
            }
        }
    }
}

@Composable
private fun LyricsBottomBar(
    state: LyricsUiState,
    onSelectMode: (LyricsMode) -> Unit,
    onSetProvider: (LyricsProvider) -> Unit,
    onSetTranslateProvider: (TranslateProvider) -> Unit,
    onToggleProviderPopup: () -> Unit,
    onToggleTranslatePopup: () -> Unit
) {
    androidx.compose.foundation.lazy.LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(BgPanel.copy(alpha = 0.9f))
            .padding(vertical = 8.dp, horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(LyricsMode.entries.size) { i ->
            val m = LyricsMode.entries[i]
            val active = state.mode == m
            Box(
                Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (active) Accent else BgActive)
                    .clickable { onSelectMode(m) }
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    when(m) { LyricsMode.Plain -> "Plain"; LyricsMode.Indo -> "Indo"; LyricsMode.Inggris -> "Inggris" },
                    color = if (active) Color.Black else TextMute, fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                )
            }
        }

        item { Box(Modifier.width(1.dp).height(16.dp).background(Border)) }

        items(4) { i ->
            val (label, p) = when(i) {
                0 -> "LRCLIB" to LyricsProvider.Lrclib
                1 -> "ovh" to LyricsProvider.LyricsOvh
                2 -> "KuGou" to LyricsProvider.KuGou
                else -> "Manual" to LyricsProvider.Manual
            }
            LyricsChip(label, state.provider == p) { onSetProvider(p) }
        }

        item { Box(Modifier.width(1.dp).height(16.dp).background(Border)) }

        items(2) { i ->
            val (label, p) = if (i == 0) "Google" to TranslateProvider.Google else "MyMemory" to TranslateProvider.MyMemory
            LyricsChip(label, state.translateProvider == p) { onSetTranslateProvider(p) }
        }
    }
}
