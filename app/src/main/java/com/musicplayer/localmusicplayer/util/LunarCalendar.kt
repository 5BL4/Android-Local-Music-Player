package com.musicplayer.localmusicplayer.util

/**
 * Chinese lunar calendar utilities.
 *
 * Conversion uses [android.icu.util.ChineseCalendar] (platform-trusted, API 24+).
 * Formatting is pure Kotlin — JVM-unit-testable.
 */
object LunarCalendar {

    data class LunarDate(
        val year: Int,
        val month: Int,   // 1..12
        val day: Int,     // 1..30
        val isLeapMonth: Boolean
    )

    // ── Pure‑Kotlin formatting (no Android imports) ──────────────────────

    private val MONTH_NAMES = arrayOf(
        "正月", "二月", "三月", "四月", "五月", "六月",
        "七月", "八月", "九月", "十月", "冬月", "腊月"
    )

    private val DAY_NAMES = arrayOf(
        "初一", "初二", "初三", "初四", "初五", "初六", "初七", "初八", "初九", "初十",
        "十一", "十二", "十三", "十四", "十五", "十六", "十七", "十八", "十九", "二十",
        "廿一", "廿二", "廿三", "廿四", "廿五", "廿六", "廿七", "廿八", "廿九", "三十"
    )

    /**
     * Formats a lunar date into readable Chinese, e.g. "正月初一", "闰五月初三", "腊月三十".
     *
     * @throws IllegalArgumentException if month is not in 1..12 or day is not in 1..30.
     */
    fun formatLunarDate(month: Int, isLeapMonth: Boolean, day: Int): String {
        require(month in 1..12) { "month must be 1..12, was $month" }
        require(day in 1..30) { "day must be 1..30, was $day" }

        val monthStr = if (isLeapMonth) "闰${MONTH_NAMES[month - 1]}" else MONTH_NAMES[month - 1]
        val dayStr = DAY_NAMES[day - 1]
        return "$monthStr$dayStr"
    }

    /** Convenience overload: formats a [LunarDate] directly. */
    fun formatLunarDate(lunar: LunarDate): String = formatLunarDate(lunar.month, lunar.isLeapMonth, lunar.day)

    // ── Conversion via android.icu.util.ChineseCalendar ──────────────────

    /**
     * Converts a Gregorian [java.time.LocalDate] to a [LunarDate].
     *
     * Uses the platform [android.icu.util.ChineseCalendar] which is available
     * on every Android device running API 24+ (minSdk for this project is 26).
     */
    fun fromGregorian(date: java.time.LocalDate): LunarDate {
        val instant = date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        val calendar = android.icu.util.ChineseCalendar().apply {
            timeInMillis = instant.toEpochMilli()
        }

        val year = calendar.get(android.icu.util.Calendar.YEAR)
        // ChineseCalendar MONTH is 0‑based (0 = first month); add 1 for 1‑based output.
        val month = calendar.get(android.icu.util.Calendar.MONTH) + 1
        val day = calendar.get(android.icu.util.Calendar.DAY_OF_MONTH)
        val isLeap = calendar.get(android.icu.util.ChineseCalendar.IS_LEAP_MONTH) == 1

        return LunarDate(year = year, month = month, day = day, isLeapMonth = isLeap)
    }

    /** Convenience: today's lunar date. */
    fun today(): LunarDate = fromGregorian(java.time.LocalDate.now())
}
