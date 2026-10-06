package com.danyal.vaultgallery.editor

import java.util.UUID
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface PhotoOperation {
    @Serializable @SerialName("crop")
    data class Crop(val left: Float, val top: Float, val right: Float, val bottom: Float, val rotation: Float = 0f) : PhotoOperation
    @Serializable @SerialName("tone")
    data class Tone(val exposure: Float = 0f, val contrast: Float = 0f, val highlights: Float = 0f, val shadows: Float = 0f) : PhotoOperation
    @Serializable @SerialName("adjustment")
    data class Adjustment(
        val filter: String = "Original",
        val lightBalance: Float = 0f,
        val brightness: Float = 0f,
        val exposure: Float = 0f,
        val contrast: Float = 1f,
        val highlights: Float = 0f,
        val shadows: Float = 0f,
        val blackPoint: Float = 0f,
        val whitePoint: Float = 255f,
        val saturation: Float = 1f,
        val tint: Float = 0f,
        val temperature: Float = 0f,
        val redGain: Float = 1f,
        val greenGain: Float = 1f,
        val blueGain: Float = 1f,
    ) : PhotoOperation
    @Serializable @SerialName("hsl")
    data class Hsl(val hue: Float = 0f, val saturation: Float = 0f, val luminance: Float = 0f) : PhotoOperation
    @Serializable @SerialName("masked")
    data class Masked(val maskId: String, val feather: Float, val inverted: Boolean, val operation: PhotoOperation) : PhotoOperation
    @Serializable @SerialName("lut")
    data class Lut(val uri: String, val strength: Float) : PhotoOperation
}

@Serializable
data class VectorLayer(
    val id: String = UUID.randomUUID().toString(),
    val kind: String,
    val payload: String,
    val visible: Boolean = true,
    val locked: Boolean = false,
    val opacity: Float = 1f,
    val blendMode: String = "normal",
)

@Serializable
data class PhotoEditProject(
    val id: String = UUID.randomUUID().toString(),
    val source: String,
    val operations: List<PhotoOperation> = emptyList(),
    val layers: List<VectorLayer> = emptyList(),
    val cursor: Int = operations.size,
) {
    fun append(operation: PhotoOperation) = copy(operations = operations.take(cursor) + operation, cursor = cursor + 1)
    fun undo() = copy(cursor = (cursor - 1).coerceAtLeast(0))
    fun redo() = copy(cursor = (cursor + 1).coerceAtMost(operations.size))
    val activeOperations get() = operations.take(cursor)
}

@Serializable
data class Keyframe(val timeMs: Long, val value: Float)
@Serializable
data class TransformKeyframes(
    val positionX: List<Keyframe> = emptyList(), val positionY: List<Keyframe> = emptyList(),
    val scale: List<Keyframe> = emptyList(), val rotation: List<Keyframe> = emptyList(),
    val opacity: List<Keyframe> = emptyList(), val effectStrength: List<Keyframe> = emptyList(),
)
@Serializable
enum class VideoTrackKind { VIDEO, IMAGE, AUDIO, TEXT, STICKER, CAPTION, VOICEOVER }
@Serializable
data class VideoClip(val id: String, val uri: String, val startMs: Long, val durationMs: Long, val sourceOffsetMs: Long = 0, val keyframes: TransformKeyframes = TransformKeyframes())
@Serializable
data class VideoTrack(val id: String, val kind: VideoTrackKind, val clips: List<VideoClip>, val muted: Boolean = false, val locked: Boolean = false)
@Serializable
data class VideoEditProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tracks: List<VideoTrack>,
    val width: Int,
    val height: Int,
    val fps: Int,
    val hdrMode: String = "preserve",
    val editorState: VideoProjectState = VideoProjectState(),
    val subtitles: List<SubtitleCue> = emptyList(),
)

@Serializable
data class VideoProjectState(
    val trimStartMs: Long = 0,
    val trimEndMs: Long = 0,
    val speed: Float = 1f,
    val muted: Boolean = false,
    val rotation: Int = 0,
    val look: String = "Original",
    val frame: String = "Free",
    val cropLeft: Float = 0f,
    val cropTop: Float = 0f,
    val cropRight: Float = 1f,
    val cropBottom: Float = 1f,
    val brightness: Float = 0f,
    val contrast: Float = 0f,
    val saturation: Float = 0f,
    val outputHeight: Int = 0,
    val hdrMode: String = "preserve",
)
