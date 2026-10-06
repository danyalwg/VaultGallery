package com.danyal.vaultgallery

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.PowerManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlin.coroutines.coroutineContext

internal enum class AiBackend(val label: String) {
    MEDIAPIPE("MediaPipe"),
    ML_KIT("ML Kit"),
    ONNX("ONNX Runtime"),
    OPENCV("OpenCV"),
    LOCAL_FILTER("Local image pipeline"),
}

internal data class AiOperationSpec(
    val id: String,
    val backend: AiBackend,
    val memoryMultiplier: Int = 4,
)

/** Shared cancellation, thermal and memory gate for every bitmap intelligence operation. */
internal class AiExecutionRuntime(context: Context) {
    private val power = context.applicationContext.getSystemService(PowerManager::class.java)

    suspend fun run(
        spec: AiOperationSpec,
        source: Bitmap,
        operation: suspend (Bitmap) -> Bitmap,
    ): Bitmap = withContext(Dispatchers.Default) {
        coroutineContext.ensureActive()
        checkThermal(spec)
        checkMemory(spec, source)
        val result = operation(source)
        try {
            coroutineContext.ensureActive()
            result
        } catch (cancelled: Throwable) {
            if (result !== source && !result.isRecycled) result.recycle()
            throw cancelled
        }
    }

    private fun checkThermal(spec: AiOperationSpec) {
        if (Build.VERSION.SDK_INT >= 29 && power.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE) {
            error("${spec.backend.label} processing is paused because the phone is too hot. Let it cool, then try again.")
        }
    }

    private fun checkMemory(spec: AiOperationSpec, source: Bitmap) {
        val runtime = Runtime.getRuntime()
        val used = runtime.totalMemory() - runtime.freeMemory()
        val available = (runtime.maxMemory() - used).coerceAtLeast(0L)
        val required = source.allocationByteCount.toLong().coerceAtLeast(source.width.toLong() * source.height * 4L) * spec.memoryMultiplier
        val safety = 48L * 1024 * 1024
        require(available >= required + safety) {
            "${spec.backend.label} needs about ${memoryLabel(required + safety)} free memory for this ${source.width}×${source.height} image. Close other apps or use a smaller copy."
        }
    }
}

private fun memoryLabel(bytes: Long): String = if (bytes >= 1024L * 1024 * 1024) {
    "%.1f GB".format(java.util.Locale.ROOT, bytes.toDouble() / (1024L * 1024 * 1024))
} else {
    "%.0f MB".format(java.util.Locale.ROOT, bytes.toDouble() / (1024L * 1024))
}
