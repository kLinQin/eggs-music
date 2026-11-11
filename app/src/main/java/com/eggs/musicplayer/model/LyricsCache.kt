package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lyrics_cache")
data class LyricsCache(
    @PrimaryKey val trackId: String,
    val provider: String,
    val synced: Boolean,
    val plain: String?,
    val linesJson: String,
    val updatedAt: Long = System.currentTimeMillis()
)
