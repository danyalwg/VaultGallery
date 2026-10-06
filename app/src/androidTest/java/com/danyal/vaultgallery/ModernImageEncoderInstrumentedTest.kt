package com.danyal.vaultgallery

import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.RandomAccessFile

@RunWith(AndroidJUnit4::class)
class ModernImageEncoderInstrumentedTest {
    @Test
    fun pixelEncodersProduceRealIsoMediaImages() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xffd02c62.toInt())
        }
        try {
            listOf(PhotoOutputFormat.HEIC, PhotoOutputFormat.AVIF).forEach { format ->
                if (!ModernImageEncoder.isSupported(format)) {
                    println("DEVICE_ENCODER_UNAVAILABLE=${format.label}")
                    return@forEach
                }
                val file = File(context.cacheDir, "encoder-proof.${format.extension}")
                RandomAccessFile(file, "rw").use { target ->
                    target.setLength(0)
                    ModernImageEncoder.encode(bitmap, format, 90, target.fd)
                    target.fd.sync()
                }
                val bytes = file.readBytes()
                assertTrue("${format.label} output is unexpectedly small", bytes.size > 64)
                assertTrue("${format.label} output is not ISO base media", bytes.copyOfRange(4, 8).decodeToString() == "ftyp")
                println("DEVICE_ENCODER_OK=${format.label}:${bytes.size}")
                file.delete()
            }
        } finally {
            bitmap.recycle()
        }
    }
}
