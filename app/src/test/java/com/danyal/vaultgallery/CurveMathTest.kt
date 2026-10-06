package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurveMathTest {
    @Test fun identityCurvePreservesLevels() {
        val lut = curveLut(listOf(CurvePoint(0f, 0f), CurvePoint(.5f, .5f), CurvePoint(1f, 1f)))
        assertEquals(0, lut[0])
        assertTrue(kotlin.math.abs(lut[128] - 128) <= 1)
        assertEquals(255, lut[255])
    }

    @Test fun pointsAreSortedClampedAndInterpolated() {
        val normalized = normalizedCurve(listOf(CurvePoint(1f, 1.3f), CurvePoint(.75f, .9f), CurvePoint(-1f, -.2f), CurvePoint(.25f, .1f)))
        assertEquals(0f, normalized.first().x)
        assertEquals(1f, normalized.last().x)
        assertTrue(normalized.zipWithNext().all { (a, b) -> a.x < b.x })
        val lut = curveLut(normalized)
        assertTrue(lut[64] < 64)
        assertTrue(lut[192] > 192)
    }
}
