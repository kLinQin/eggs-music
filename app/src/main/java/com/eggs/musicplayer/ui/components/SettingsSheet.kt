package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.window.Dialog
import com.eggs.musicplayer.model.AppSettings
import com.eggs.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    settings: AppSettings,
    onChange: (AppSettings) -> Unit,
    onRefreshCovers: () -> Unit,
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
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
            Text("Pengaturan", color = TextMain, fontSize = 15.sp)
            Spacer(Modifier.height(10.dp))
            SettingSwitch("Ambil lirik otomatis", settings.autoFetchLyrics) {
                onChange(settings.copy(autoFetchLyrics = it))
            }
            SettingSwitch("Aktifkan lyrics.ovh", settings.enableLyricsOvh) {
                onChange(settings.copy(enableLyricsOvh = it))
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = onRefreshCovers,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Refresh Cover Art", color = Accent, fontSize = 13.sp) }
            Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = TextMain, fontSize = 13.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
