package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eggs.musicplayer.player.EqState
import com.eggs.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqState,
    presets: List<String>,
    onPreset: (String) -> Unit,
    onBandChange: (Int, Short) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = BgPanel,
            shape = RoundedCornerShape(10.dp)
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Text("Equalizer", color = TextMain, fontSize = 15.sp)
            Spacer(Modifier.height(4.dp))
            if (!state.available) {
                Text(state.error ?: "Equalizer tidak tersedia di perangkat ini", color = TextMute, fontSize = 12.sp)
                Spacer(Modifier.height(24.dp))
                return@Column
            }

            LazyRow(
                Modifier.fillMaxWidth().padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(presets) { p ->
                    PresetChip(p, state.presetName == p) { onPreset(p) }
                }
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(thickness = 1.dp, color = Border)
            Spacer(Modifier.height(10.dp))

            state.currentLevelsMb.forEachIndexed { idx, level ->
                val min = state.minLevelMb.toInt()
                val max = state.maxLevelMb.toInt()
                Column(Modifier.padding(vertical = 2.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.labelForBand(idx) + " Hz", color = TextMute, fontSize = 11.sp, modifier = Modifier.width(64.dp))
                        Slider(
                            value = level.toFloat(),
                            onValueChange = { onBandChange(idx, it.toInt().toShort()) },
                            valueRange = min.toFloat()..max.toFloat(),
                            modifier = Modifier.weight(1f),
                            colors = SliderDefaults.colors(
                                thumbColor = Accent,
                                activeTrackColor = Accent,
                                inactiveTrackColor = Color(0xFF4D4D4D)
                            )
                        )
                        Text(level.toString(), color = TextDim, fontSize = 10.sp, modifier = Modifier.width(48.dp))
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PresetChip(label: String, active: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(if (active) Accent else BgActive)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = if (active) Color.Black else TextMute, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
    }
}
