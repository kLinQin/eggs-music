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
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
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
            Column(modifier = Modifier.heightIn(max = 300.dp).verticalScroll(rememberScrollState())) {
                if (info.releaseNotes.isBlank()) {
                    Text("Versi baru telah tersedia. Silakan update untuk mendapatkan fitur terbaru.", fontSize = 13.sp, lineHeight = 18.sp)
                } else {
                    MarkdownText(info.releaseNotes)
                }
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

@Composable
private fun parseInlineMarkdown(text: String): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        var i = 0
        while (i < text.length) {
            if (i + 1 < text.length && text[i] == '*' && text[i+1] == '*') {
                val end = text.indexOf("**", i + 2)
                if (end != -1) {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = TextMain)) {
                        append(text.substring(i + 2, end))
                    }
                    i = end + 2
                    continue
                }
            }
            if (text[i] == '`') {
                val end = text.indexOf('`', i + 1)
                if (end != -1) {
                    withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = BgActive, color = Accent)) {
                        append(text.substring(i + 1, end))
                    }
                    i = end + 1
                    continue
                }
            }
            append(text[i])
            i++
        }
    }
}

@Composable
private fun MarkdownText(markdown: String) {
    Column {
        markdown.split("\n").forEach { line ->
            val trimmed = line.trim()
            when {
                trimmed.startsWith("### ") -> Text(parseInlineMarkdown(trimmed.removePrefix("### ")), color = Accent, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                trimmed.startsWith("## ") -> Text(parseInlineMarkdown(trimmed.removePrefix("## ")), color = TextMain, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                trimmed.startsWith("# ") -> Text(parseInlineMarkdown(trimmed.removePrefix("# ")), color = TextMain, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    Row(modifier = Modifier.padding(vertical = 2.dp)) {
                        Text("•", color = Accent, fontSize = 13.sp, modifier = Modifier.padding(end = 6.dp))
                        Text(parseInlineMarkdown(trimmed.substring(2)), color = TextMute, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                }
                trimmed.isNotEmpty() -> Text(parseInlineMarkdown(trimmed), color = TextMute, fontSize = 13.sp, lineHeight = 18.sp, modifier = Modifier.padding(vertical = 2.dp))
            }
        }
    }
}
