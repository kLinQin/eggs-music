package com.eggs.musicplayer.model

data class AppSettings(
    val autoFetchLyrics: Boolean = true,
    val enableLyricsOvh: Boolean = false,
    val stretchLyricsDuration: Boolean = true
)
