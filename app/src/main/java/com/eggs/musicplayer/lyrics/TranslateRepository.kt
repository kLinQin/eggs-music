package com.eggs.musicplayer.lyrics

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder

enum class TranslateProvider { Google, MyMemory }

class TranslateQuotaException : Exception("Quota terjemahan habis")

class TranslateRepository {
    private val client = OkHttpClient.Builder().build()
    private val json = Json { ignoreUnknownKeys = true }

    // Flag blokir Google 60 detik jika 429/403
    @Volatile private var googleBlockedUntil = 0L

    private val fallbackSources = listOf("en", "ko", "ja", "zh", "id")

    // ============ MyMemory ============
    @Serializable
    private data class MyMemoryResp(
        val responseStatus: Int = 0,
        val responseDetails: String = "",
        val responseData: MyMemoryData? = null
    )
    @Serializable
    private data class MyMemoryData(val translatedText: String = "")

    private suspend fun translateMyMemory(text: String, target: String): String? {
        val sources = if (target == "en") listOf("en") else fallbackSources
        var last: String? = null
        for (src in sources) {
            if (src == target) continue
            val pair = "$src|$target"
            val url = "https://api.mymemory.translated.net/get?q=" +
                URLEncoder.encode(text, "UTF-8") + "&langpair=" + URLEncoder.encode(pair, "UTF-8")
            val req = Request.Builder().url(url).build()
            val res = withContext(Dispatchers.IO) { runCatching { client.newCall(req).execute() }.getOrNull() } ?: continue
            res.use { r ->
                if (!r.isSuccessful) return@use
                val body = r.body?.string() ?: return@use
                val parsed = runCatching { json.decodeFromString<MyMemoryResp>(body) }.getOrNull() ?: return@use
                if (parsed.responseStatus == 403) throw TranslateQuotaException()
                if (parsed.responseStatus == 200) {
                    val t = parsed.responseData?.translatedText
                    if (!t.isNullOrBlank()) return t
                }
            }
            if (last != null) break
        }
        return last
    }

    // ============ Google (unofficial) ============
    private fun googleBlocked(): Boolean = System.currentTimeMillis() < googleBlockedUntil
    private fun blockGoogle() { googleBlockedUntil = System.currentTimeMillis() + 60_000 }

    @Serializable
    private data class GoogleResp(val sentences: List<GoogleSentence> = emptyList())
    @Serializable
    private data class GoogleSentence(val trans: String = "", val orig: String = "")

    private suspend fun translateGoogle(text: String, target: String): String? {
        if (googleBlocked()) return null
        val url = "https://translate.googleapis.com/translate_a/single?client=gtx" +
            "&sl=auto&tl=$target&dt=t&q=" + URLEncoder.encode(text, "UTF-8")
        val req = Request.Builder().url(url).header("User-Agent", "Mozilla/5.0").build()
        val res = withContext(Dispatchers.IO) { runCatching { client.newCall(req).execute() }.getOrNull() } ?: return null
        res.use { r ->
            if (r.code == 429 || r.code == 403) { blockGoogle(); return null }
            if (!r.isSuccessful) return null
            val body = r.body?.string() ?: return null
            // Response Google: [[["teks","src",...],...],...]
            // Ambil semua segmen teks pertama.
            val arr = runCatching { json.parseToJsonElement(body) }.getOrNull() ?: return null
            val outer = arr as? kotlinx.serialization.json.JsonArray ?: return null
            val first = outer.firstOrNull() as? kotlinx.serialization.json.JsonArray ?: return null
            val sb = StringBuilder()
            for (el in first) {
                val seg = el as? kotlinx.serialization.json.JsonArray ?: continue
                val t = (seg.firstOrNull() as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull ?: continue
                sb.append(t)
            }
            return sb.toString().ifBlank { null }
        }
    }

    suspend fun translate(text: String, target: String, provider: TranslateProvider): String? {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return null
        return when (provider) {
            TranslateProvider.Google -> translateGoogle(trimmed, target) ?: translateMyMemory(trimmed, target)
            TranslateProvider.MyMemory -> translateMyMemory(trimmed, target) ?: translateGoogle(trimmed, target)
        }
    }
}
