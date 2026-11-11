package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistDialog(
    playlists: List<Playlist>,
    onDismiss: () -> Unit,
    onAddToPlaylist: (Playlist) -> Unit,
    onCreateNewPlaylist: (String, (Playlist) -> Unit) -> Unit
) {
    var showCreateInput by remember { mutableStateOf(false) }
    var newPlaylistName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPanel,
        titleContentColor = TextMain,
        textContentColor = TextMute,
        shape = RoundedCornerShape(12.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LibraryMusic, null, tint = Accent, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tambahkan ke Playlist", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 300.dp)) {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(playlists.filter { !it.isSystem && !it.id.startsWith("recently_added") && !it.id.startsWith("singles") && !it.id.startsWith("most_played") }) { pl ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onAddToPlaylist(pl) }
                                .padding(vertical = 10.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Folder, null, tint = TextMute, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(12.dp))
                            Text(pl.name, color = TextMain, fontSize = 14.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (showCreateInput) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        placeholder = { Text("Nama playlist baru", color = TextDim) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Accent,
                            unfocusedBorderColor = Border,
                            focusedTextColor = TextMain,
                            unfocusedTextColor = TextMain
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = {
                            if (newPlaylistName.isNotBlank()) {
                                onCreateNewPlaylist(newPlaylistName) { pl ->
                                    onAddToPlaylist(pl)
                                    showCreateInput = false
                                    newPlaylistName = ""
                                }
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "Simpan", tint = Accent)
                    }
                }
            } else {
                TextButton(onClick = { showCreateInput = true }) {
                    Icon(Icons.Default.Add, null, tint = Accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Playlist Baru", color = Accent)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Batal", color = TextMute)
            }
        }
    )
}
