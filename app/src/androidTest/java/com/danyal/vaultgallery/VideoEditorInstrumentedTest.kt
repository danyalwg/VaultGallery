package com.danyal.vaultgallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.media.MediaExtractor
import android.media.MediaMetadataRetriever
import android.media.MediaFormat
import android.net.Uri
import android.provider.MediaStore
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.audio.SpeedProvider
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.io.File
import java.io.RandomAccessFile
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Exercises the real Media3 export pipeline used by the public and Secure video editors. */
@RunWith(AndroidJUnit4::class)
class VideoEditorInstrumentedTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context: Context get() = instrumentation.targetContext

    private fun auditVideo(): Uri {
        val local = File(context.cacheDir, "editor-audit-source.mp4")
        if (!local.isFile || local.length() < 1024L) {
            instrumentation.context.assets.open("editor-audit-video.mp4").use { input ->
                local.outputStream().use(input::copyTo)
            }
        }
        return Uri.fromFile(local)
    }

    private fun export(
        label: String,
        startMs: Long = 0L,
        endMs: Long = 6_000L,
        speed: Float = 1f,
        removeAudio: Boolean = false,
        effects: List<Effect> = emptyList(),
    ): Pair<File, ExportResult> {
        val output = File(context.cacheDir, "audit-$label-${System.nanoTime()}.mp4")
        val completed = CountDownLatch(1)
        val result = AtomicReference<ExportResult?>()
        val failure = AtomicReference<Throwable?>()
        instrumentation.runOnMainSync {
            val clipping = MediaItem.ClippingConfiguration.Builder()
                .setStartPositionMs(startMs)
                .setEndPositionMs(endMs)
                .build()
            val builder = EditedMediaItem.Builder(
                MediaItem.Builder().setUri(auditVideo()).setClippingConfiguration(clipping).build(),
            ).setRemoveAudio(shouldRemoveVideoAudio(removeAudio, speed))
                .setEffects(Effects(buildVideoAudioProcessors(removeAudio, speed), effects))
            if (speed != 1f) builder.setSpeed(object : SpeedProvider {
                override fun getSpeed(timeUs: Long): Float = speed
                override fun getNextSpeedChangeTimeUs(timeUs: Long): Long = androidx.media3.common.C.TIME_UNSET
            })
            Transformer.Builder(context).addListener(object : Transformer.Listener {
                override fun onCompleted(composition: androidx.media3.transformer.Composition, exportResult: ExportResult) {
                    result.set(exportResult)
                    completed.countDown()
                }

                override fun onError(composition: androidx.media3.transformer.Composition, exportResult: ExportResult, exception: ExportException) {
                    failure.set(exception)
                    completed.countDown()
                }
            }).build().start(builder.build(), output.absolutePath)
        }
        assertTrue("$label export timed out", completed.await(90, TimeUnit.SECONDS))
        failure.get()?.let { throw AssertionError("$label export failed", it) }
        assertTrue("$label produced no file", output.length() > 1024L)
        return output to requireNotNull(result.get())
    }

    private fun metadata(file: File, key: Int): String? = MediaMetadataRetriever().let { retriever ->
        try {
            retriever.setDataSource(file.absolutePath)
            retriever.extractMetadata(key)
        } finally {
            retriever.release()
        }
    }

    private fun displayedAspect(file: File): Float {
        val width = metadata(file, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 0f
        val height = metadata(file, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 0f
        val rotation = metadata(file, MediaMetadataRetriever.METADATA_KEY_VIDEO_ROTATION)?.toIntOrNull() ?: 0
        require(width > 0f && height > 0f)
        return if (rotation % 180 == 0) width / height else height / width
    }

    @Test
    fun trimAndAudioExport() {
        val (file, result) = export("trim", startMs = 1_000L, endMs = 4_000L)
        assertTrue(result.durationMs in 2_700L..3_300L)
        assertEquals("yes", metadata(file, MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
        file.delete()
    }

    @Test
    fun speedAndMuteExport() {
        val (file, result) = export("speed-mute", speed = 2f, removeAudio = true)
        assertTrue(result.durationMs in 2_700L..3_300L)
        // The speed+mute compatibility path intentionally retains a zero-gain track because
        // Media3 stalls on this device when speed-changing after removing audio entirely.
        assertEquals("yes", metadata(file, MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
        file.delete()
    }

    @Test
    fun filtersToneCropRotationAndSizeExport() {
        val sourceWidth = metadata(File(auditVideo().path!!), MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toFloatOrNull() ?: 16f
        val sourceHeight = metadata(File(auditVideo().path!!), MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toFloatOrNull() ?: 9f
        val effects = buildVideoEffects(
            rotation = 90,
            look = VideoLook.WARM,
            crop = centeredVideoCrop(sourceHeight / sourceWidth, 1f),
            brightness = .12f,
            contrast = .14f,
            saturation = 18f,
            outputHeight = 480,
        )
        val (file, result) = export("effects", effects = effects)
        assertNotNull(metadata(file, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
        assertNotNull(metadata(file, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
        assertTrue(result.width > 0 && result.height > 0)
        assertTrue("square crop encoded as ${result.width}×${result.height}", kotlin.math.abs(result.width - result.height) <= 4)
        file.delete()
    }

    @Test
    fun cropWindowChangesTheEncodedFrameGeometry() {
        val (baselineFile, baseline) = export("crop-baseline")
        val effects = buildVideoEffects(
            rotation = 0,
            look = VideoLook.ORIGINAL,
            crop = VideoCropRect(0f, 0f, .5f, 1f),
            brightness = 0f,
            contrast = 0f,
            saturation = 0f,
            outputHeight = 0,
        )
        val (file, result) = export("crop-window", effects = effects)
        val expectedAspect = displayedAspect(baselineFile) * .5f
        val encodedAspect = displayedAspect(file)
        assertTrue(
            "crop encoded ${result.width}×${result.height} with displayed aspect $encodedAspect; expected $expectedAspect",
            kotlin.math.abs(encodedAspect - expectedAspect) < .02f,
        )
        baselineFile.delete()
        file.delete()
    }

    @Test
    fun everyVideoFilterExportsThroughTheRealCodec() {
        VideoLook.entries.filterNot { it == VideoLook.ORIGINAL }.forEach { look ->
            val effects = buildVideoEffects(
                rotation = 0,
                look = look,
                crop = VideoCropRect.Full,
                brightness = 0f,
                contrast = 0f,
                saturation = 0f,
                outputHeight = 0,
            )
            val (file, result) = export("filter-${look.name.lowercase()}", endMs = 1_000L, effects = effects)
            assertTrue("${look.label} returned invalid dimensions", result.width > 0 && result.height > 0)
            file.delete()
        }
    }

    @Test
    fun slideshowQueuesEveryRequestedFrame() {
        val output = File(context.cacheDir, "audit-slideshow-${System.nanoTime()}.mp4")
        val red = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.RED) }
        val blue = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.BLUE) }
        try {
            RandomAccessFile(output, "rw").use { encodeSlideshowMovie(it.fd, listOf(red, blue), 320, 320, 15, 2) }
            val extractor = MediaExtractor()
            var samples = 0
            try {
                extractor.setDataSource(output.absolutePath)
                val videoTrack = (0 until extractor.trackCount).first {
                    extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME)?.startsWith("video/") == true
                }
                extractor.selectTrack(videoTrack)
                while (extractor.sampleTime >= 0L) {
                    samples++
                    if (!extractor.advance()) break
                }
            } finally { extractor.release() }
            assertEquals("two pictures × two seconds × 15 fps", 60, samples)
            val duration = metadata(output, MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            assertTrue("slideshow duration was $duration ms", duration in 3_900L..4_100L)
        } finally {
            red.recycle(); blue.recycle(); output.delete()
        }
    }

    @Test
    fun mixedMovieKeepsVideoFramesCrossfadeAndTitle() = runBlocking {
        val output = File(context.cacheDir, "audit-mixed-movie-${System.nanoTime()}.mp4")
        val still = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.MAGENTA) }
        try {
            val duration = RandomAccessFile(output, "rw").use {
                encodeMixedMovie(
                    context = context,
                    descriptor = it.fd,
                    sources = listOf(
                        CreativeMovieSource(still = still),
                        CreativeMovieSource(videoUri = auditVideo(), durationMs = 6_000L),
                    ),
                    width = 480,
                    height = 270,
                    fps = 15,
                    options = MovieOptions(
                        ratio = MovieRatio.LANDSCAPE,
                        height = 270,
                        secondsPerImage = 1,
                        maxVideoSeconds = 2,
                        transition = MovieTransition.CROSSFADE,
                        title = "Vault test",
                    ),
                )
            }
            assertTrue("mixed movie duration was $duration ms", duration in 3_300L..3_600L)
            assertEquals("480", metadata(output, MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH))
            assertEquals("270", metadata(output, MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT))
            val poster = decodeVideoPoster(context, Uri.fromFile(output), 320)
            assertNotNull(poster)
            poster?.recycle()
        } finally {
            still.recycle()
            output.delete()
        }
        Unit
    }

    @Test
    fun backgroundAudioIsMuxedAndClippedToMovieDuration() = runBlocking {
        val silent = File(context.cacheDir, "audit-silent-${System.nanoTime()}.mp4")
        val output = File(context.cacheDir, "audit-music-${System.nanoTime()}.mp4")
        val still = Bitmap.createBitmap(160, 120, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.CYAN) }
        try {
            val duration = RandomAccessFile(silent, "rw").use {
                encodeMixedMovie(
                    context,
                    it.fd,
                    listOf(CreativeMovieSource(still = still)),
                    width = 320,
                    height = 240,
                    fps = 15,
                    options = MovieOptions(secondsPerImage = 2, transition = MovieTransition.NONE),
                )
            }
            RandomAccessFile(output, "rw").use {
                muxMovieWithBackgroundAudio(context, silent, auditVideo(), it.fd, duration)
            }
            assertEquals("yes", metadata(output, MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO))
            assertEquals("yes", metadata(output, MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO))
            val measured = metadata(output, MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            assertTrue("music movie duration was $measured ms", measured in 1_850L..2_150L)
        } finally {
            still.recycle()
            silent.delete()
            output.delete()
        }
        Unit
    }
}
