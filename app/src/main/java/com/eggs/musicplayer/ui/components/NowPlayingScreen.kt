package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.eggs.musicplayer.model.Track
import com.eggs.musicplayer.lyrics.LyricsUiState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import com.eggs.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingScreen(
    track: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeat: RepeatMode,
    onClose: () -> Unit,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    lyricsState: LyricsUiState? = null
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = Bg,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Bg)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
        Spacer(Modifier.height(16.dp))
        Box(
            Modifier
                .fillMaxWidth(0.85f)
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp))
                .background(BgPanel),
            contentAlignment = Alignment.Center
        ) {
            val coverPath = track?.coverUri
            if (!coverPath.isNullOrBlank()) {
                val ctx = LocalContext.current
                val req = androidx.compose.runtime.remember(coverPath, ctx) {
                    ImageRequest.Builder(ctx)
                        .data(java.io.File(coverPath))
                        .size(1080, 1080)
                        .crossfade(true)
                        .build()
                }
                AsyncImage(
                    model = req,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(Icons.Default.MusicNote, null, tint = TextDim, modifier = Modifier.size(96.dp))
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            track?.title ?: "Tidak ada lagu",
            color = TextMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            track?.artist ?: "—",
            color = TextMute,
            fontSize = 14.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        lyricsState?.result?.let { res ->
            if (res.lines.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                val listState = rememberLazyListState()
                LaunchedEffect(lyricsState.activeLineIndex) {
                    if (lyricsState.activeLineIndex >= 0) {
                        runCatching { listState.animateScrollToItem(lyricsState.activeLineIndex) }
                    }
                }
                val center = lyricsState.activeLineIndex.coerceAtLeast(0)
                val windowStart = (center - 2).coerceAtLeast(0)
                val windowEnd = (windowStart + 5).coerceAtMost(res.lines.size)
                val window = res.lines.subList(windowStart, windowEnd)

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    window.forEachIndexed { i, line ->
                        val realIdx = windowStart + i
                        val active = res.synced && realIdx == lyricsState.activeLineIndex
                        Text(
                            line.text,
                            color = if (active) Accent else TextMute,
                            fontSize = if (active) 15.sp else 13.sp,
                            fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(formatMs(positionMs), color = TextMute, fontSize = 11.sp, modifier = Modifier.width(40.dp))
            ThinSlider(
                value = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                onValueChange = { onSeek((it * durationMs).toLong()) },
                modifier = Modifier.weight(1f).height(24.dp)
            )
            Text(formatMs(durationMs), color = TextMute, fontSize = 11.sp, modifier = Modifier.width(40.dp), textAlign = TextAlign.End)
        }

        Spacer(Modifier.height(16.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconButton(onClick = onToggleShuffle, modifier = Modifier.size(44.dp)) {
                Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) Accent else TextMute, modifier = Modifier.size(24.dp))
            }
            IconButton(onClick = onPrev, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.SkipPrevious, "Prev", tint = TextMain, modifier = Modifier.size(36.dp))
            }
            Box(
                modifier = Modifier.size(64.dp).clip(CircleShape).background(Accent).clickableNoRipple(onToggle),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    if (isPlaying) "Pause" else "Play",
                    tint = Color.Black,
                    modifier = Modifier.size(36.dp)
                )
            }
            IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.SkipNext, "Next", tint = TextMain, modifier = Modifier.size(36.dp))
            }
            IconButton(onClick = onCycleRepeat, modifier = Modifier.size(44.dp)) {
                Icon(
                    if (repeat == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                    "Repeat",
                    tint = if (repeat != RepeatMode.Off) Accent else TextMute,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        }
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(
        interactionSource = androidx.compose.foundation.interaction.MutableInteractionSource(),
        indication = null,
        onClick = onClick
    )
