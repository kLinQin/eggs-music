package com.eggs.musicplayer.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import kotlin.math.abs

class LyricsKuGouRepository {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val mutex = Mutex()
    private var lastAt = 0L
    private val cache = mutableMapOf<String, LyricsResult?>()

    @Serializable
    private data class SearchRoot(val candidates: List<Candidate> = emptyList())
    @Serializable
    private data class Candidate(
        val id: String = "",
        @SerialName("accesskey") val accessKey: String = "",
        val song: String = "",
        val singer: String = "",
        val duration: Long = 0
    )
    @Serializable
    private data class DownloadRoot(val content: String = "")

    private suspend fun throttle() {
        mutex.withLock {
            val now = System.currentTimeMillis()
            val wait = (400L - (now - lastAt)).coerceAtLeast(0L)
            if (wait > 0) kotlinx.coroutines.delay(wait)
            lastAt = System.currentTimeMillis()
        }
    }

    private suspend fun get(url: String): String? = withContext(Dispatchers.IO) {
        val req = Request.Builder().url(url)
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 10)")
            .build()
        runCatching { client.newCall(req).execute() }.getOrNull()?.use { r ->
            if (!r.isSuccessful) return@use null
            r.body?.string()
        }
    }

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

    suspend fun fetch(
        trackName: String,
        artistName: String,
        albumName: String?,
        durationSec: Int?
    ): LyricsResult? {
        val key = "$trackName|$artistName|$durationSec"
        if (cache.containsKey(key)) return cache[key]

        val durMs = (durationSec ?: 0) * 1000L
        val query = listOfNotNull(
            trackName.takeIf { it.isNotBlank() },
            artistName.takeIf { it.isNotBlank() }
        ).joinToString(" ")
        if (query.isBlank()) { cache[key] = null; return null }

        val searchUrl = buildString {
            append("https://lyrics.kugou.com/search?ver=1&man=yes&client=pc")
            append("&keyword=").append(URLEncoder.encode(query, "UTF-8"))
            if (durMs > 0) append("&duration=").append(durMs)
        }
        Log.d("KuGou", "search $searchUrl")
        throttle()
        val searchBody = get(searchUrl) ?: run { cache[key] = null; return null }
        val root = runCatching { json.decodeFromString<SearchRoot>(searchBody) }.getOrNull()
            ?: run { cache[key] = null; return null }
        if (root.candidates.isEmpty()) { cache[key] = null; return null }

        // pilih kandidat paling mirip durasi
        val best = root.candidates.minByOrNull { c ->
            if (durMs > 0 && c.duration > 0) abs(c.duration - durMs) else Long.MAX_VALUE
        } ?: root.candidates.first()

        if (best.id.isBlank() || best.accessKey.isBlank()) { cache[key] = null; return null }

        val dlUrl = "https://lyrics.kugou.com/download?ver=1&client=pc" +
            "&id=" + URLEncoder.encode(best.id, "UTF-8") +
            "&accesskey=" + URLEncoder.encode(best.accessKey, "UTF-8") +
            "&fmt=lrc&charset=utf8"
        throttle()
        val dlBody = get(dlUrl) ?: run { cache[key] = null; return null }
        val dl = runCatching { json.decodeFromString<DownloadRoot>(dlBody) }.getOrNull()
        if (dl == null || dl.content.isBlank()) { cache[key] = null; return null }

        val decoded = runCatching {
            String(android.util.Base64.decode(dl.content, android.util.Base64.DEFAULT), Charsets.UTF_8)
        }.getOrNull()
        if (decoded.isNullOrBlank()) { cache[key] = null; return null }

        val lines = parseLrc(decoded)
        val res = if (lines.isNotEmpty()) LyricsResult(true, lines, decoded)
                  else LyricsResult(false, decoded.split("\n").map { LyricLine(0, it) }, decoded)
        cache[key] = res
        return res
    }
}
