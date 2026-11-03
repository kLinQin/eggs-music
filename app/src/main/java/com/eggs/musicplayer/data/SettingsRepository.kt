package com.eggs.musicplayer.data

import android.content.Context
import com.eggs.musicplayer.model.AppSettings

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("egg_music_prefs", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        autoFetchLyrics = prefs.getBoolean(KEY_AUTO_LYRICS, true),
        enableLyricsOvh = prefs.getBoolean(KEY_LYRICS_OVH, false)
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_LYRICS, s.autoFetchLyrics)
            .putBoolean(KEY_LYRICS_OVH, s.enableLyricsOvh)
            .apply()
    }

    companion object {
        private const val KEY_AUTO_LYRICS = "autoFetchLyrics"
        private const val KEY_LYRICS_OVH = "enableLyricsOvh"
    }
}
