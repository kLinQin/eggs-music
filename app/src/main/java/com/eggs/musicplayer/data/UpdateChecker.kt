package com.eggs.musicplayer.data

import android.util.Log
import com.eggs.musicplayer.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionName: String, val downloadUrl: String, val releaseNotes: String)

object UpdateChecker {
    private const val REPO_OWNER = "kLinQin"
    private const val REPO_NAME = "eggs-music"

    suspend fun checkForUpdate(): UpdateInfo? = withContext(Dispatchers.IO) {


        try {
            val url = URL("https://api.github.com/repos/$REPO_OWNER/$REPO_NAME/releases/latest")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000

            if (conn.responseCode != 200) return@withContext null

            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(body)
            
            val tag = json.optString("tag_name").replace("v", "")
            val current = BuildConfig.VERSION_NAME
            
            if (isNewerVersion(tag, current)) {
                var apkUrl = json.optString("html_url")
                val assets = json.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val name = assets.getJSONObject(i).optString("name")
                        if (name.endsWith(".apk")) {
                            apkUrl = assets.getJSONObject(i).optString("browser_download_url")
                            break
                        }
                    }
                }
                return@withContext UpdateInfo(tag, apkUrl, json.optString("body"))
            }
        } catch (e: Exception) {
            Log.e("UpdateChecker", "Error: ${e.message}")
        }
        return@withContext null
    }

    private fun isNewerVersion(newV: String, currentV: String): Boolean {
        return try {
            val n = newV.split(".").map { it.toInt() }
            val c = currentV.split("-")[0].split(".").map { it.toInt() }
            for (i in 0 until maxOf(n.size, c.size)) {
                val nv = n.getOrElse(i) { 0 }
                val cv = c.getOrElse(i) { 0 }
                if (nv > cv) return true
                if (nv < cv) return false
            }
            false
        } catch (e: Exception) {
            newV != currentV
        }
    }
}
