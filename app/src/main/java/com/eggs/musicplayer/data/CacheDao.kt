package com.eggs.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eggs.musicplayer.model.LyricsCache
import com.eggs.musicplayer.model.TranslationCache

@Dao
interface CacheDao {
    @Query("SELECT * FROM lyrics_cache WHERE trackId = :trackId LIMIT 1")
    suspend fun lyricsByTrack(trackId: String): LyricsCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLyrics(entry: LyricsCache)

    @Query("DELETE FROM lyrics_cache WHERE trackId = :trackId")
    suspend fun deleteLyrics(trackId: String)

    @Query("SELECT * FROM translation_cache WHERE key = :key LIMIT 1")
    suspend fun translationByKey(key: String): TranslationCache?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTranslation(entry: TranslationCache)

    @Query("DELETE FROM translation_cache WHERE trackId = :trackId")
    suspend fun deleteTranslationsForTrack(trackId: String)
}
