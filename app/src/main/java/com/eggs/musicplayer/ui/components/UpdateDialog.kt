package com.eggs.musicplayer.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.eggs.musicplayer.data.UpdateInfo
import com.eggs.musicplayer.ui.theme.*

@Composable
fun UpdateDialog(
    info: UpdateInfo,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = BgPanel,
        titleContentColor = TextMain,
        textContentColor = TextMute,
        shape = RoundedCornerShape(12.dp),
        title = {
            Text("Update Tersedia (v${info.versionName})", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.heightIn(max = 250.dp).verticalScroll(rememberScrollState())) {
                Text(
                    if (info.releaseNotes.isBlank()) "Versi baru telah tersedia. Silakan update untuk mendapatkan fitur terbaru." else info.releaseNotes,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.downloadUrl))
                context.startActivity(intent)
                onDismiss()
            }) {
                Text("Update Sekarang", color = Accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nanti", color = TextDim)
            }
        }
    )
}
