package com.eggs.musicplayer.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.eggs.musicplayer.model.Playlist
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY isSystem DESC, createdAt ASC")
    fun all(): Flow<List<Playlist>>

    @Query("SELECT * FROM playlists WHERE id = :id")
    suspend fun byId(id: String): Playlist?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(p: Playlist)

    @Query("DELETE FROM playlists WHERE id = :id AND isSystem = 0")
    suspend fun delete(id: String)

    @Query("UPDATE playlists SET name = :name WHERE id = :id AND isSystem = 0")
    suspend fun rename(id: String, name: String)

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId")
    fun trackIdsForPlaylist(playlistId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlaylistTrack(pt: com.eggs.musicplayer.model.PlaylistTrack)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun deletePlaylistTrack(playlistId: String, trackId: String)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId")
    suspend fun clearPlaylistTracks(playlistId: String)
}
