package com.eggs.musicplayer.player

import android.media.audiofx.Equalizer
import android.util.Log
import android.content.Context
import com.eggs.musicplayer.data.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** State EQ: band frekuensi (Hz), range level (mB), dan level saat ini. */
data class EqState(
    val available: Boolean = false,
    val numberOfBands: Int = 0,
    val centerFreqsHz: List<Int> = emptyList(),
    val minLevelMb: Short = -1500,
    val maxLevelMb: Short = 1500,
    val currentLevelsMb: List<Short> = emptyList(),
    val presetName: String = "Flat",
    val error: String? = null
) {
    fun labelForBand(idx: Int): String {
        val hz = centerFreqsHz.getOrNull(idx) ?: return "?"
        return if (hz >= 1000) "${hz / 1000}k" else hz.toString()
    }
}

class EqualizerController(private val appContext: Context) {
    private val prefs = SettingsRepository(appContext)
    private var eq: Equalizer? = null
    private var sessionId: Int = -1

    private val _state = MutableStateFlow(EqState())
    val state: StateFlow<EqState> = _state.asStateFlow()

    val presets = listOf("Flat", "Bass Boost", "Vocal", "Rock", "Pop", "Electronic")

    /** Dipanggil saat audioSessionId valid. */
    fun attach(sessionId: Int) {
        if (sessionId == 0 || sessionId == this.sessionId) return
        releaseInternal()
        this.sessionId = sessionId
        try {
            val e = Equalizer(0, sessionId)
            e.enabled = true
            val n = e.numberOfBands.toInt()
            val freqs = (0 until n).map { (e.getCenterFreq(it.toShort()) / 1000) }
            val range = e.bandLevelRange
            val current = (0 until n).map { e.getBandLevel(it.toShort()) }
            eq = e
            _state.value = EqState(
                available = true,
                numberOfBands = n,
                centerFreqsHz = freqs,
                minLevelMb = range[0],
                maxLevelMb = range[1],
                currentLevelsMb = current,
                presetName = "Flat"
            )
            // Restore dari prefs
            val savedPreset = prefs.loadEqPreset()
            val savedLevels = prefs.loadEqLevels()
            if (savedLevels.size == n) {
                runCatching {
                    savedLevels.forEachIndexed { i, lv -> e.setBandLevel(i.toShort(), lv) }
                    _state.update { it.copy(currentLevelsMb = savedLevels, presetName = savedPreset) }
                }
            } else if (savedPreset != "Flat") {
                applyPreset(savedPreset)
            }
            Log.d("EqController", "attached session=$sessionId bands=$n range=${range[0]}..${range[1]}")
        } catch (t: Throwable) {
            Log.w("EqController", "attach failed: ${t.message}")
            _state.value = EqState(available = false, error = t.message)
        }
    }

    fun setBandLevel(index: Int, levelMb: Short) {
        val e = eq ?: return
        val n = _state.value.numberOfBands
        if (index !in 0 until n) return
        runCatching {
            e.setBandLevel(index.toShort(), levelMb)
            val newLevels = _state.value.currentLevelsMb.toMutableList()
            newLevels[index] = levelMb
            _state.update { it.copy(currentLevelsMb = newLevels, presetName = "Custom") }
            prefs.saveEqLevels(newLevels)
            prefs.saveEqPreset("Custom")
        }.onFailure { Log.w("EqController", "setBandLevel: ${it.message}") }
    }

    fun applyPreset(name: String) {
        val e = eq ?: return
        val s = _state.value
        if (!s.available || s.numberOfBands == 0) return
        val max = s.maxLevelMb.toInt()
        val min = s.minLevelMb.toInt()

        fun clampMb(v: Int): Short = v.coerceIn(min, max).toShort()

        val levels = (0 until s.numberOfBands).map { idx ->
            val hz = s.centerFreqsHz.getOrNull(idx) ?: 1000
            when (name) {
                "Flat" -> 0
                "Bass Boost" -> when {
                    hz < 150 -> max * 0.9
                    hz < 400 -> max * 0.6
                    hz < 1000 -> max * 0.2
                    else -> 0
                }
                "Vocal" -> when {
                    hz in 500..2000 -> max * 0.7
                    hz in 2000..4000 -> max * 0.4
                    hz < 200 -> -max * 0.3
                    else -> 0
                }
                "Rock" -> when {
                    hz < 200 -> max * 0.5
                    hz in 200..800 -> -max * 0.2
                    hz in 800..3000 -> max * 0.3
                    else -> max * 0.5
                }
                "Pop" -> when {
                    hz < 150 -> max * 0.3
                    hz in 150..500 -> max * 0.15
                    hz in 500..2000 -> -max * 0.15
                    else -> max * 0.25
                }
                "Electronic" -> when {
                    hz < 150 -> max * 0.7
                    hz < 500 -> max * 0.4
                    hz in 500..2000 -> -max * 0.25
                    else -> max * 0.3
                }
                else -> 0
            }.toInt()
        }.map { clampMb(it) }

        runCatching {
            levels.forEachIndexed { i, lv -> e.setBandLevel(i.toShort(), lv) }
            _state.update { it.copy(currentLevelsMb = levels, presetName = name) }
            prefs.saveEqLevels(levels)
            prefs.saveEqPreset(name)
        }.onFailure { Log.w("EqController", "applyPreset: ${it.message}") }
    }

    fun release() {
        releaseInternal()
        _state.value = EqState()
    }

    private fun releaseInternal() {
        runCatching { eq?.enabled = false }
        runCatching { eq?.release() }
        eq = null
        sessionId = -1
    }
}
