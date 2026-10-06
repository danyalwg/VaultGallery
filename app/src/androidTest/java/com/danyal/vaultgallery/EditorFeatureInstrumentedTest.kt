package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.ColorMatrix
import android.net.Uri
import android.os.SystemClock
import android.content.ContentValues
import android.provider.MediaStore
import android.view.MotionEvent
import androidx.exifinterface.media.ExifInterface
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Device-level smoke tests for every full-resolution photo processor exposed by the editors. */
@RunWith(AndroidJUnit4::class)
class EditorFeatureInstrumentedTest {
    private fun source(): Bitmap = Bitmap.createBitmap(192, 144, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        for (y in 0 until bitmap.height) {
            paint.color = Color.rgb(35 + y, 70 + y / 2, 170 - y / 2)
            canvas.drawLine(0f, y.toFloat(), bitmap.width.toFloat(), y.toFloat(), paint)
        }
        paint.color = Color.rgb(238, 184, 145)
        canvas.drawOval(64f, 20f, 130f, 104f, paint)
        paint.color = Color.WHITE
        paint.textSize = 24f
        canvas.drawText("Vault 42", 35f, 132f, paint)
    }

    private fun mask(): Bitmap = Bitmap.createBitmap(192, 144, Bitmap.Config.ARGB_8888).also { bitmap ->
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawCircle(96f, 68f, 45f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE })
    }

    private fun verify(
        name: String,
        original: Bitmap,
        result: Bitmap,
        allowResize: Boolean = false,
        allowUnchanged: Boolean = false,
    ) {
        assertFalse("$name returned a recycled bitmap", result.isRecycled)
        assertTrue("$name returned an empty width", result.width > 0)
        assertTrue("$name returned an empty height", result.height > 0)
        if (!allowResize) {
            assertTrue("$name changed width unexpectedly", result.width == original.width)
            assertTrue("$name changed height unexpectedly", result.height == original.height)
            val originalPixels = IntArray(original.width * original.height)
            val resultPixels = IntArray(result.width * result.height)
            original.getPixels(originalPixels, 0, original.width, 0, 0, original.width, original.height)
            result.getPixels(resultPixels, 0, result.width, 0, 0, result.width, result.height)
            val changed = originalPixels.indices.count { originalPixels[it] != resultPixels[it] }
            if (!allowUnchanged) {
                assertTrue("$name returned an unchanged image", changed > originalPixels.size / 1_000)
            }
        }
        if (result !== original) result.recycle()
    }

    @Test
    fun enhancementAndRestorationToolsRender() = runBlocking {
        val input = source()
        val tools = listOf<Pair<String, suspend (Bitmap) -> Bitmap>>(
            "Auto enhance" to ::autoEnhancePhoto,
            "Low-light rescue" to ::recoverLowLightPhoto,
            "Denoise" to ::denoisePhoto,
            "Clarity" to ::improveClarityPhoto,
            "White balance" to ::whiteBalancePhoto,
            "Soft glow" to ::softGlowPhoto,
            "Smart sharpen" to ::smartSharpenPhoto,
            "Lift shadows" to ::liftShadowsPhoto,
            "Recover highlights" to ::recoverHighlightsPhoto,
            "Dehaze" to ::dehazePhoto,
            "HDR detail" to ::hdrDetailPhoto,
            "Restore faded" to ::restoreFadedPhoto,
        )
        tools.forEach { (name, operation) -> verify(name, input, operation(input)) }
        verify("2x detail", input, upscalePhoto(input), allowResize = true)
        input.recycle()
    }

    @Test
    fun documentAndCreativeToolsRender() = runBlocking {
        val input = source()
        val tools = listOf<Pair<String, suspend (Bitmap) -> Bitmap>>(
            "Clean B&W" to ::cleanDocumentPhoto,
            "Clean color" to ::cleanColorDocumentPhoto,
            "Remove shadows" to ::removeDocumentShadowsPhoto,
            "Ink boost" to ::inkBoostDocumentPhoto,
            "Grayscale" to ::grayscaleDocumentPhoto,
            "Cinematic" to ::cinematicPhoto,
            "Matte film" to ::mattePhoto,
            "Noir" to ::noirPhoto,
            "Graphic novel" to ::comicPhoto,
            "Pencil sketch" to ::pencilSketchPhoto,
            "Watercolor" to ::watercolorPhoto,
            "Pixel art" to ::pixelatePhoto,
        )
        tools.forEach { (name, operation) -> verify(name, input, operation(input)) }
        input.recycle()
    }

    @Test
    fun geometryAndSelectionToolsRender() = runBlocking {
        val input = source()
        verify("Straighten", input, straightenPhoto(input, 8f))
        verify("Horizontal perspective", input, perspectivePhoto(input, horizontal = .18f))
        verify("Vertical perspective", input, perspectivePhoto(input, vertical = -.18f))
        val selection = mask()
        val tools = listOf<Pair<String, suspend (Bitmap) -> Bitmap>>(
            "Selection background blur" to { aiSelectionBackgroundBlur(it, selection) },
            "Selection color pop" to { aiSelectionColorPop(it, selection) },
            "Selection relight" to { aiSelectionRelight(it, selection) },
            "Selection studio backdrop" to { aiSelectionStudioBackdrop(it, selection) },
            "Selection sticker" to { aiSelectionSticker(it, selection) },
            "Object eraser" to { aiSelectionErase(it, selection) },
        )
        tools.forEach { (name, operation) -> verify(name, input, operation(input)) }
        selection.recycle()
        input.recycle()
    }

    @Test
    fun portraitToolsRender() = runBlocking {
        val input = source()
        val tools = listOf<Pair<String, suspend (Bitmap) -> Bitmap>>(
            "Portrait blur" to ::aiPortraitBackgroundBlur,
            "Studio light" to ::aiPortraitSpotlight,
            "Color focus" to ::aiPortraitColorPop,
            "Clean backdrop" to ::aiPortraitStudioBackground,
            "Natural skin" to ::aiPortraitSmoothPhoto,
            "Warm subject" to ::aiPortraitWarmPhoto,
            "Night portrait" to ::aiPortraitNightPhoto,
        )
        tools.forEach { (name, operation) ->
            // Subject-only skin smoothing deliberately leaves an image untouched when the person
            // detector finds no person. This fixture is a geometric drawing, not a real portrait.
            verify(name, input, operation(input), allowUnchanged = name in setOf("Natural skin", "Warm subject"))
        }
        input.recycle()
    }

    @Test
    fun mosaicBrushPaintsAndExportsAtSourceResolution() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val input = source()
        lateinit var result: Bitmap
        instrumentation.runOnMainSync {
            val view = MosaicBrushView(instrumentation.targetContext)
            view.layout(0, 0, input.width, input.height)
            view.setSource(input)
            val downTime = SystemClock.uptimeMillis()
            view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN, 50f, 50f, 0))
            view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 20, MotionEvent.ACTION_MOVE, 145f, 95f, 0))
            view.dispatchTouchEvent(MotionEvent.obtain(downTime, downTime + 40, MotionEvent.ACTION_UP, 145f, 95f, 0))
            result = view.render()
        }
        verify("Mosaic brush", input, result)
        input.recycle()
    }

    @Test
    fun editorDecodeDoesNotSilentlyApplyTheFormer8192PixelCap() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val source = Bitmap.createBitmap(8_205, 16, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        val file = File(context.cacheDir, "wide-full-resolution-${System.nanoTime()}.png")
        try {
            FileOutputStream(file).use { check(source.compress(Bitmap.CompressFormat.PNG, 100, it)) }
            val decoded = decodeBitmapForEditing(context, Uri.fromFile(file))
            assertEquals(8_205, decoded?.width)
            assertEquals(16, decoded?.height)
            decoded?.recycle()
        } finally {
            source.recycle()
            file.delete()
        }
    }

    @Test
    fun fullResolutionFilterRendererIsSynchronousAndPreservesDimensions() {
        val input = source()
        val warm = ColorMatrix(floatArrayOf(
            1.08f, 0f, 0f, 0f, 7f,
            0f, 1.01f, 0f, 0f, 0f,
            0f, 0f, .88f, 0f, -4f,
            0f, 0f, 0f, 1f, 0f,
        ))
        val output = renderPhotoColorMatrix(input, warm)
        verify("Warm filter", input, output)
        input.recycle()
    }

    @Test
    fun editedCopyUsesTruthfulFormatAndPreservesCaptureMetadata() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val resolver = context.contentResolver
        val timestamp = 1_700_000_123_000L
        val sourceUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "audit-source-${System.nanoTime()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Vault Gallery Tests")
            put(MediaStore.Images.Media.DATE_TAKEN, timestamp)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }) ?: error("Could not create metadata test source")
        var outputUri: Uri? = null
        val bitmap = source()
        try {
            resolver.openOutputStream(sourceUri, "w")!!.use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)) }
            resolver.openFileDescriptor(sourceUri, "rw")!!.use { descriptor ->
                ExifInterface(descriptor.fileDescriptor).apply {
                    setAttribute(ExifInterface.TAG_MAKE, "Vault Test Camera")
                    setLatLong(24.8607, 67.0011)
                    saveAttributes()
                }
            }
            resolver.update(sourceUri, ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }, null, null)
            outputUri = saveEditedBitmap(
                context,
                bitmap,
                GalleryMedia(
                    id = -10L,
                    uri = sourceUri,
                    name = "legacy-capture.heic",
                    mimeType = "image/heic",
                    kind = MediaKind.IMAGE,
                    width = bitmap.width,
                    height = bitmap.height,
                    durationMs = 0L,
                    sizeBytes = 0L,
                    dateTakenMs = timestamp,
                    bucketId = -10L,
                    bucketName = "Vault Gallery Tests",
                    isFavourite = false,
                    relativePath = "Pictures/Vault Gallery Tests/",
                ),
            )
            resolver.query(outputUri, arrayOf(MediaStore.Images.Media.DISPLAY_NAME, MediaStore.Images.Media.MIME_TYPE, MediaStore.Images.Media.DATE_ADDED), null, null, null)!!.use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertTrue(cursor.getString(0).endsWith(".jpg"))
                assertEquals("image/jpeg", cursor.getString(1))
                assertEquals(timestamp / 1000L, cursor.getLong(2))
            }
            resolver.openFileDescriptor(outputUri, "r")!!.use { descriptor ->
                val exif = ExifInterface(descriptor.fileDescriptor)
                assertEquals("Vault Test Camera", exif.getAttribute(ExifInterface.TAG_MAKE))
                assertNotNull(exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL))
                assertNotNull(exif.latLong)
            }
            assertFalse(canSafelyOverwritePhoto("image/heic"))
        } finally {
            outputUri?.let { resolver.delete(it, null, null) }
            resolver.delete(sourceUri, null, null)
            bitmap.recycle()
        }
    }
}
