package com.eggs.musicplayer.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.eggs.musicplayer.model.LyricsCache
import com.eggs.musicplayer.model.ManualLyrics
import com.eggs.musicplayer.model.TranslationCache
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.model.Track

@Database(entities = [Track::class, Playlist::class, ManualLyrics::class, LyricsCache::class, TranslationCache::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun playlistDao(): PlaylistDao
    abstract fun manualLyricsDao(): ManualLyricsDao
    abstract fun cacheDao(): CacheDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "music.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
