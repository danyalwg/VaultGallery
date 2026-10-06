package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LensCorrectionInstrumentedTest {
    @Test
    fun neutralCorrectionIsPixelExact() {
        val source = Bitmap.createBitmap(5, 5, Bitmap.Config.ARGB_8888)
        for (y in 0 until 5) for (x in 0 until 5) source.setPixel(x, y, Color.rgb(x * 40, y * 40, (x + y) * 20))
        val output = renderLensCorrection(source, LensCorrectionParameters())
        for (y in 0 until 5) for (x in 0 until 5) assertEquals(source.getPixel(x, y), output.getPixel(x, y))
    }

    @Test
    fun radialCorrectionChangesEdgesButKeepsOpticalCentreStable() {
        val source = Bitmap.createBitmap(9, 9, Bitmap.Config.ARGB_8888)
        for (y in 0 until 9) for (x in 0 until 9) source.setPixel(x, y, Color.rgb(x * 20, y * 20, 80))
        val output = renderLensCorrection(source, LensCorrectionParameters(distortion = .35f))
        assertEquals(source.getPixel(4, 4), output.getPixel(4, 4))
        assertNotEquals(source.getPixel(1, 1), output.getPixel(1, 1))
    }
}
