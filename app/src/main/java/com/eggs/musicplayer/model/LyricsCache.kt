package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Cache hasil fetch lirik per track (LRCLIB / lyrics.ovh). */
@Entity(tableName = "lyrics_cache")
data class LyricsCache(
    @PrimaryKey val trackId: String,
    val provider: String,
    val synced: Boolean,
    val plain: String?,
    val linesJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
