package com.eggs.musicplayer.data

import android.content.Context
import com.eggs.musicplayer.model.AppSettings

class SettingsRepository(context: Context) {
    private val prefs = context.getSharedPreferences("egg_music_prefs", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        autoFetchLyrics = prefs.getBoolean(KEY_AUTO_LYRICS, true),
        enableLyricsOvh = prefs.getBoolean(KEY_LYRICS_OVH, false),
        stretchLyricsDuration = prefs.getBoolean(KEY_STRETCH_LYRICS, true)
    )

    fun save(s: AppSettings) {
        prefs.edit()
            .putBoolean(KEY_AUTO_LYRICS, s.autoFetchLyrics)
            .putBoolean(KEY_LYRICS_OVH, s.enableLyricsOvh)
            .putBoolean(KEY_STRETCH_LYRICS, s.stretchLyricsDuration)
            .apply()
    }

    fun loadEqPreset(): String = prefs.getString(KEY_EQ_PRESET, "Flat") ?: "Flat"
    fun saveEqPreset(name: String) { prefs.edit().putString(KEY_EQ_PRESET, name).apply() }

    fun loadEqLevels(): List<Short> = (prefs.getString(KEY_EQ_LEVELS, "") ?: "")
        .split(",").mapNotNull { it.toShortOrNull() }
    fun saveEqLevels(levels: List<Short>) {
        prefs.edit().putString(KEY_EQ_LEVELS, levels.joinToString(",")).apply()
    }

    fun loadVolume(): Float = prefs.getFloat(KEY_VOLUME, 1f)
    fun saveVolume(v: Float) { prefs.edit().putFloat(KEY_VOLUME, v).apply() }

    fun loadShuffle(): Boolean = prefs.getBoolean(KEY_SHUFFLE, false)
    fun saveShuffle(v: Boolean) { prefs.edit().putBoolean(KEY_SHUFFLE, v).apply() }

    fun loadRepeat(): String = prefs.getString(KEY_REPEAT, "Off") ?: "Off"
    fun saveRepeat(name: String) { prefs.edit().putString(KEY_REPEAT, name).apply() }

    companion object {
        private const val KEY_AUTO_LYRICS = "autoFetchLyrics"
        private const val KEY_LYRICS_OVH = "enableLyricsOvh"
        private const val KEY_STRETCH_LYRICS = "stretchLyricsDuration"
        private const val KEY_EQ_PRESET = "eqPreset"
        private const val KEY_EQ_LEVELS = "eqLevels"
        private const val KEY_VOLUME = "playerVolume"
        private const val KEY_SHUFFLE = "playerShuffle"
        private const val KEY_REPEAT = "playerRepeat"
    }
}
