@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.audio.DefaultGainProvider
import androidx.media3.common.audio.GainProcessor
import androidx.media3.effect.Presentation
import androidx.media3.transformer.Composition
import androidx.media3.transformer.EditedMediaItem
import androidx.media3.transformer.EditedMediaItemSequence
import androidx.media3.transformer.Effects
import androidx.media3.transformer.ExportException
import androidx.media3.transformer.ExportResult
import androidx.media3.transformer.Transformer
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.data.MediaKind
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

internal data class MovieCompositionPlan(
    val composition: Composition,
    val durationMs: Long,
    val width: Int,
    val height: Int,
)

/**
 * Builds the single immutable Media3 object used as the source of truth for movie preview/export.
 * A future UI surface can hand [composition] directly to CompositionPlayer without translating
 * editor state into a second representation.
 */
internal fun buildMovieComposition(
    media: List<GalleryMedia>,
    options: MovieOptions,
): MovieCompositionPlan {
    require(media.isNotEmpty()) { "No media was selected" }
    val height = options.height.coerceIn(360, 1080)
    val width = ((height.toFloat() * options.ratio.widthScale / options.ratio.heightScale).toInt() / 2 * 2)
        .coerceAtLeast(2)
    val presentation = Presentation.createForWidthAndHeight(
        width,
        height,
        Presentation.LAYOUT_SCALE_TO_FIT_WITH_CROP,
    )
    val visualEffects = Effects(emptyList(), listOf(presentation))
    var durationMs = 0L
    val visualItems = media.take(32).map { source ->
        val itemDuration = if (source.kind == MediaKind.VIDEO) {
            source.durationMs.coerceAtLeast(1L).coerceAtMost(options.maxVideoSeconds.coerceIn(2, 30) * 1_000L)
        } else {
            options.secondsPerImage.coerceIn(1, 5) * 1_000L
        }
        durationMs += itemDuration
        val mediaItem = MediaItem.Builder().setUri(source.uri).apply {
            if (source.kind == MediaKind.IMAGE) setImageDurationMs(itemDuration)
            else setClippingConfiguration(
                MediaItem.ClippingConfiguration.Builder().setStartPositionMs(0).setEndPositionMs(itemDuration).build(),
            )
        }.build()
        EditedMediaItem.Builder(mediaItem)
            .setDurationUs(itemDuration * 1_000L)
            .setFrameRate(30)
            .setEffects(visualEffects)
            .build()
    }
    val sequences = mutableListOf(EditedMediaItemSequence.withAudioAndVideoFrom(visualItems))
    options.backgroundAudioUri?.let { audioUri ->
        val baseGain = if (options.voiceoverUri != null && options.duckMusicForVoiceover) .25f else .78f
        val provider = DefaultGainProvider.Builder(baseGain).apply {
            if (options.fadeAudio) {
                val fadeUs = 800_000L.coerceAtMost(durationMs * 500L)
                addFadeAt(0L, fadeUs) { positionUs, durationUs -> positionUs.toFloat() / durationUs.coerceAtLeast(1L) }
                addFadeAt((durationMs * 1_000L - fadeUs).coerceAtLeast(0L), fadeUs) { positionUs, durationUs ->
                    1f - positionUs.toFloat() / durationUs.coerceAtLeast(1L)
                }
            }
        }.build()
        val audio = EditedMediaItem.Builder(MediaItem.fromUri(audioUri))
            .setRemoveVideo(true)
            .setEffects(Effects(listOf(GainProcessor(provider)), emptyList()))
            .build()
        sequences += EditedMediaItemSequence.Builder(listOf(audio)).setIsLooping(true).build()
    }
    options.voiceoverUri?.let { voiceoverUri ->
        val voiceover = EditedMediaItem.Builder(MediaItem.fromUri(voiceoverUri))
            .setRemoveVideo(true)
            .build()
        sequences += EditedMediaItemSequence.Builder(listOf(voiceover)).build()
    }
    val composition = Composition.Builder(sequences)
        .setHdrMode(Composition.HDR_MODE_KEEP_HDR)
        .experimentalSetRetainHdrFromUltraHdrImage(true)
        .build()
    return MovieCompositionPlan(composition, durationMs, width, height)
}

internal suspend fun exportMovieComposition(
    context: Context,
    plan: MovieCompositionPlan,
    output: File,
): ExportResult = withContext(Dispatchers.Main.immediate) {
    suspendCancellableCoroutine { continuation ->
        output.parentFile?.mkdirs()
        output.delete()
        lateinit var transformer: Transformer
        transformer = Transformer.Builder(context)
            .setVideoMimeType(MimeTypes.VIDEO_H264)
            .setAudioMimeType(MimeTypes.AUDIO_AAC)
            .setPortraitEncodingEnabled(true)
            .addListener(object : Transformer.Listener {
                override fun onCompleted(composition: Composition, result: ExportResult) {
                    if (continuation.isActive) continuation.resume(result)
                }

                override fun onError(composition: Composition, result: ExportResult, exception: ExportException) {
                    output.delete()
                    if (continuation.isActive) continuation.resumeWithException(exception)
                }
            })
            .build()
        continuation.invokeOnCancellation {
            transformer.cancel()
            output.delete()
        }
        transformer.start(plan.composition, output.absolutePath)
    }
}
