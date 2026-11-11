package com.eggs.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eggs.musicplayer.model.Track
import kotlinx.coroutines.flow.Flow

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY title COLLATE NOCASE")
    fun all(): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE folderId = :folderId ORDER BY title COLLATE NOCASE")
    fun byFolder(folderId: String): Flow<List<Track>>

    @Query("SELECT * FROM tracks WHERE uri LIKE :uriLike LIMIT 1")
    suspend fun byUriLike(uriLike: String): Track?

    @Query("SELECT * FROM tracks WHERE uri = :uri LIMIT 1")
    suspend fun byUri(uri: String): Track?

    @Query("SELECT * FROM tracks ORDER BY addedAt DESC LIMIT 50")
    fun recentlyAdded(): Flow<List<Track>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<Track>)

    @Query("UPDATE tracks SET playCount = playCount + 1 WHERE id = :id")
    suspend fun incrementPlayCount(id: String)

    @Query("DELETE FROM tracks WHERE uri = :uri")
    suspend fun deleteByUri(uri: String)

    @Query("DELETE FROM tracks WHERE folderId = :folderId")
    suspend fun deleteByFolder(folderId: String)
}
