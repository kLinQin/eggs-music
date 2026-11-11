package com.eggs.musicplayer

import android.util.Log

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.documentfile.provider.DocumentFile
import com.eggs.musicplayer.data.AppDatabase
import com.eggs.musicplayer.data.FolderScanner
import com.eggs.musicplayer.player.PlayerController
import com.eggs.musicplayer.data.SettingsRepository
import com.eggs.musicplayer.lyrics.LyricsMode
import com.eggs.musicplayer.lyrics.LyricsProvider
import com.eggs.musicplayer.lyrics.LyricsRepository
import com.eggs.musicplayer.lyrics.LyricsResult
import com.eggs.musicplayer.lyrics.LyricsUiState
import com.eggs.musicplayer.lyrics.TranslateProvider
import com.eggs.musicplayer.lyrics.TranslateRepository
import com.eggs.musicplayer.lyrics.LyricsOvhRepository
import com.eggs.musicplayer.lyrics.LyricsKuGouRepository
import com.eggs.musicplayer.lyrics.LocalLyricsRepository
import com.eggs.musicplayer.lyrics.LyricsInstrumentalDetector
import com.eggs.musicplayer.lyrics.EmbeddedLyricsReader
import com.eggs.musicplayer.lyrics.LyricLine
import com.eggs.musicplayer.lyrics.LyricsSearchItem
import com.eggs.musicplayer.model.AppSettings
import com.eggs.musicplayer.model.LyricsCache
import com.eggs.musicplayer.model.TranslationCache
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import com.eggs.musicplayer.model.ManualLyrics
import kotlinx.coroutines.Job
import com.eggs.musicplayer.model.Playlist
import com.eggs.musicplayer.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    private val trackDao = db.trackDao()
    private val playlistDao = db.playlistDao()
    private val scanner = FolderScanner(app)
    private val settingsRepo = SettingsRepository(app)
    private val lyricsRepo = LyricsRepository()
    private val lyricsOvhRepo = LyricsOvhRepository()
    private val localLyricsRepo = LocalLyricsRepository(app)
    private val lyricsKuGouRepo = LyricsKuGouRepository()
    private val embeddedLyricsReader = EmbeddedLyricsReader(app)
    private val translateRepo = TranslateRepository()
    private val manualLyricsDao = db.manualLyricsDao()
    private val cacheDao = db.cacheDao()
    private val json = Json { ignoreUnknownKeys = true }
    private val _notFoundOnlineMS = mutableMapOf<String, Long>()
    private val NOT_FOUND_RETRY_MS = 6L * 60 * 60 * 1000

    private fun notFoundKey(trackId: String, provider: LyricsProvider) = trackId + "|" + provider.name
    private fun wasNotFoundRecently(trackId: String, provider: LyricsProvider): Boolean {
        val at = _notFoundOnlineMS[notFoundKey(trackId, provider)] ?: return false
        return (System.currentTimeMillis() - at) < NOT_FOUND_RETRY_MS
    }
    private fun markNotFound(trackId: String, provider: LyricsProvider) {
        _notFoundOnlineMS[notFoundKey(trackId, provider)] = System.currentTimeMillis()
    }

    private val _appSettings = MutableStateFlow(settingsRepo.load())
    val appSettings: StateFlow<AppSettings> = _appSettings.asStateFlow()

    private val _sleepTimerRemainingMs = MutableStateFlow<Long?>(null)
    val sleepTimerRemainingMs: StateFlow<Long?> = _sleepTimerRemainingMs.asStateFlow()
    private var sleepTimerJob: Job? = null

    fun recordPlay(trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { trackDao.incrementPlayCount(trackId) }
        }
    }

    private val _lyrics = MutableStateFlow(LyricsUiState())
    val lyrics: StateFlow<LyricsUiState> = _lyrics.asStateFlow()
    private var lyricsJob: Job? = null
    val playerController: PlayerController = PlayerController(
        app,
        resolveTrack = { uri -> trackDao.byUri(uri) },
        resolveTrackLike = { like -> trackDao.byUriLike(like) }
    )

    private val _tracks = MutableStateFlow<List<Track>>(emptyList())
    val tracks: StateFlow<List<Track>> = _tracks.asStateFlow()

    private val _playlists = MutableStateFlow<List<Playlist>>(emptyList())
    val playlists: StateFlow<List<Playlist>> = _playlists.asStateFlow()

    private val _importing = MutableStateFlow(false)
    val importing: StateFlow<Boolean> = _importing.asStateFlow()

    private val _progress = MutableStateFlow<Pair<Int, Int>?>(null)
    val progress: StateFlow<Pair<Int, Int>?> = _progress.asStateFlow()

    init {
        viewModelScope.launch {
            trackDao.all().collect { _tracks.value = it }
        }
        viewModelScope.launch {
            playlistDao.all().collect { _playlists.value = it }
        }
        viewModelScope.launch {
            if (playlistDao.byId("library") == null) {
                playlistDao.upsert(Playlist("library", "Library", isSystem = true))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerController.release()
        lyricsJob?.cancel()
        sleepTimerJob?.cancel()
    }

    fun updateAppSettings(s: AppSettings) {
        settingsRepo.save(s)
        _appSettings.value = s
    }

    private fun computeStretchMultiplier(res: LyricsResult, trackDurationMs: Long): Double {
        if (!_appSettings.value.stretchLyricsDuration) return 0.0
        val lenMs = res.lengthMs ?: return 0.0
        if (lenMs <= 0 || trackDurationMs <= 0) return 0.0
        val mult = trackDurationMs.toDouble() / lenMs.toDouble()
        return if (mult in 0.5..2.0) mult else 0.0
    }

    private fun applyStretch(res: LyricsResult, trackDurationMs: Long): LyricsResult {
        val mult = computeStretchMultiplier(res, trackDurationMs)
        if (mult == 0.0) return res
        return res.copy(lines = res.lines.map { LyricLine((it.timeMs * mult).toLong(), it.text) })
    }
    fun onTrackChangedForLyrics(trackId: String?, trackName: String?, artist: String?, album: String?, durationMs: Long, audioPath: String? = null) {
        val current = _lyrics.value
        if (trackId == current.trackId) return
        lyricsJob?.cancel()
        _lyrics.value = LyricsUiState(trackId = trackId)
        if (trackId == null) return
        lyricsJob = viewModelScope.launch {
            val manual = manualLyricsDao.byTrack(trackId)
            if (manual != null) {
                _lyrics.update { it.copy(provider = LyricsProvider.Manual, hasManual = true, result = parseManualText(manual.text)) }
                return@launch
            }
            _lyrics.update { it.copy(hasManual = false) }
            val ignoreMarker = runCatching { cacheDao.lyricsByTrack(trackId) }.getOrNull()
            if (ignoreMarker != null && ignoreMarker.provider == "IGNORE") {
                _lyrics.update { it.copy(isIgnored = true, loading = false, error = "Lirik ditandai tidak tersedia") }
                return@launch
            }
            if (LyricsInstrumentalDetector.isInstrumentalTitle(trackName)) {
                _lyrics.update { it.copy(isInstrumental = true, loading = false) }
                return@launch
            }
            val priority = _appSettings.value.lyricsSourcePriority
            val tryLocal = {
                val local = runCatching { localLyricsRepo.findSidecar(audioPath) }.getOrNull()
                if (local != null) {
                    val localStretched = applyStretch(local, durationMs)
                    _lyrics.update { it.copy(provider = LyricsProvider.Local, result = localStretched, loading = false, error = null) }
                    if (local.lines.isNotEmpty()) maybeTranslate()
                }
                local != null
            }
            val tryEmbedded = {
                val embedded = runCatching { embeddedLyricsReader.read(audioPath) }.getOrNull()
                if (embedded != null) {
                    val embeddedStretched = applyStretch(embedded, durationMs)
                    _lyrics.update { it.copy(provider = LyricsProvider.Local, result = embeddedStretched, loading = false, error = null) }
                    if (embedded.lines.isNotEmpty()) maybeTranslate()
                }
                embedded != null
            }
            if (priority == "embedded") {
                if (tryEmbedded()) return@launch
                if (tryLocal()) return@launch
            } else {
                if (tryLocal()) return@launch
                if (tryEmbedded()) return@launch
            }
            val st = _appSettings.value
            val provider = when {
                !st.autoFetchLyrics -> null
                st.enableLyricsOvh -> LyricsProvider.LyricsOvh
                else -> LyricsProvider.Lrclib
            }
            if (provider == null) return@launch
            _lyrics.update { it.copy(provider = provider) }
            fetchLyricsNow(trackId, trackName, artist, album, durationMs, provider)
        }
    }

    private fun serializeLyrics(res: LyricsResult): String {
        val arr = buildJsonArray {
            res.lines.forEach { line ->
                addJsonObject {
                    put("t", line.timeMs)
                    put("x", line.text)
                }
            }
        }
        return arr.toString()
    }
    private fun deserializeLyrics(synced: Boolean, plain: String?, jsonStr: String): LyricsResult? {
        return runCatching {
            val arr = json.parseToJsonElement(jsonStr).jsonArray
            val lines = arr.map { el ->
                val o = el.jsonObject
                LyricLine((o["t"]!!.jsonPrimitive.longOrNull ?: 0L), o["x"]!!.jsonPrimitive.content)
            }
            LyricsResult(synced, lines, plain)
        }.getOrNull()
    }

    private suspend fun fetchLyricsNow(trackId: String, trackName: String?, artist: String?, album: String?, durationMs: Long, provider: LyricsProvider) {
        if (wasNotFoundRecently(trackId, provider)) {
            _lyrics.update { it.copy(loading = false, error = "Lirik tidak tersedia (coba lagi nanti)", result = null) }
            return
        }
        Log.d("LyricsVM", "fetch provider=" + provider + " title=" + trackName + " artist=" + artist + " album=" + album + " dur=" + durationMs)
        _lyrics.update { it.copy(loading = true, error = null, result = null) }
        val cached = runCatching { cacheDao.lyricsByTrack(trackId) }.getOrNull()
        if (cached != null && cached.provider == provider.name) {
            val cachedRes = deserializeLyrics(cached.synced, cached.plain, cached.linesJson)
            if (cachedRes != null) {
                Log.d("LyricsVM", "cache hit provider=" + provider + " lines=" + cachedRes.lines.size)
                val stretched = applyStretch(cachedRes, durationMs)
                _lyrics.update { it.copy(loading = false, result = stretched, error = null) }
                if (cachedRes.lines.isNotEmpty()) maybeTranslate()
                return
            }
        }
        val res = runCatching {
            when (provider) {
                LyricsProvider.Lrclib -> lyricsRepo.fetchLyricsWithFallback(
                    trackName = trackName?.takeIf { it.isNotBlank() } ?: "unknown",
                    artistName = artist?.takeIf { it.isNotBlank() } ?: "unknown",
                    albumName = album,
                    durationSec = (durationMs / 1000).toInt().takeIf { it > 0 }
                )
                LyricsProvider.LyricsOvh -> lyricsOvhRepo.fetch(
                    artist?.takeIf { it.isNotBlank() } ?: "unknown",
                    trackName?.takeIf { it.isNotBlank() } ?: "unknown"
                )
                LyricsProvider.Manual, LyricsProvider.Local -> null
                LyricsProvider.KuGou -> lyricsKuGouRepo.fetch(
                    trackName = trackName?.takeIf { it.isNotBlank() } ?: "unknown",
                    artistName = artist?.takeIf { it.isNotBlank() } ?: "unknown",
                    albumName = album,
                    durationSec = (durationMs / 1000).toInt().takeIf { it > 0 }
                )
            }
        }.getOrElse { err ->
            _lyrics.update { it.copy(loading = false, error = err.message ?: "Gagal memuat lirik") }
            return
        }
        Log.d("LyricsVM", "result=" + res + " lines=" + (res?.lines?.size ?: 0))
        if (res != null) {
            runCatching {
                cacheDao.upsertLyrics(LyricsCache(
                    trackId = trackId,
                    provider = provider.name,
                    synced = res.synced,
                    plain = res.plain,
                    linesJson = serializeLyrics(res)
                ))
            }
        }
        if (res == null && (provider == LyricsProvider.Lrclib || provider == LyricsProvider.KuGou || provider == LyricsProvider.LyricsOvh)) {
            markNotFound(trackId, provider)
        }
        val finalRes = res?.let { applyStretch(it, durationMs) }
        _lyrics.update { it.copy(loading = false, result = finalRes, error = if (finalRes == null) "Lirik tidak tersedia" else null) }
        if (res != null) maybeTranslate()
    }

    fun ignoreLyrics() {
        val trackId = _lyrics.value.trackId ?: return
        viewModelScope.launch {
            runCatching {
                cacheDao.upsertLyrics(LyricsCache(
                    trackId = trackId,
                    provider = "IGNORE",
                    synced = false,
                    plain = null,
                    linesJson = "[]"
                ))
            }
            _lyrics.update { it.copy(provider = null, result = null, isIgnored = true, error = "Lirik ditandai tidak tersedia", translations = emptyList()) }
        }
    }

    fun removeIgnore() {
        val trackId = _lyrics.value.trackId ?: return
        viewModelScope.launch {
            runCatching { cacheDao.deleteLyrics(trackId) }
            _lyrics.update { it.copy(isIgnored = false, error = null) }
        }
    }

    fun setLyricsProvider(provider: LyricsProvider) {
        val cur = _lyrics.value
        val id = cur.trackId ?: return
        lyricsJob?.cancel()
        _lyrics.update { it.copy(provider = provider, showProviderPopup = false, result = null, translations = emptyList(), error = null) }
        if (provider == LyricsProvider.Manual) {
            _lyrics.update { it.copy(manualDialogOpen = true) }
            return
        }
        val tab = tracks.value.firstOrNull { it.id == id } ?: return
        lyricsJob = viewModelScope.launch {
            fetchLyricsNow(tab.id, tab.title, tab.artist, tab.album, tab.durationMs, provider)
        }
    }

    fun setLyricsMode(mode: LyricsMode) {
        _lyrics.update { it.copy(mode = mode) }
        if (mode == LyricsMode.Plain) _lyrics.update { it.copy(translations = emptyList()) }
        else maybeTranslate()
    }

    fun setTranslateProvider(p: TranslateProvider) {
        _lyrics.update { it.copy(translateProvider = p, translations = emptyList(), showTranslatePopup = false) }
        maybeTranslate()
    }

    fun toggleProviderPopup() { _lyrics.update { it.copy(showProviderPopup = !it.showProviderPopup, showTranslatePopup = false) } }
    fun toggleTranslatePopup() { _lyrics.update { it.copy(showTranslatePopup = !it.showTranslatePopup, showProviderPopup = false) } }

    fun updateActiveLine(positionMs: Long) {
        val res = _lyrics.value.result ?: return
        if (!res.synced) return
        var idx = -1
        for (i in res.lines.indices.reversed()) {
            if (positionMs >= res.lines[i].timeMs) { idx = i; break }
        }
        if (idx != _lyrics.value.activeLineIndex) _lyrics.update { it.copy(activeLineIndex = idx) }
    }

    fun openManualDialog() {
        val cur = _lyrics.value
        val txt = cur.result?.plain ?: cur.result?.lines?.joinToString("\n") { it.text } ?: ""
        _lyrics.update { it.copy(manualDialogOpen = true, manualText = txt) }
    }
    fun closeManualDialog() { _lyrics.update { it.copy(manualDialogOpen = false) } }
    fun updateManualText(t: String) { _lyrics.update { it.copy(manualText = t) } }
    fun saveManualLyrics() {
        val cur = _lyrics.value
        val id = cur.trackId ?: return
        val text = cur.manualText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            manualLyricsDao.upsert(ManualLyrics(id, text))
            _lyrics.update {
                it.copy(manualDialogOpen = false, hasManual = true, provider = LyricsProvider.Manual, result = parseManualText(text), error = null)
            }
            maybeTranslate()
        }
    }
    fun deleteManualLyrics() {
        val id = _lyrics.value.trackId ?: return
        viewModelScope.launch {
            manualLyricsDao.delete(id)
            _lyrics.update { it.copy(manualDialogOpen = false, hasManual = false, result = null, translations = emptyList()) }
        }
    }

    private fun parseManualText(text: String): LyricsResult {
        val regex = Regex("""\\[(\\d{2}):(\\d{2})(?:[.:](\\d{2,3}))?\\](.*)""")
        val lines = mutableListOf<LyricLine>()
        var hasTs = false
        for (raw in text.split("\n")) {
            val m = regex.find(raw)
            if (m != null) {
                hasTs = true
                val (min, sec, ms, t) = m.destructured
                val msVal = if (ms.isNotEmpty()) ms.padEnd(3, '0').toInt() else 0
                val timeMs = min.toLong() * 60000 + sec.toLong() * 1000 + msVal
                if (t.trim().isNotEmpty()) lines.add(LyricLine(timeMs, t.trim()))
            } else {
                if (raw.trim().isNotEmpty()) lines.add(LyricLine(0, raw.trim()))
            }
        }
        return if (hasTs) LyricsResult(true, lines.sortedBy { it.timeMs }, null)
               else LyricsResult(false, lines, text)
    }

    fun maybeTranslate() {
        val cur = _lyrics.value
        val res = cur.result ?: return
        if (cur.mode == LyricsMode.Plain) return
        if (res.lines.isEmpty()) return
        lyricsJob?.cancel()
        lyricsJob = viewModelScope.launch {
            _lyrics.update { it.copy(translating = true, translateProgress = 0, translations = emptyList()) }
            val target = if (cur.mode == LyricsMode.Inggris) "en" else "id"
            val provider = cur.translateProvider

            val tid = cur.trackId ?: return@launch

            val key = "$tid|$target|" + provider.name

            val cachedTr = runCatching { cacheDao.translationByKey(key) }.getOrNull()

            if (cachedTr != null) {

                val arr = runCatching { json.parseToJsonElement(cachedTr.linesJson).jsonArray.map { it.jsonPrimitive.content } }.getOrNull()

                if (arr != null) {

                    _lyrics.update { it.copy(translating = false, translations = arr, translateProgress = 100) }

                    return@launch

                }

            }
            val out = MutableList(res.lines.size) { "" }
            var done = 0
            for ((i, line) in res.lines.withIndex()) {
                val text = line.text.trim()
                if (text.length < 3) { done++; continue }
                val t = runCatching { translateRepo.translate(text, target, provider) }.getOrNull()
                if (!t.isNullOrBlank()) out[i] = t
                done++
                _lyrics.update { it.copy(translateProgress = (done * 100) / res.lines.size) }
            }
            runCatching {
                val arr = buildJsonArray { out.forEach { add(JsonPrimitive(it)) } }
                cacheDao.upsertTranslation(TranslationCache(
                    key = key,
                    trackId = tid,
                    targetLang = target,
                    provider = provider.name,
                    linesJson = arr.toString()
                ))
            }
            _lyrics.update { it.copy(translating = false, translations = out, translateProgress = 100) }
        }
    }

    fun updateSearchQuery(q: String) { _lyrics.update { it.copy(searchQuery = q) } }
    fun searchLyricsNow() {
        val q = _lyrics.value.searchQuery.trim()
        if (q.isEmpty()) return
        viewModelScope.launch {
            _lyrics.update { it.copy(searching = true, searchResults = emptyList()) }
            val r = runCatching { lyricsRepo.searchLyrics(q) }.getOrDefault(emptyList())
            _lyrics.update { it.copy(searching = false, searchResults = r) }
        }
    }
    fun applySearchResult(item: LyricsSearchItem) {
        viewModelScope.launch {
            _lyrics.update { it.copy(loading = true, provider = LyricsProvider.Lrclib, searchResults = emptyList(), searchQuery = "") }
            val r = runCatching { lyricsRepo.fetchLyricsById(item.id) }.getOrNull()
            _lyrics.update { it.copy(loading = false, result = r, error = if (r == null) "Gagal memuat lirik pilihan" else null) }
            if (r != null) maybeTranslate()
        }
    }

    fun deleteTrack(t: Track) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                trackDao.deleteByUri(t.uri)
                runCatching {
                    java.io.File(getApplication<Application>().filesDir, "covers/${t.id}.jpg").delete()
                }
            }
            if (playerController.currentTrack.value?.id == t.id) {
                playerController.toggle()
            }
        }
    }

    fun startSleepTimer(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalMs = minutes * 60_000L
        _sleepTimerRemainingMs.value = totalMs
        sleepTimerJob = viewModelScope.launch {
            val start = System.currentTimeMillis()
            while (true) {
                val remaining = totalMs - (System.currentTimeMillis() - start)
                if (remaining <= 0L) {
                    _sleepTimerRemainingMs.value = null
                    if (playerController.isPlaying.value) playerController.toggle()
                    return@launch
                }
                _sleepTimerRemainingMs.value = remaining
                kotlinx.coroutines.delay(1000)
            }
        }
    }
    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = null
    }

    fun refreshAllCovers() {
        viewModelScope.launch {
            val list = tracks.value.filter { it.coverUri.isNullOrBlank() }
            withContext(kotlinx.coroutines.Dispatchers.IO) {
                for (t in list) {
                    val path = scanner.refreshCoverFor(t)
                    if (path != null) trackDao.insertAll(listOf(t.copy(coverUri = path)))
                }
            }
        }
    }

    fun persistPermission(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching {
            getApplication<Application>().contentResolver
                .takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun createPlaylist(name: String, onCreated: (Playlist) -> Unit) {
        viewModelScope.launch {
            val id = java.util.UUID.randomUUID().toString()
            val pl = Playlist(id = id, name = name, isSystem = false)
            playlistDao.upsert(pl)
            onCreated(pl)
        }
    }

    fun addTrackToPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                playlistDao.insertPlaylistTrack(com.eggs.musicplayer.model.PlaylistTrack(playlistId, trackId))
            }
        }
    }

    fun removeTrackFromPlaylist(playlistId: String, trackId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { playlistDao.deletePlaylistTrack(playlistId, trackId) }
        }
    }

    fun playlistTrackIds(playlistId: String): Flow<List<String>> = playlistDao.trackIdsForPlaylist(playlistId)

    fun renamePlaylist(pl: Playlist, newName: String) {
        viewModelScope.launch { playlistDao.rename(pl.id, newName) }
    }

    fun deletePlaylist(pl: Playlist) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                trackDao.deleteByFolder(pl.id)
                playlistDao.delete(pl.id)
            }
        }
    }

    fun importSingleFile(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val t = scanner.readTrackFromUri(uri, "library")
                if (t != null) trackDao.insertAll(listOf(t))
            }
        }
    }

    fun importFolder(treeUri: Uri) {
        viewModelScope.launch {
            _importing.value = true
            _progress.value = 0 to 0
            try {
                persistPermission(treeUri)
                val (playlist, list) = scanner.scan(treeUri) { d, t ->
                    _progress.value = d to t
                }
                withContext(Dispatchers.IO) {
                    playlistDao.upsert(playlist)
                    trackDao.insertAll(list)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _importing.value = false
                _progress.value = null
            }
        }
    }
}
