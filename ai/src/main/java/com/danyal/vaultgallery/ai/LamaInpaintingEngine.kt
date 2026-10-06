package com.danyal.vaultgallery.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.channels.FileChannel
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Fixed-resolution Big-LaMa ONNX runner used by object erase/content-aware fill.
 *
 * The model is memory-mapped directly from the uncompressed APK asset, so opening the editor does
 * not duplicate a 198 MB graph into app storage. Inference is fully local and the original pixels
 * outside the requested mask are composited back verbatim.
 */
class LamaInpaintingEngine(
    context: Context,
    assetName: String = MODEL_ASSET,
) : AutoCloseable {
    private val environment = OrtEnvironment.getEnvironment("VaultGallery-LaMa")
    private val asset = context.applicationContext.assets.openFd(assetName)
    private val stream = FileInputStream(asset.fileDescriptor)
    private val mappedModel: ByteBuffer = stream.channel.map(
        FileChannel.MapMode.READ_ONLY,
        asset.startOffset,
        asset.length,
    )
    private val session = createSession(mappedModel)
    private val inferenceMutex = Mutex()

    suspend fun inpaint(source: Bitmap, mask: Bitmap): Bitmap = withContext(Dispatchers.Default) {
        inferenceMutex.withLock {
        require(source.width > 1 && source.height > 1 && !source.isRecycled) { "The source image is unavailable" }
        require(mask.width > 1 && mask.height > 1 && !mask.isRecycled) { "The selection mask is unavailable" }
        coroutineContext.ensureActive()

        val modelImage = Bitmap.createBitmap(MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
        val modelMask = Bitmap.createBitmap(MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
        Canvas(modelImage).drawBitmap(source, null, Rect(0, 0, MODEL_SIZE, MODEL_SIZE), Paint(Paint.FILTER_BITMAP_FLAG))
        Canvas(modelMask).drawBitmap(mask, null, Rect(0, 0, MODEL_SIZE, MODEL_SIZE), Paint(Paint.FILTER_BITMAP_FLAG))

        val pixels = IntArray(MODEL_SIZE * MODEL_SIZE)
        val maskPixels = IntArray(pixels.size)
        modelImage.getPixels(pixels, 0, MODEL_SIZE, 0, 0, MODEL_SIZE, MODEL_SIZE)
        modelMask.getPixels(maskPixels, 0, MODEL_SIZE, 0, 0, MODEL_SIZE, MODEL_SIZE)
        modelImage.recycle()
        modelMask.recycle()

        val imageValues = FloatArray(3 * pixels.size)
        val maskValues = FloatArray(maskPixels.size)
        val plane = pixels.size
        var selected = 0
        for (index in pixels.indices) {
            val color = pixels[index]
            imageValues[index] = Color.red(color) / 255f
            imageValues[plane + index] = Color.green(color) / 255f
            imageValues[plane * 2 + index] = Color.blue(color) / 255f
            val alpha = Color.alpha(maskPixels[index]) / 255f
            val binary = if (alpha >= MASK_THRESHOLD) 1f else 0f
            maskValues[index] = binary
            if (binary > 0f) selected++
        }
        require(selected >= 12) { "Paint over the complete object before using Content-aware fill" }
        require(selected.toFloat() / maskValues.size < .78f) { "Content-aware fill needs visible surroundings to rebuild the selection" }
        coroutineContext.ensureActive()

        val imageTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(imageValues),
            longArrayOf(1, 3, MODEL_SIZE.toLong(), MODEL_SIZE.toLong()),
        )
        val maskTensor = OnnxTensor.createTensor(
            environment,
            FloatBuffer.wrap(maskValues),
            longArrayOf(1, 1, MODEL_SIZE.toLong(), MODEL_SIZE.toLong()),
        )
        try {
            session.run(mapOf("image" to imageTensor, "mask" to maskTensor)).use { result ->
                coroutineContext.ensureActive()
                val generated = tensorToBitmap(result[0].value)
                try {
                    compositeGeneratedPixels(source, mask, generated)
                } finally {
                    generated.recycle()
                }
            }
        } finally {
            imageTensor.close()
            maskTensor.close()
            imageValues.fill(0f)
            maskValues.fill(0f)
            pixels.fill(0)
            maskPixels.fill(0)
        }
        }
    }

    override fun close() {
        session.close()
        stream.close()
        asset.close()
    }

    private fun createSession(model: ByteBuffer): OrtSession {
        val attempts = listOf<(OrtSession.SessionOptions) -> Unit>(
            { it.addNnapi() },
            { it.addXnnpack(mapOf("intra_op_num_threads" to "4")) },
            { it.addCPU(true) },
        )
        var lastFailure: Throwable? = null
        attempts.forEach { configure ->
            val options = OrtSession.SessionOptions()
            try {
                options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                options.setMemoryPatternOptimization(true)
                options.setIntraOpNumThreads(4)
                configure(options)
                model.rewind()
                val created = environment.createSession(model, options)
                options.close()
                return created
            } catch (failure: Throwable) {
                lastFailure = failure
                options.close()
            }
        }
        throw IllegalStateException("LaMa could not start on this device", lastFailure)
    }

    @Suppress("UNCHECKED_CAST")
    private fun tensorToBitmap(value: Any): Bitmap {
        val tensor = value as? Array<Array<Array<FloatArray>>>
            ?: error("LaMa returned an unsupported output tensor")
        require(tensor.size == 1 && tensor[0].size >= 3) { "LaMa returned an invalid output tensor" }
        val channels = tensor[0]
        val red = channels[0]
        val green = channels[1]
        val blue = channels[2]
        require(red.size == MODEL_SIZE && red.firstOrNull()?.size == MODEL_SIZE) { "LaMa returned an unexpected image size" }

        // The published Carve graph emits RGB values in 0..255. Retain compatibility with an
        // equivalent normalized export so a future signed model update does not tint the output.
        var observedMaximum = 0f
        for (y in 0 until MODEL_SIZE step 31) for (x in 0 until MODEL_SIZE step 31) {
            observedMaximum = maxOf(observedMaximum, red[y][x], green[y][x], blue[y][x])
        }
        val scale = if (observedMaximum <= 2f) 255f else 1f
        val output = IntArray(MODEL_SIZE * MODEL_SIZE)
        for (y in 0 until MODEL_SIZE) for (x in 0 until MODEL_SIZE) {
            val index = y * MODEL_SIZE + x
            output[index] = Color.rgb(
                (red[y][x] * scale).roundToInt().coerceIn(0, 255),
                (green[y][x] * scale).roundToInt().coerceIn(0, 255),
                (blue[y][x] * scale).roundToInt().coerceIn(0, 255),
            )
        }
        return Bitmap.createBitmap(output, MODEL_SIZE, MODEL_SIZE, Bitmap.Config.ARGB_8888)
    }

    private fun compositeGeneratedPixels(source: Bitmap, mask: Bitmap, generated: Bitmap): Bitmap {
        val resizedGenerated = Bitmap.createScaledBitmap(generated, source.width, source.height, true)
        val resizedMask = if (mask.width == source.width && mask.height == source.height) mask
        else Bitmap.createScaledBitmap(mask, source.width, source.height, true)
        val original = IntArray(source.width * source.height)
        val replacement = IntArray(original.size)
        val alpha = IntArray(original.size)
        source.getPixels(original, 0, source.width, 0, 0, source.width, source.height)
        resizedGenerated.getPixels(replacement, 0, source.width, 0, 0, source.width, source.height)
        resizedMask.getPixels(alpha, 0, source.width, 0, 0, source.width, source.height)
        for (index in original.indices) {
            // A short smoothstep makes seams disappear without allowing the model to recolour
            // unselected pixels. The caller supplies a slightly expanded/feathered repair mask.
            val raw = Color.alpha(alpha[index]) / 255f
            val amount = (raw * raw * (3f - 2f * raw)).coerceIn(0f, 1f)
            if (amount > 0f) original[index] = blend(original[index], replacement[index], amount)
        }
        resizedGenerated.recycle()
        if (resizedMask !== mask) resizedMask.recycle()
        replacement.fill(0)
        alpha.fill(0)
        return Bitmap.createBitmap(original, source.width, source.height, Bitmap.Config.ARGB_8888)
    }

    private fun blend(original: Int, generated: Int, amount: Float): Int {
        fun channel(shift: Int): Int {
            val from = original ushr shift and 0xff
            val to = generated ushr shift and 0xff
            return (from + (to - from) * amount).roundToInt().coerceIn(0, 255)
        }
        return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
    }

    private companion object {
        const val MODEL_ASSET = "lama_fp32.onnx"
        const val MODEL_SIZE = 512
        const val MASK_THRESHOLD = .22f
    }
}
