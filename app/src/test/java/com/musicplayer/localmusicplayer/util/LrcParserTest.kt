package com.musicplayer.localmusicplayer.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun `parse standard 3-digit millisecond timestamp`() {
        val result = LrcParser.parse("[01:23.456]hello")

        assertEquals(1, result.size)
        assertEquals(83456L, result[0].timeMs)
        assertEquals("hello", result[0].text)
    }

    @Test
    fun `parse normalizes 2-digit millisecond to 3-digit`() {
        // 45 < 100, so 45 * 10 = 450
        val result = LrcParser.parse("[00:01.45]hi")

        assertEquals(1, result.size)
        assertEquals(1450L, result[0].timeMs)
        assertEquals("hi", result[0].text)
    }

    @Test
    fun `parse skips empty text lines`() {
        val result = LrcParser.parse("[00:00.000]")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `parse sorts lines by timestamp even if input is out of order`() {
        val lrc = """
            [00:03.000]third
            [00:01.000]first
            [00:02.000]second
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(3, result.size)
        assertEquals("first", result[0].text)
        assertEquals(1000L, result[0].timeMs)
        assertEquals("second", result[1].text)
        assertEquals(2000L, result[1].timeMs)
        assertEquals("third", result[2].text)
        assertEquals(3000L, result[2].timeMs)
    }

    @Test
    fun `parseSyncedPair merges translation by matching timeMs`() {
        val orig = "[00:01.000]hello"
        val trans = "[00:01.000]\u4F60\u597D"

        val result = LrcParser.parseSyncedPair(orig, trans)

        assertEquals(1, result.size)
        assertEquals("hello\n\u4F60\u597D", result[0].text)
        assertEquals(1000L, result[0].timeMs)
    }

    @Test
    fun `parseSyncedPair with null tlyric returns original`() {
        val orig = "[00:01.000]hello"

        val result = LrcParser.parseSyncedPair(orig, null)

        assertEquals(1, result.size)
        assertEquals("hello", result[0].text)
        assertEquals(1000L, result[0].timeMs)
    }

    @Test
    fun `parseSyncedPair where translation has no matching timestamp leaves original unchanged`() {
        val orig = "[00:01.000]hello\n[00:02.000]world"
        val trans = "[00:03.000]extra"

        val result = LrcParser.parseSyncedPair(orig, trans)

        assertEquals(2, result.size)
        assertEquals("hello", result[0].text)
        assertEquals(1000L, result[0].timeMs)
        assertEquals("world", result[1].text)
        assertEquals(2000L, result[1].timeMs)
    }

    @Test
    fun `parse handles multiple lines with text`() {
        val lrc = """
            [00:01.000]hello
            [00:02.500]world
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(2, result.size)
        assertEquals("hello", result[0].text)
        assertEquals(1000L, result[0].timeMs)
        assertEquals("world", result[1].text)
        assertEquals(2500L, result[1].timeMs)
    }

    @Test
    fun `parse ignores metadata tag lines`() {
        val lrc = """
            [ti:Song Title]
            [00:01.000]hello
        """.trimIndent()

        val result = LrcParser.parse(lrc)

        assertEquals(1, result.size)
        assertEquals("hello", result[0].text)
    }

    @Test
    fun `parse edge case ms 100 is not normalized since it is at least 100`() {
        // 100 >= 100, so ms stays 100 (not multiplied by 10)
        val result = LrcParser.parse("[00:00.100]test")

        assertEquals(1, result.size)
        assertEquals(100L, result[0].timeMs)
        assertEquals("test", result[0].text)
    }

    @Test
    fun `parseSyncedPair merges partial translation matches`() {
        val orig = "[00:01.000]hello\n[00:02.000]world\n[00:03.000]foo"
        val trans = "[00:01.000]\u4F60\u597D\n[00:03.000]bar"

        val result = LrcParser.parseSyncedPair(orig, trans)

        assertEquals(3, result.size)
        assertEquals("hello\n\u4F60\u597D", result[0].text)
        assertEquals(1000L, result[0].timeMs)
        assertEquals("world", result[1].text)
        assertEquals(2000L, result[1].timeMs)
        assertEquals("foo\nbar", result[2].text)
        assertEquals(3000L, result[2].timeMs)
    }
}
