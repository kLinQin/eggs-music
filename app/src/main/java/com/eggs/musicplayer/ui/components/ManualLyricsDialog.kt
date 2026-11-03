package com.eggs.musicplayer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.eggs.musicplayer.ui.theme.*

@Composable
fun ManualLyricsDialog(
    text: String,
    hasManual: Boolean,
    onChange: (String) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(10.dp), color = BgPanel) {
            Column(Modifier.padding(14.dp).fillMaxWidth()) {
                Text("Lirik Manual", color = TextMain, fontSize = 15.sp)
                Spacer(Modifier.height(8.dp))
                Box(
                    Modifier.fillMaxWidth().heightIn(min = 180.dp, max = 340.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BgActive)
                        .padding(8.dp)
                ) {
                    BasicTextField(
                        value = text,
                        onValueChange = onChange,
                        textStyle = TextStyle(color = TextMain, fontSize = 13.sp),
                        cursorBrush = SolidColor(Accent),
                        modifier = Modifier.fillMaxSize(),
                        decorationBox = { inner ->
                            if (text.isEmpty()) Text("Tempel/ketik lirik. Format LRC ([mm:ss.xx]) atau teks biasa.", color = TextDim, fontSize = 12.sp)
                            inner()
                        }
                    )
                }
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (hasManual) {
                        TextButton(onClick = onDelete) { Text("Hapus", color = ErrorRed, fontSize = 12.sp) }
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = onDismiss) { Text("Batal", color = TextMute, fontSize = 12.sp) }
                    Button(
                        onClick = onSave,
                        enabled = text.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = androidx.compose.ui.graphics.Color.Black)
                    ) { Text("Simpan", fontSize = 12.sp) }
                }
            }
        }
    }
}
