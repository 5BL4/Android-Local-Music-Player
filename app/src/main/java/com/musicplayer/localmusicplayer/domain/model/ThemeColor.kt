package com.musicplayer.localmusicplayer.domain.model

import androidx.compose.ui.graphics.Color

enum class ThemeColor(val seedColor: Color, val displayNameEn: String, val displayNameZh: String) {
    White(Color(0xFFF8F8F8), "White", "白色"),
    Black(Color(0xFF1A1A1A), "Black", "黑色"),
    Red(Color(0xFFE53935), "Red", "红色"),
    Orange(Color(0xFFFB8C00), "Orange", "橙色"),
    Yellow(Color(0xFFFDD835), "Yellow", "黄色"),
    Green(Color(0xFF43A047), "Green", "绿色"),
    Blue(Color(0xFF1E88E5), "Blue", "蓝色"),
    Purple(Color(0xFF8E24AA), "Purple", "紫色"),
    Pink(Color(0xFFD81B60), "Pink", "粉色"),
    Gray(Color(0xFF757575), "Gray", "灰色");

    fun localized(): String {
        val lang = java.util.Locale.getDefault().language
        return if (lang == "zh") displayNameZh else displayNameEn
    }
}
