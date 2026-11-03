package com.eggs.musicplayer.lyrics

import android.util.Log

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

data class LyricLine(val timeMs: Long, val text: String)

data class LyricsResult(
    val synced: Boolean,
    val lines: List<LyricLine>,
    val plain: String?
)

data class LyricsSearchItem(
    val id: Long,
    val trackName: String,
    val artistName: String,
    val albumName: String,
    val durationSec: Int,
    val hasSynced: Boolean,
    val hasPlain: Boolean
)

class LyricsRateLimitException : Exception("Rate limit LRCLIB tercapai")

class LyricsRepository {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val throttleMutex = Mutex()
    private var lastRequestAt = 0L
    private val memoryCache = mutableMapOf<String, LyricsResult?>()
    private val searchCache = mutableMapOf<String, List<LyricsSearchItem>>()
    private val byIdCache = mutableMapOf<Long, LyricsResult?>()

    private suspend fun throttledExecute(url: String): okhttp3.Response {
        throttleMutex.withLock {
            val now = System.currentTimeMillis()
            val wait = (400L - (now - lastRequestAt)).coerceAtLeast(0L)
            if (wait > 0) delay(wait)
            lastRequestAt = System.currentTimeMillis()
        }
        val req = Request.Builder().url(url)
            .header("Lrclib-Client", "egg-music-android/1.0")
            .build()
        return withContext(Dispatchers.IO) { client.newCall(req).execute() }
    }

    private data class LrcResponse(
        val syncedLyrics: String?,
        val plainLyrics: String?,
        val instrumental: Boolean
    )
    private fun parseLrcResponse(body: String): LrcResponse? {
        val obj = runCatching { json.parseToJsonElement(body) as kotlinx.serialization.json.JsonObject }.getOrNull() ?: return null
        val synced = obj["syncedLyrics"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        val plain = obj["plainLyrics"]?.let { (it as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull }
        val inst = (obj["instrumental"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull() ?: false
        return LrcResponse(synced, plain, inst)
    }

    private fun parseLrc(lrc: String): List<LyricLine> {
        val regex = Regex("""\[(\d{2}):(\d{2})(?:[.:](\d{2,3}))?\](.*)""")
        val out = mutableListOf<LyricLine>()
        for (raw in lrc.split("\n")) {
            val m = regex.find(raw) ?: continue
            val (min, sec, ms, text) = m.destructured
            val msVal = if (ms.isNotEmpty()) ms.padEnd(3, '0').toInt() else 0
            val timeMs = min.toLong() * 60_000 + sec.toLong() * 1000 + msVal
            val clean = text.trim()
            if (clean.isNotEmpty()) out.add(LyricLine(timeMs, clean))
        }
        return out.sortedBy { it.timeMs }
    }

    private fun normalize(r: LrcResponse): LyricsResult? {
        if (r.instrumental) return null
        r.syncedLyrics?.let { s ->
            return LyricsResult(true, parseLrc(s), r.plainLyrics)
        }
        r.plainLyrics?.let { p ->
            return LyricsResult(false, p.split("\n").map { LyricLine(0, it) }, p)
        }
        return null
    }

    suspend fun fetchLyrics(
        trackName: String,
        artistName: String,
        albumName: String?,
        durationSec: Int?
    ): LyricsResult? {
        val key = "$trackName|$artistName|${albumName ?: ""}|${durationSec ?: 0}"
        if (memoryCache.containsKey(key)) return memoryCache[key]

        val sb = StringBuilder("https://lrclib.net/api/get?track_name=")
        sb.append(URLEncoder.encode(trackName, "UTF-8"))
        sb.append("&artist_name=").append(URLEncoder.encode(artistName, "UTF-8"))
        if (!albumName.isNullOrBlank()) sb.append("&album_name=").append(URLEncoder.encode(albumName, "UTF-8"))
        if (durationSec != null && durationSec in 1..3600) sb.append("&duration=").append(durationSec)

        Log.d("LyricsRepo", "GET " + sb.toString())
        val res = throttledExecute(sb.toString())
        res.use { r ->
            val peek = runCatching { r.peekBody(2048).string() }.getOrDefault("")
            Log.d("LyricsRepo", "code=" + r.code + " body=" + peek)
            if (r.code == 404) { memoryCache[key] = null; return null }
            if (r.code == 429) throw LyricsRateLimitException()
            if (!r.isSuccessful) return null
            val body = r.body?.string() ?: return null
            val parsed = runCatching { parseLrcResponse(body) }.getOrNull() ?: return null
            val out = normalize(parsed)
            memoryCache[key] = out
            return out
        }
    }

    @Serializable
    private data class SearchRaw(
        val id: Long,
        @SerialName("trackName") val trackName: String = "",
        @SerialName("artistName") val artistName: String = "",
        @SerialName("albumName") val albumName: String = "",
        val duration: Int = 0,
        @SerialName("syncedLyrics") val syncedLyrics: String? = null,
        @SerialName("plainLyrics") val plainLyrics: String? = null
    )

    suspend fun searchLyrics(query: String): List<LyricsSearchItem> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        searchCache[q]?.let { return it }
        val url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(q, "UTF-8")
        val res = throttledExecute(url)
        res.use { r ->
            if (r.code == 429) throw LyricsRateLimitException()
            if (!r.isSuccessful) return emptyList()
            val body = r.body?.string() ?: return emptyList()
            val arr = runCatching { json.decodeFromString<List<SearchRaw>>(body) }.getOrNull() ?: return emptyList()
            val mapped = arr.map {
                LyricsSearchItem(
                    id = it.id,
                    trackName = it.trackName,
                    artistName = it.artistName,
                    albumName = it.albumName,
                    durationSec = it.duration,
                    hasSynced = !it.syncedLyrics.isNullOrBlank(),
                    hasPlain = !it.plainLyrics.isNullOrBlank()
                )
            }
            searchCache[q] = mapped
            return mapped
        }
    }

    suspend fun fetchLyricsById(id: Long): LyricsResult? {
        byIdCache.containsKey(id)?.let { return byIdCache[id] }
        val url = "https://lrclib.net/api/get/$id"
        val res = throttledExecute(url)
        res.use { r ->
            if (r.code == 429) throw LyricsRateLimitException()
            if (!r.isSuccessful) return null
            val body = r.body?.string() ?: return null
            val parsed = runCatching { parseLrcResponse(body) }.getOrNull() ?: return null
            val out = normalize(parsed)
            byIdCache[id] = out
            return out
        }
    }
}
