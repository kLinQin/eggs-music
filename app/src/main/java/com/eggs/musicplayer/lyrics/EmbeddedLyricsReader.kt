package com.eggs.musicplayer.lyrics

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import org.jaudiotagger.audio.AudioFileIO
import org.jaudiotagger.tag.FieldKey
import java.io.File
import java.io.FileOutputStream

class EmbeddedLyricsReader(private val context: Context) {
    private val tsRegex = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{2,3}))?\](.*)""")

    private fun parseLrc(lrc: String): List<LyricLine> {
        val out = mutableListOf<LyricLine>()
        for (raw in lrc.split("\n")) {
            val m = tsRegex.find(raw) ?: continue
            val (min, sec, ms, text) = m.destructured
            val msVal = if (ms.isNotEmpty()) ms.padEnd(3, '0').toInt() else 0
            val timeMs = min.toLong() * 60_000 + sec.toLong() * 1000 + msVal
            val clean = text.trim()
            if (clean.isNotEmpty()) out.add(LyricLine(timeMs, clean))
        }
        return out.sortedBy { it.timeMs }
    }

    fun read(audioUri: String?): LyricsResult? {
        if (audioUri.isNullOrBlank()) return null
        var tmp: File? = null
        return try {
            val file = if (audioUri.startsWith("content://")) {
                copyToTemp(audioUri).also { tmp = it }
            } else File(audioUri)
            if (file == null || !file.exists()) return null
            val audioFile = AudioFileIO.read(file)
            val tag = audioFile.tag ?: return null
            val lyricsField = runCatching { tag.getFirst(FieldKey.LYRICS) }.getOrNull()
            if (lyricsField.isNullOrBlank()) return null
            val text = lyricsField.trim()
            if (text.isBlank()) return null
            val lines = parseLrc(text)
            if (lines.isNotEmpty()) LyricsResult(true, lines, text)
            else LyricsResult(false, text.split("\n").map { LyricLine(0, it) }, text)
        } catch (e: Exception) {
            Log.d("EmbeddedLyrics", "read error: " + e.message)
            null
        } finally {
            runCatching { tmp?.delete() }
        }
    }

    private fun copyToTemp(uriStr: String): File? {
        return try {
            val uri = Uri.parse(uriStr)
            val name = DocumentFile.fromSingleUri(context, uri)?.name ?: "audio.tmp"
            val ext = name.substringAfterLast('.', "mp3")
            val tmp = File(context.cacheDir, "embed_lyrics_" + System.currentTimeMillis() + "." + ext)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { output -> input.copyTo(output) }
            } ?: return null
            tmp
        } catch (e: Exception) {
            Log.d("EmbeddedLyrics", "copy error: " + e.message)
            null
        }
    }
}
