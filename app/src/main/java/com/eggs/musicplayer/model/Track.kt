package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tracks")
data class Track(
    @PrimaryKey val id: String,
    val uri: String,
    val title: String,
    val artist: String?,
    val album: String?,
    val durationMs: Long,
    val folderId: String,
    val coverUri: String? = null,
    val mimeType: String? = null,
    val addedAt: Long = System.currentTimeMillis(),
    val playCount: Int = 0
)
