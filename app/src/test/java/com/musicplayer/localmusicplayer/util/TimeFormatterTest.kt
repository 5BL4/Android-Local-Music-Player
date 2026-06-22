package com.musicplayer.localmusicplayer.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeFormatterTest {

    @Test
    fun `formatDuration 0ms returns 0_00`() {
        assertEquals("0:00", formatDuration(0L))
    }

    @Test
    fun `formatDuration 1000ms returns 0_01`() {
        assertEquals("0:01", formatDuration(1000L))
    }

    @Test
    fun `formatDuration 59999ms returns 0_59`() {
        assertEquals("0:59", formatDuration(59999L))
    }

    @Test
    fun `formatDuration 60000ms returns 1_00`() {
        assertEquals("1:00", formatDuration(60000L))
    }

    @Test
    fun `formatDuration 1 hour returns 60_00`() {
        assertEquals("60:00", formatDuration(3600000L))
    }

    @Test
    fun `formatDuration 3661000ms returns 61_01`() {
        assertEquals("61:01", formatDuration(3661000L))
    }

    @Test
    fun `formatDuration 5999000ms returns 99_59`() {
        assertEquals("99:59", formatDuration(5999000L))
    }

    @Test
    fun `formatDuration truncates sub-second values`() {
        assertEquals("0:01", formatDuration(1500L))
    }

    @Test
    fun `formatDuration handles 500ms`() {
        assertEquals("0:00", formatDuration(500L))
    }

    @Test
    fun `formatDuration single digit seconds pads with zero`() {
        assertEquals("0:09", formatDuration(9000L))
    }
}
