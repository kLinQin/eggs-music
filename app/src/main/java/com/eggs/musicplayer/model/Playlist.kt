package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class Playlist(
    @PrimaryKey val id: String,
    val name: String,
    val treeUri: String? = null,
    val isSystem: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
