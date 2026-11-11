package com.eggs.musicplayer.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "translation_cache")
data class TranslationCache(
    @PrimaryKey val key: String,        // "${trackId}|${targetLang}|${provider}"
    val trackId: String,
    val targetLang: String,
    val provider: String,
    val linesJson: String,              // JSON array string
    val updatedAt: Long = System.currentTimeMillis()
)
