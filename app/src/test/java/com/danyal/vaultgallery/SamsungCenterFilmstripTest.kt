package com.danyal.vaultgallery

import org.junit.Assert.assertEquals
import org.junit.Test

class SamsungCenterFilmstripTest {
    @Test
    fun portraitMediaNeverBecomesMinuscule() {
        assertEquals(52, filmstripCompactWidthDp(1080, 2400, selected = false))
        assertEquals(63, filmstripCompactWidthDp(1080, 2400, selected = true))
    }

    @Test
    fun landscapeMediaRetainsUsefulAspectCueWithinBoundedWidth() {
        assertEquals(64, filmstripCompactWidthDp(1920, 1080, selected = false))
        assertEquals(78, filmstripCompactWidthDp(1920, 1080, selected = true))
        assertEquals(72, filmstripCompactWidthDp(4000, 1000, selected = false))
        assertEquals(88, filmstripCompactWidthDp(4000, 1000, selected = true))
    }
}
