package com.eggs.musicplayer.player

import android.content.ComponentName
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.eggs.musicplayer.model.Track
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PlayerController(
    private val context: Context,
    private val resolveTrack: (suspend (String) -> Track?)? = null,
    private val resolveTrackLike: (suspend (String) -> Track?)? = null
) {

    private val _currentTrack = MutableStateFlow<Track?>(null)
    val currentTrack: StateFlow<Track?> = _currentTrack.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _positionMs = MutableStateFlow(0L)
    val positionMs: StateFlow<Long> = _positionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _shuffle = MutableStateFlow(false)
    val shuffle: StateFlow<Boolean> = _shuffle.asStateFlow()

    private val _repeatMode = MutableStateFlow(0)
    val repeatMode: StateFlow<Int> = _repeatMode.asStateFlow()
    private val _queue = mutableListOf<Track>()
    private var _queueIndex = -1

    private val scope = CoroutineScope(Dispatchers.Main + Job())
    private var ticker: Job? = null

    private var controller: MediaController? = null
    private var future: ListenableFuture<MediaController>? = null
    private val trackIndex = mutableMapOf<String, Track>()

    init {
        val token = SessionToken(context, ComponentName(context, PlaybackService::class.java))
        future = MediaController.Builder(context, token).buildAsync()
        future!!.addListener({
            controller = future!!.get()
            controller?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(playing: Boolean) { _isPlaying.value = playing }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        _durationMs.value = controller?.duration?.coerceAtLeast(0) ?: 0L
                    }
                }
                override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) { _shuffle.value = shuffleModeEnabled }
                override fun onRepeatModeChanged(repeatMode: Int) { _repeatMode.value = repeatMode }
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    val uri = item?.mediaId ?: item?.localConfiguration?.uri?.toString()
                    if (uri != null) resolveAndSet(uri)
                }
            })
            controller?.let { c ->
                val item = c.currentMediaItem
                val uri = item?.mediaId ?: item?.localConfiguration?.uri?.toString()
                if (uri != null) resolveAndSet(uri)
                _isPlaying.value = c.isPlaying
                _durationMs.value = c.duration.coerceAtLeast(0)
                _positionMs.value = c.currentPosition.coerceAtLeast(0)
                _shuffle.value = c.shuffleModeEnabled
                _repeatMode.value = c.repeatMode
            }
            startTicker()
        }, MoreExecutors.directExecutor())
    }

    private fun resolveAndSet(uri: String) {
        val cached = trackIndex[uri]
        if (cached != null) {
            _currentTrack.value = cached
            return
        }
        scope.launch {
            val direct = runCatching { resolveTrack?.invoke(uri) }.getOrNull()
            val t = direct ?: runCatching {
                val like = "%" + uri.substringAfterLast("/") + "%"
                resolveTrackLike?.invoke(like)
            }.getOrNull()
            if (t != null) {
                trackIndex[uri] = t
                _currentTrack.value = t
            }
        }
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = scope.launch {
            while (true) {
                _positionMs.value = controller?.currentPosition?.coerceAtLeast(0) ?: 0L
                delay(500)
            }
        }
    }

    fun play(track: Track) {
        val c = controller ?: return
        trackIndex[track.uri] = track
        _currentTrack.value = track
        val item = MediaItem.Builder()
            .setUri(track.uri)
            .setMediaId(track.uri)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(track.title)
                    .setArtist(track.artist)
                    .setAlbumTitle(track.album)
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build()
            )
            .build()
        c.setMediaItem(item)
        c.prepare()
        c.play()
    }

    fun playQueue(tracks: List<Track>, startIndex: Int) {
        val c = controller ?: return
        if (tracks.isEmpty()) return
        val idx = startIndex.coerceIn(0, tracks.lastIndex)
        _queue.clear()
        _queue.addAll(tracks)
        _queueIndex = idx
        for (t in tracks) trackIndex[t.uri] = t
        val items = tracks.map { t ->
            MediaItem.Builder()
                .setUri(t.uri)
                .setMediaId(t.uri)
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle(t.title)
                        .setArtist(t.artist)
                        .setAlbumTitle(t.album)
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build()
                )
                .build()
        }
        c.setMediaItems(items, idx, 0L)
        c.prepare()
        c.play()
    }

    fun next() {
        val c = controller ?: return
        if (c.hasNextMediaItem()) c.seekToNextMediaItem()
    }

    fun prev() {
        val c = controller ?: return
        if (c.hasPreviousMediaItem()) c.seekToPreviousMediaItem()
        else c.seekTo(0)
    }

    fun setShuffle(enabled: Boolean) {
        controller?.shuffleModeEnabled = enabled
    }

    fun setRepeat(mode: Int) {
        controller?.repeatMode = mode
    }

    fun toggle() {
        val c = controller ?: return
        if (c.isPlaying) c.pause() else c.play()
    }

    fun seek(ms: Long) { controller?.seekTo(ms) }

    fun release() {
        ticker?.cancel()
        future?.let { MediaController.releaseFuture(it) }
        controller = null
    }
}
