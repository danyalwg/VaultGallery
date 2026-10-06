package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.sqrt

@RunWith(AndroidJUnit4::class)
class VisualSimilarityInstrumentedTest {
    @Test
    fun descriptorIsNormalizedAndStableForCopies() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) for (x in 0 until 64) image.setPixel(x, y, Color.rgb(x * 4, y * 4, 128))
        val first = visualDescriptor(image)
        val second = visualDescriptor(image.copy(Bitmap.Config.ARGB_8888, false))
        assertEquals(176, first.size)
        assertEquals(1f, sqrt(first.sumOf { (it * it).toDouble() }).toFloat(), .0001f)
        first.indices.forEach { assertEquals(first[it], second[it], .000001f) }
    }

    @Test
    fun differentLayoutsProduceDifferentSpatialDescriptors() {
        val horizontal = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        val vertical = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        for (y in 0 until 32) for (x in 0 until 32) {
            horizontal.setPixel(x, y, if (y < 16) Color.WHITE else Color.BLACK)
            vertical.setPixel(x, y, if (x < 16) Color.WHITE else Color.BLACK)
        }
        val a = visualDescriptor(horizontal)
        val b = visualDescriptor(vertical)
        val cosine = a.indices.sumOf { (a[it] * b[it]).toDouble() }.toFloat()
        assertTrue(cosine < .98f)
    }
}
