package com.musicplayer.localmusicplayer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class LunarFormatterTest {

    @Test
    fun `formatLunarDate month 1 day 1 returns 正月初一`() {
        assertEquals("正月初一", LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 1))
    }

    @Test
    fun `formatLunarDate leap month 5 day 3 returns 闰五月初三`() {
        assertEquals("闰五月初三", LunarCalendar.formatLunarDate(month = 5, isLeapMonth = true, day = 3))
    }

    @Test
    fun `formatLunarDate month 12 day 30 returns 腊月三十`() {
        assertEquals("腊月三十", LunarCalendar.formatLunarDate(month = 12, isLeapMonth = false, day = 30))
    }

    @Test
    fun `formatLunarDate month 11 day 20 returns 冬月二十`() {
        assertEquals("冬月二十", LunarCalendar.formatLunarDate(month = 11, isLeapMonth = false, day = 20))
    }

    @Test
    fun `formatLunarDate day 10 returns 初十`() {
        assertEquals("正月初十", LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 10))
    }

    @Test
    fun `formatLunarDate day 11 returns 十一`() {
        assertEquals("正月十一", LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 11))
    }

    @Test
    fun `formatLunarDate day 20 returns 二十`() {
        assertEquals("正月二十", LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 20))
    }

    @Test
    fun `formatLunarDate day 21 returns 廿一`() {
        assertEquals("正月廿一", LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 21))
    }

    @Test
    fun `formatLunarDate day 29 returns 廿九`() {
        assertEquals("正月廿九", LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 29))
    }

    @Test
    fun `formatLunarDate leap month 6 day 1 returns 闰六月初一`() {
        assertEquals("闰六月初一", LunarCalendar.formatLunarDate(month = 6, isLeapMonth = true, day = 1))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatLunarDate month 0 throws IllegalArgumentException`() {
        LunarCalendar.formatLunarDate(month = 0, isLeapMonth = false, day = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatLunarDate month 13 throws IllegalArgumentException`() {
        LunarCalendar.formatLunarDate(month = 13, isLeapMonth = false, day = 1)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatLunarDate day 0 throws IllegalArgumentException`() {
        LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `formatLunarDate day 31 throws IllegalArgumentException`() {
        LunarCalendar.formatLunarDate(month = 1, isLeapMonth = false, day = 31)
    }
}
