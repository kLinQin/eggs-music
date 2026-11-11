package com.eggs.musicplayer.lyrics

object LyricsInstrumentalDetector {
    private val regex = Regex(
        """[(\[【「][^)\]】」]*\b(?:instrumental|off[ -]?vocal|inst)\b|(?:^|\s)[-–—~～]\s*(?:instrumental|off[ -]?vocal|inst\.?)(?:\s+ver(?:sion|\.)?)?\s*(?:[-–—~～]|$)""",
        RegexOption.IGNORE_CASE
    )
    fun isInstrumentalTitle(title: String?): Boolean {
        if (title.isNullOrBlank()) return false
        return regex.containsMatchIn(title)
    }
}
