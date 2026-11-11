package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "playlist_tracks",
    primaryKeys = ["playlistId", "trackId"],
    indices = [Index("playlistId"), Index("trackId")]
)
data class PlaylistTrack(
    val playlistId: String,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)
