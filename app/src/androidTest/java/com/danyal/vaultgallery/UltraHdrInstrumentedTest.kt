package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Gainmap
import android.graphics.ImageDecoder
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer

@RunWith(AndroidJUnit4::class)
class UltraHdrInstrumentedTest {
    @Test
    fun editorSurfaceAndJpegRetainGainmap() {
        assumeTrue(Build.VERSION.SDK_INT >= 34)
        val source = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GRAY) }
        val gainContents = Bitmap.createBitmap(16, 12, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        source.gainmap = Gainmap(gainContents)
        val edited = createEditBitmapLike(source)
        assertTrue(edited.hasGainmap())
        val encoded = ByteArrayOutputStream().also { output -> assertTrue(edited.compress(Bitmap.CompressFormat.JPEG, 95, output)) }.toByteArray()
        val decoded = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(encoded)))
        assertTrue("JPEG encoder dropped the Ultra HDR gain map", decoded.hasGainmap())
        decoded.recycle(); edited.recycle(); source.recycle(); gainContents.recycle()
    }
}
