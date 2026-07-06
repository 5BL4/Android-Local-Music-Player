package com.musicplayer.localmusicplayer.domain.model

/**
 * Visual style for the real-time audio waveform shown on the now-playing screen.
 *
 * Persisted via DataStore as the enum [name] (see [AppPreferences] / [ThemeRepository]),
 * mirroring the [SortOption] / [Language] persistence pattern.
 */
enum class WaveformStyle(val displayNameEn: String, val displayNameZh: String) {
    MirroredBars("Mirrored Bars", "镜像竖条"),
    Bars("Bars", "竖条"),
    Line("Smooth Line", "平滑曲线");

    val displayName: String
        get() = displayNameEn

    fun localized(): String {
        val lang = java.util.Locale.getDefault().language
        return if (lang == "zh") displayNameZh else displayNameEn
    }
}
