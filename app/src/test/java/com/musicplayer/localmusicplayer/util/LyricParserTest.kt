package com.musicplayer.localmusicplayer.util

import com.musicplayer.localmusicplayer.domain.model.LyricLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricParserTest {

    @Test
    fun `parse standard 3-digit millisecond timestamp`() {
        val result = LyricParser.parse("[01:23.456]hello")

        assertEquals(1, result.size)
        assertEquals(83456L, result[0].timestampMs)
        assertEquals("hello", result[0].text)
    }

    @Test
    fun `parse normalizes 2-digit millisecond via padEnd`() {
        // "45".padEnd(3, '0') = "450"
        val result = LyricParser.parse("[00:01.45]hi")

        assertEquals(1, result.size)
        assertEquals(1450L, result[0].timestampMs)
        assertEquals("hi", result[0].text)
    }

    @Test
    fun `parse skips 1-digit millisecond since regex requires 2-3 digits`() {
        // \d{2,3} requires at least 2 digits, so "5" alone won't match
        val result = LyricParser.parse("[00:01.5]hi")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse skips empty text lines`() {
        val result = LyricParser.parse("[00:00.000]")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse sorts lines by timestamp even if input is out of order`() {
        val lrc = """
            [00:03.000]third
            [00:01.000]first
            [00:02.000]second
        """.trimIndent()

        val result = LyricParser.parse(lrc)

        assertEquals(3, result.size)
        assertEquals("first", result[0].text)
        assertEquals(1000L, result[0].timestampMs)
        assertEquals("second", result[1].text)
        assertEquals(2000L, result[1].timestampMs)
        assertEquals("third", result[2].text)
        assertEquals(3000L, result[2].timestampMs)
    }

    @Test
    fun `parse ignores metadata tag lines via matchEntire`() {
        val lrc = """
            [ti:Song Title]
            [00:01.000]hello
        """.trimIndent()

        val result = LyricParser.parse(lrc)

        assertEquals(1, result.size)
        assertEquals("hello", result[0].text)
        assertEquals(1000L, result[0].timestampMs)
    }

    @Test
    fun `parse handles multiple lines with text`() {
        val lrc = """
            [00:01.000]hello
            [00:02.500]world
        """.trimIndent()

        val result = LyricParser.parse(lrc)

        assertEquals(2, result.size)
        assertEquals("hello", result[0].text)
        assertEquals(1000L, result[0].timestampMs)
        assertEquals("world", result[1].text)
        assertEquals(2500L, result[1].timestampMs)
    }

    @Test
    fun `parse returns empty list for empty string input`() {
        val result = LyricParser.parse("")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse returns empty list for blank input`() {
        val result = LyricParser.parse("   \n  \n ")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse trims whitespace from text`() {
        val result = LyricParser.parse("[00:01.000]  hello  ")

        assertEquals(1, result.size)
        assertEquals("hello", result[0].text)
        assertEquals(1000L, result[0].timestampMs)
    }

    @Test
    fun `parse merges lines sharing a timestamp into one entry`() {
        // Embedded lyrics store original + translation as separate [mm:ss.xx]
        // entries with identical timestamps (original block first, then translation).
        val lrc = """
            [00:01.000]English line
            [00:02.000]Second line
            [00:01.000]中文翻译
            [00:02.000]第二行
        """.trimIndent()

        val result = LyricParser.parse(lrc)

        assertEquals(2, result.size)
        assertEquals(1000L, result[0].timestampMs)
        assertEquals("English line\n中文翻译", result[0].text)
        assertEquals(2000L, result[1].timestampMs)
        assertEquals("Second line\n第二行", result[1].text)
    }
}
