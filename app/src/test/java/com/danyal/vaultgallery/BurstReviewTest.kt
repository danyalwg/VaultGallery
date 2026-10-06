package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Test

class BurstReviewTest {
    private fun signal(id: Long, time: Long, sharpness: Double, luma: Double = 118.0) =
        BurstSignal(id, 7L, 4000, 3000, time, sharpness, luma)

    @Test fun threeRapidFramesFormBurstAndSharpestIsRecommended() {
        val signals = listOf(signal(1, 0, 20.0), signal(2, 700, 80.0), signal(3, 1_400, 35.0))
        val groups = detectBurstGroups(signals)
        assertEquals(1, groups.size)
        assertEquals(2L, bestBurstSignal(groups.first()).id)
    }

    @Test fun separatedFramesDoNotFormBurst() {
        assertEquals(0, detectBurstGroups(listOf(signal(1, 0, 20.0), signal(2, 2_000, 80.0), signal(3, 4_000, 35.0))).size)
    }
}
