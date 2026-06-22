package com.musicplayer.localmusicplayer.domain.model

enum class SortOption(val displayNameEn: String, val displayNameZh: String) {
    Title("Title", "标题"),
    Artist("Artist", "艺术家"),
    Album("Album", "专辑"),
    DateAdded("Date Added", "添加日期"),
    Duration("Duration", "时长");

    val displayName: String
        get() = displayNameEn

    fun localized(): String {
        val lang = java.util.Locale.getDefault().language
        return if (lang == "zh") displayNameZh else displayNameEn
    }
}
