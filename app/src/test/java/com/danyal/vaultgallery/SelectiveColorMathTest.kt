package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectiveColorMathTest {
    @Test fun hueRangeWrapsAcrossRedBoundary() {
        assertEquals(1f, selectiveHueWeight(359f, SelectiveHue.RED.center), .001f)
        assertEquals(1f, selectiveHueWeight(1f, SelectiveHue.RED.center), .001f)
    }

    @Test fun distantHueIsNotSelected() {
        assertTrue(selectiveHueWeight(180f, SelectiveHue.RED.center) == 0f)
    }
}
