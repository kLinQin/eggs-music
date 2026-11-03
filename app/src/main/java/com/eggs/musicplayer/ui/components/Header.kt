package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.ui.theme.*

@Composable
fun Header(
    progress: Pair<Int, Int>? = null,
    onAddFiles: () -> Unit,
    onAddFolder: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(Bg)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(Accent),
                contentAlignment = Alignment.Center
            ) { Text("♪", color = Color.Black, fontSize = 18.sp, fontWeight = FontWeight.Bold) }
            Text("Egg Music", color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (progress != null) {
                Text("${progress.first}/${progress.second}", color = TextMute, fontSize = 12.sp)
            }
            IconButton(onClick = onAddFiles) {
                Icon(Icons.Default.Add, "Tambah Lagu", tint = TextMain)
            }
            IconButton(onClick = onAddFolder) {
                Icon(Icons.Default.CreateNewFolder, "Tambah Folder", tint = TextMain)
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, "Settings", tint = TextMain)
            }
        }
    }
    HorizontalDivider(thickness = 1.dp, color = Border)
}
