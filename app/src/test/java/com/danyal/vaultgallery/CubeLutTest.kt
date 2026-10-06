package com.danyal.vaultgallery

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.StringReader

class CubeLutTest {
    private val identity2 = """
        TITLE "Identity"
        LUT_3D_SIZE 2
        0 0 0
        1 0 0
        0 1 0
        1 1 0
        0 0 1
        1 0 1
        0 1 1
        1 1 1
    """.trimIndent()

    @Test fun parsesStandardCubeAndSamplesTrilinearly() {
        val lut = parseCubeLut(StringReader(identity2))
        assertEquals("Identity", lut.title)
        assertEquals(2, lut.size)
        assertArrayEquals(floatArrayOf(.25f, .5f, .75f), sampleCubeLut(lut, .25f, .5f, .75f), .0001f)
    }
}
