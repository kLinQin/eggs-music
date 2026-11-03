package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "manual_lyrics")
data class ManualLyrics(
    @PrimaryKey val trackId: String,
    val text: String,
    val updatedAt: Long = System.currentTimeMillis()
)
