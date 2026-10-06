package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VectorLayerEditorInstrumentedTest {
    @Test
    fun renderingIsNonDestructiveAndAppliesVisibleLayers() {
        val source = Bitmap.createBitmap(320, 240, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLACK) }
        val output = renderVectorScene(
            source,
            listOf(EditableVectorLayer(kind = VectorLayerKind.TEXT, content = "TEST", color = Color.WHITE, scale = 1.4f)),
        )
        assertEquals(Color.BLACK, source.getPixel(160, 120))
        var changed = false
        for (y in 60 until 180 step 4) for (x in 50 until 270 step 4) {
            if (output.getPixel(x, y) != Color.BLACK) changed = true
        }
        assertNotEquals(false, changed)
    }

    @Test
    fun hiddenLayerDoesNotAffectOutput() {
        val source = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        val output = renderVectorScene(source, listOf(EditableVectorLayer(kind = VectorLayerKind.OVAL, visible = false)))
        assertEquals(Color.BLUE, output.getPixel(32, 32))
    }
}
