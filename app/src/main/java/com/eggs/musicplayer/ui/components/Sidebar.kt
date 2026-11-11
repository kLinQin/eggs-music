package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.ui.theme.*

@Composable
fun Sidebar(
    playlists: List<Playlist>,
    currentId: String?,
    onSelect: (String) -> Unit,
    onRename: (Playlist, String) -> Unit,
    onDelete: (Playlist) -> Unit
) {
    var editing by remember { mutableStateOf<Playlist?>(null) }
    var editText by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(8.dp))
            .background(BgElev)
            .padding(12.dp)
    ) {
        Text(
            "PLAYLIST",
            color = TextMute,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(8.dp))
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(playlists, key = { it.id }) { pl ->
                val current = pl.id == currentId
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (current) BgActive else BgElev)
                        .clickable { onSelect(pl.id) }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val icon = when (pl.id) {
                        "recently_added" -> Icons.Default.Timer
                        "most_played" -> Icons.Default.Favorite
                        "singles" -> Icons.Default.MusicNote
                        "library" -> Icons.Default.LibraryMusic
                        else -> Icons.Default.Folder
                    }
                    Icon(
                        icon,
                        null,
                        tint = if (current) Accent else TextMute,
                        modifier = Modifier.size(20.dp)
                    )
                    if (editing?.id == pl.id) {
                        BasicInput(
                            value = editText,
                            onValueChange = { editText = it },
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = {
                            if (editText.isNotBlank()) onRename(pl, editText.trim())
                            editing = null
                        }) { Text("OK", color = Accent, fontSize = 12.sp) }
                    } else {
                        Text(
                            pl.name,
                            color = if (current) TextMain else TextMute,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (!pl.isSystem) {
                            IconButton(onClick = { editing = pl; editText = pl.name }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Edit, "Rename", tint = TextMute, modifier = Modifier.size(16.dp))
                            }
                            IconButton(onClick = { onDelete(pl) }, modifier = Modifier.size(28.dp)) {
                                Icon(Icons.Default.Delete, "Hapus", tint = TextMute, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BasicInput(value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    androidx.compose.foundation.text.BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 13.sp),
        cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent),
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(BgActive)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    )
}
