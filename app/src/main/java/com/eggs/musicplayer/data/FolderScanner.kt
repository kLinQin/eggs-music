package com.eggs.musicplayer.data

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

class FolderScanner(private val context: Context) {

    private val audioExts = setOf("mp3","flac","m4a","aac","ogg","wav","opus","wma","mp4")

    /** Scan tree URI, kembalikan (playlist, list track) dan progres via callback. */
    suspend fun scan(
        treeUri: Uri,
        onProgress: (done: Int, total: Int) -> Unit = { _, _ -> }
    ): Pair<Playlist, List<Track>> = withContext(Dispatchers.IO) {
        val root = DocumentFile.fromTreeUri(context, treeUri)
            ?: error("Tidak bisa buka folder")
        val folderName = root.name ?: "Folder"
        val playlistId = UUID.randomUUID().toString()

        val audioFiles = mutableListOf<DocumentFile>()
        collect(root, audioFiles)

        val list = mutableListOf<Track>()
        var done = 0
        val total = audioFiles.size
        for (df in audioFiles) {
            val t = readTrack(df, playlistId)
            if (t != null) list.add(t)
            done++
            if (done % 5 == 0 || done == total) onProgress(done, total)
        }
        onProgress(total, total)

        val playlist = Playlist(
            id = playlistId,
            name = folderName,
            treeUri = treeUri.toString(),
            isSystem = false
        )
        playlist to list
    }

    private fun collect(dir: DocumentFile, out: MutableList<DocumentFile>) {
        for (f in dir.listFiles()) {
            if (f.isDirectory) collect(f, out)
            else {
                val ext = f.name?.substringAfterLast('.', "")?.lowercase() ?: ""
                if (ext in audioExts) out.add(f)
            }
        }
    }

    fun readTrackFromUri(uri: Uri, folderId: String): Track? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val name = uri.lastPathSegment ?: "audio"
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: name.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val mime = context.contentResolver.getType(uri)
            Track(
                id = UUID.randomUUID().toString(),
                uri = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                folderId = folderId,
                mimeType = mime
            )
        } catch (e: Exception) { null } finally { runCatching { retriever.release() } }
    }

    private fun readTrack(df: DocumentFile, folderId: String): Track? {
        val uri = df.uri
        val name = df.name ?: return null
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: name.substringBeforeLast('.')
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val mime = context.contentResolver.getType(uri)
            Track(
                id = UUID.randomUUID().toString(),
                uri = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                folderId = folderId,
                mimeType = mime
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
