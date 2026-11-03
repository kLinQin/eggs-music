package com.eggs.musicplayer.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

class LyricsOvhRepository {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }
    private val cache = mutableMapOf<String, LyricsResult?>()

    @Serializable
    private data class OvhResponse(val lyrics: String = "")

    suspend fun fetch(artist: String, title: String): LyricsResult? {
        val key = "$artist|$title"
        if (cache.containsKey(key)) return cache[key]
        val url = "https://api.lyrics.ovh/v1/" +
            URLEncoder.encode(artist, "UTF-8") + "/" +
            URLEncoder.encode(title, "UTF-8")
        val req = Request.Builder().url(url).build()
        val res = withContext(Dispatchers.IO) { client.newCall(req).execute() }
        res.use { r ->
            if (!r.isSuccessful) { cache[key] = null; return null }
            val body = r.body?.string() ?: return null
            val parsed = runCatching { json.decodeFromString<OvhResponse>(body) }.getOrNull() ?: return null
            val lyrics = parsed.lyrics.replace("\r\n", "\n").trim()
            if (lyrics.isBlank()) { cache[key] = null; return null }
            val out = LyricsResult(false, lyrics.split("\n").map { LyricLine(0, it) }, lyrics)
            cache[key] = out
            return out
        }
    }
}
