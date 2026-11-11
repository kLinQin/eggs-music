package com.eggs.musicplayer.lyrics

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File

class LocalLyricsRepository(private val context: Context) {
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

    private fun toResult(name: String, text: String): LyricsResult? {
        if (text.isBlank()) return null
        val isLrc = name.endsWith(".lrc", true)
        if (isLrc) {
            val lines = parseLrc(text)
            if (lines.isNotEmpty()) return LyricsResult(true, lines, text)
        }
        return LyricsResult(false, text.split("\n").map { LyricLine(0, it) }, text)
    }

    fun findSidecar(audioPath: String?): LyricsResult? {
        if (audioPath.isNullOrBlank()) return null
        return try {
            if (audioPath.startsWith("content://")) findSidecarContent(audioPath)
            else findSidecarFile(audioPath)
        } catch (e: Exception) {
            Log.d("LocalLyrics", "findSidecar error: " + e.message)
            null
        }
    }

    private fun findSidecarFile(audioPath: String): LyricsResult? {
        val f = File(audioPath)
        if (!f.exists()) return null
        val base = f.nameWithoutExtension
        val dir = f.parentFile ?: return null
        for (ext in listOf("lrc", "LRC", "txt", "TXT")) {
            val sidecar = File(dir, "$base.$ext")
            if (!sidecar.exists() || sidecar.length() == 0L) continue
            val text = runCatching { sidecar.readText() }.getOrNull() ?: continue
            toResult(sidecar.name, text)?.let { return it }
        }
        return null
    }

    private fun findSidecarContent(audioUriStr: String): LyricsResult? {
        return try {
            val uri = Uri.parse(audioUriStr)
            val docId = runCatching { DocumentsContract.getDocumentId(uri) }.getOrNull() ?: return null
            val audioDoc = DocumentFile.fromSingleUri(context, uri) ?: return null
            val name = audioDoc.name ?: return null
            val base = name.substringBeforeLast('.', name)
            val authority = uri.authority ?: return null

            val parentDocId = docId.substringBeforeLast('/')
            val parentUri = DocumentsContract.buildDocumentUri(authority, parentDocId)
            val parent = DocumentFile.fromSingleUri(context, parentUri) ?: return null

            for (ext in listOf("lrc", "LRC", "txt", "TXT")) {
                val sidecar = parent.findFile("$base.$ext") ?: continue
                if (!sidecar.exists()) continue
                val text = runCatching {
                    context.contentResolver.openInputStream(sidecar.uri)?.bufferedReader()?.use { it.readText() }
                }.getOrNull() ?: continue
                toResult(sidecar.name ?: "", text)?.let { return it }
            }
            null
        } catch (e: Exception) {
            Log.d("LocalLyrics", "content uri sidecar: " + e.message)
            null
        }
    }
}
