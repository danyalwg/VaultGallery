package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.math.abs
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NeuralInpaintingInstrumentedTest {
    @Test
    fun edgeAwareMaskPreservesProbabilityInAlphaChannel() {
        val size = 320
        val source = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val pixels = IntArray(size * size) { index ->
                val x = index % size
                if (x < size / 2) Color.rgb(30, 30, 35) else Color.rgb(230, 225, 215)
            }
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        }
        val semantic = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            val pixels = IntArray(size * size) { index ->
                val x = index % size
                val probability = ((x - 80) / 160f).coerceIn(0f, 1f)
                Color.argb((probability * 255).toInt(), 255, 255, 255)
            }
            bitmap.setPixels(pixels, 0, size, 0, 0, size, size)
        }
        org.opencv.android.OpenCVLoader.initLocal()
        val refined = edgeAwareSelectionMask(source, semantic)
        assertTrue(Color.alpha(refined.getPixel(40, 160)) < 40)
        assertTrue(Color.alpha(refined.getPixel(280, 160)) > 210)
        assertTrue("the mask contract must store confidence in alpha", Color.alpha(refined.getPixel(170, 160)) in 1..254)
        refined.recycle(); semantic.recycle(); source.recycle()
    }

    @Test
    fun lamaRebuildsSelectionAndLeavesOutsidePixelsUntouched() = runBlocking {
        val size = 512
        val sourcePixels = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            if (x in 205..306 && y in 205..306) Color.rgb(8, 8, 8)
            else Color.rgb(60 + x * 130 / size, 90 + y * 110 / size, 145)
        }
        val maskPixels = IntArray(size * size) { index ->
            val x = index % size
            val y = index / size
            if (x in 200..311 && y in 200..311) Color.WHITE else Color.TRANSPARENT
        }
        val source = Bitmap.createBitmap(sourcePixels, size, size, Bitmap.Config.ARGB_8888)
        val mask = Bitmap.createBitmap(maskPixels, size, size, Bitmap.Config.ARGB_8888)
        val output = aiSelectionErase(ApplicationProvider.getApplicationContext(), source, mask)

        assertEquals("a distant unselected pixel must be byte-identical", source.getPixel(20, 20), output.getPixel(20, 20))
        val before = source.getPixel(256, 256)
        val after = output.getPixel(256, 256)
        val difference = abs(Color.red(before) - Color.red(after)) +
            abs(Color.green(before) - Color.green(after)) + abs(Color.blue(before) - Color.blue(after))
        assertTrue("LaMa should synthesize the masked centre", difference > 24)

        output.recycle(); source.recycle(); mask.recycle()
    }
}
