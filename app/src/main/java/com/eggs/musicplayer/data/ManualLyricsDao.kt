package com.eggs.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eggs.musicplayer.model.ManualLyrics

@Dao
interface ManualLyricsDao {
    @Query("SELECT * FROM manual_lyrics WHERE trackId = :trackId LIMIT 1")
    suspend fun byTrack(trackId: String): ManualLyrics?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entry: ManualLyrics)

    @Query("DELETE FROM manual_lyrics WHERE trackId = :trackId")
    suspend fun delete(trackId: String)
}
