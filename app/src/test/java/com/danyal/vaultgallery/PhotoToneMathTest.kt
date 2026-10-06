package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoToneMathTest {
    @Test fun neutralLevelsAreIdentity() {
        val result = levelTransform(0f, 255f)
        assertEquals(1f, result.scale, .0001f)
        assertEquals(0f, result.offset, .0001f)
    }

    @Test fun raisedBlackAndLoweredWhiteExpandTheRemainingRange() {
        val result = levelTransform(20f, 220f)
        assertTrue(result.scale > 1f)
        assertTrue(result.offset < 0f)
        assertEquals(0f, 20f * result.scale + result.offset, .001f)
        assertEquals(255f, 220f * result.scale + result.offset, .001f)
    }

    @Test fun whiteBalanceReducesAHotRedSample() {
        val gains = neutralWhiteBalance(220f, 110f, 110f)
        assertTrue(gains.red < 1f)
        assertTrue(gains.green > 1f)
        assertEquals(gains.green, gains.blue, .0001f)
    }
}
