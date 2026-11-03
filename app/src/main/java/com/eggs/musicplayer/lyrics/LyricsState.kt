package com.eggs.musicplayer.lyrics

enum class LyricsMode { Plain, Indo, Inggris }

enum class LyricsProvider { Lrclib, LyricsOvh, Manual }

data class LyricsUiState(
    val trackId: String? = null,
    val provider: LyricsProvider? = null,
    val mode: LyricsMode = LyricsMode.Plain,
    val translateProvider: TranslateProvider = TranslateProvider.Google,
    val loading: Boolean = false,
    val translating: Boolean = false,
    val translateProgress: Int = 0,
    val result: LyricsResult? = null,
    val translations: List<String> = emptyList(),
    val error: String? = null,
    val hasManual: Boolean = false,
    val activeLineIndex: Int = -1,
    val manualText: String = "",
    val manualDialogOpen: Boolean = false,
    val searchQuery: String = "",
    val searchResults: List<LyricsSearchItem> = emptyList(),
    val searching: Boolean = false,
    val showProviderPopup: Boolean = false,
    val showTranslatePopup: Boolean = false
)
