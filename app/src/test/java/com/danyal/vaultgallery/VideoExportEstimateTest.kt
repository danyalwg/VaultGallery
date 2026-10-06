package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoExportEstimateTest {
    @Test fun trimAndSpeedAffectDuration() {
        val estimate = estimateVideoExport(3840, 2160, 20_000_000, 1_000f, 11_000f, 2f, VideoCropRect.Full, 0, 1080, false)
        assertEquals(5_000L, estimate.durationMs)
        assertEquals(1080, estimate.height)
        assertEquals(1920, estimate.width)
        assertTrue(estimate.bytes > 0)
    }

    @Test fun rotationSwapsOutputOrientation() {
        val estimate = estimateVideoExport(1920, 1080, 10_000_000, 0f, 1_000f, 1f, VideoCropRect.Full, 90, 0, true)
        assertEquals(1080, estimate.width)
        assertEquals(1920, estimate.height)
    }
}
