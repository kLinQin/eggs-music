package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.platform.LocalContext
import com.eggs.musicplayer.model.Track
import com.eggs.musicplayer.ui.theme.*

enum class RepeatMode { Off, All, One }

@Composable
fun PlayerBar(
    track: Track?,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    shuffle: Boolean,
    repeat: RepeatMode,
    volume: Float,
    isMuted: Boolean,
    onToggle: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onVolume: (Float) -> Unit,
    onToggleMute: () -> Unit,
    onOpenEq: () -> Unit,
    sleepTimerRemainingMs: Long? = null,
    onSleepTimer: (Int) -> Unit = {},
    onCancelSleepTimer: () -> Unit = {}
) {
    var sleepMenuOpen by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Bg)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        HorizontalDivider(thickness = 1.dp, color = Border, modifier = Modifier.padding(bottom = 6.dp))

        // ===== Baris 1: info (kiri) | EQ + mute + volume (kanan) =====
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(4.dp)).background(BgPanel),
                    contentAlignment = Alignment.Center
                ) {
                    val coverPath = track?.coverUri
                    if (!coverPath.isNullOrBlank()) {
                        AsyncImage(
                            model = coverPath,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop
                        )
                    } else {
                        Icon(Icons.Default.MusicNote, null, tint = TextDim, modifier = Modifier.size(20.dp))
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        track?.title ?: "Tidak ada lagu",
                        color = TextMain, fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        track?.artist ?: "—",
                        color = TextMute, fontSize = 11.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Box {
                    IconButton(onClick = { sleepMenuOpen = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            Icons.Default.Timer,
                            "Sleep Timer",
                            tint = if (sleepTimerRemainingMs != null) Accent else TextMute,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    androidx.compose.material3.DropdownMenu(
                        expanded = sleepMenuOpen,
                        onDismissRequest = { sleepMenuOpen = false }
                    ) {
                        listOf(5, 10, 15, 30, 45, 60).forEach { m ->
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("${m} menit", fontSize = 12.sp) },
                                onClick = { onSleepTimer(m); sleepMenuOpen = false }
                            )
                        }
                        if (sleepTimerRemainingMs != null) {
                            androidx.compose.material3.DropdownMenuItem(
                                text = { Text("Batalkan timer", fontSize = 12.sp, color = ErrorRed) },
                                onClick = { onCancelSleepTimer(); sleepMenuOpen = false }
                            )
                        }
                    }
                }
                IconButton(onClick = onOpenEq, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Tune, "EQ", tint = TextMute, modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleMute, modifier = Modifier.size(30.dp)) {
                    Icon(
                        if (isMuted || volume == 0f) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                        "Mute", tint = TextMute, modifier = Modifier.size(18.dp)
                    )
                }
                ThinSlider(
                    value = if (isMuted) 0f else volume,
                    onValueChange = onVolume,
                    modifier = Modifier.width(60.dp).height(20.dp)
                )
            }
        }

        Spacer(Modifier.height(6.dp))

        // ===== Baris 2: controls di tengah + seek di bawah =====
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(onClick = onToggleShuffle, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Shuffle, "Shuffle", tint = if (shuffle) Accent else TextMute, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onPrev, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipPrevious, "Prev", tint = TextMute, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(4.dp))
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(Accent).clickableNoRipple(onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        if (isPlaying) "Pause" else "Play",
                        tint = Color.Black, modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.SkipNext, "Next", tint = TextMute, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(4.dp))
                IconButton(onClick = onCycleRepeat, modifier = Modifier.size(32.dp)) {
                    Icon(
                        if (repeat == RepeatMode.One) Icons.Default.RepeatOne else Icons.Default.Repeat,
                        "Repeat",
                        tint = if (repeat != RepeatMode.Off) Accent else TextMute,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            // Seek full width di bawah controls
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(formatMs(positionMs), color = TextMute, fontSize = 10.sp, modifier = Modifier.width(36.dp))
                ThinSlider(
                    value = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f,
                    onValueChange = { onSeek((it * durationMs).toLong()) },
                    modifier = Modifier.weight(1f).height(20.dp)
                )
                Text(formatMs(durationMs), color = TextMute, fontSize = 10.sp, modifier = Modifier.width(36.dp))
            }
        }
    }
}

/** Slider tipis: track 4dp, thumb 12dp (persis CSS input[type=range]). */
@Composable
private fun ThinSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var widthPx by remember { mutableStateOf(1) }
    val density = LocalDensity.current
    val thumbDp = 12.dp
    val thumbPx = with(density) { thumbDp.toPx() }

    Box(
        modifier = modifier
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    val x = change.position.x.coerceIn(0f, widthPx.toFloat())
                    onValueChange(x / widthPx)
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val x = offset.x.coerceIn(0f, widthPx.toFloat())
                    onValueChange(x / widthPx)
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .align(Alignment.Center)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF4D4D4D))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(value.coerceIn(0f, 1f))
                .height(4.dp)
                .align(Alignment.CenterStart)
                .clip(RoundedCornerShape(2.dp))
                .background(Accent)
        )
        Box(
            modifier = Modifier
                .offset {
                    val x = (value.coerceIn(0f, 1f) * widthPx - thumbPx / 2f).toInt()
                    IntOffset(x, 0)
                }
                .size(thumbDp)
                .align(Alignment.CenterStart)
                .clip(CircleShape)
                .background(Color.White)
        )
    }
}

private fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier =
    this.clickable(interactionSource = MutableInteractionSource(), indication = null, onClick = onClick)

