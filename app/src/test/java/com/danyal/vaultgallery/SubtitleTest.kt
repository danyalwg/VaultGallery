package com.danyal.vaultgallery

import com.danyal.vaultgallery.editor.SubtitleCue
import com.danyal.vaultgallery.editor.parseSrt
import com.danyal.vaultgallery.editor.serializeSrt
import org.junit.Assert.assertEquals
import org.junit.Test

class SubtitleTest {
    @Test fun srtRoundTripPreservesTimingAndMultilineText() {
        val original = listOf(
            SubtitleCue(startMs = 1_250, endMs = 3_900, text = "First line\nSecond line"),
            SubtitleCue(startMs = 3_600_001, endMs = 3_602_345, text = "Later"),
        )
        val parsed = parseSrt(serializeSrt(original))
        assertEquals(original.map { it.copy(id = "") }, parsed.map { it.copy(id = "") })
    }

    @Test fun malformedBlocksAreIgnoredWithoutDiscardingValidCues() {
        val parsed = parseSrt("garbage\n\n1\n00:00:01,000 --> 00:00:02,000\nHello\n")
        assertEquals(1, parsed.size)
        assertEquals("Hello", parsed.single().text)
    }
}
