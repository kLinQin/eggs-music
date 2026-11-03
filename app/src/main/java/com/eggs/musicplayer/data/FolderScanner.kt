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
import java.io.File
import java.io.FileOutputStream

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
            var bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            if (bitrate <= 0 && duration > 0) {
                val size = runCatching {
                    context.contentResolver.openAssetFileDescriptor(uri, "r")?.use { it.length }
                }.getOrNull() ?: 0L
                if (size > 0) bitrate = ((size * 8L) / (duration / 1000L)).toInt()
            }
            val id = UUID.randomUUID().toString()
            val coverBytes = runCatching { retriever.embeddedPicture }.getOrNull()
            val coverPath = saveCover(id, coverBytes)
            Track(
                id = id,
                uri = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                folderId = folderId,
                coverUri = coverPath,
                mimeType = mime,
                bitrate = bitrate
            )
        } catch (e: Exception) { null } finally { runCatching { retriever.release() } }
    }

    /** Cari file cover di folder yang sama (cover.jpg, folder.jpg, album.jpg, front.jpg). */
    private fun findFolderCover(audioFile: DocumentFile): ByteArray? {
        val parent = audioFile.parentFile ?: return null
        val names = listOf("cover.jpg", "cover.jpeg", "folder.jpg", "folder.jpeg", "album.jpg", "album.jpeg", "front.jpg", "front.jpeg")
        for (n in names) {
            val f = parent.findFile(n) ?: continue
            if (f.isFile) {
                return runCatching {
                    context.contentResolver.openInputStream(f.uri)?.use { it.readBytes() }
                }.getOrNull()
            }
        }
        return null
    }

    private fun saveCover(trackId: String, bytes: ByteArray?): String? {
        if (bytes == null || bytes.isEmpty()) return null
        return runCatching {
            val dir = File(context.filesDir, "covers").apply { mkdirs() }
            val f = File(dir, "$trackId.jpg")
            FileOutputStream(f).use { it.write(bytes) }
            f.absolutePath
        }.getOrNull()
    }

    /** Ambil ulang cover untuk track lama (embedded / folder). */
    fun refreshCoverFor(track: Track): String? {
        val uri = Uri.parse(track.uri)
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            var bytes = runCatching { retriever.embeddedPicture }.getOrNull()
            if (bytes == null) {
                val doc = DocumentFile.fromSingleUri(context, uri)
                if (doc != null) bytes = findFolderCover(doc)
            }
            saveCover(track.id, bytes)
        } catch (e: Exception) { null }
          finally { runCatching { retriever.release() } }
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
            val id = UUID.randomUUID().toString()
            var coverBytes = runCatching { retriever.embeddedPicture }.getOrNull()
            if (coverBytes == null) coverBytes = findFolderCover(df)
            val coverPath = saveCover(id, coverBytes)
            Track(
                id = id,
                uri = uri.toString(),
                title = title,
                artist = artist,
                album = album,
                durationMs = duration,
                folderId = folderId,
                coverUri = coverPath,
                mimeType = mime
            )
        } catch (e: Exception) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
