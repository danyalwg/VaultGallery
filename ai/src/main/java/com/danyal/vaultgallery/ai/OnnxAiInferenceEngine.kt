package com.danyal.vaultgallery.ai

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.io.File
import java.nio.FloatBuffer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/** One ONNX Runtime path for every optional model, with deterministic accelerator fallback. */
class OnnxAiInferenceEngine(
    private val modelResolver: (String) -> File,
) : AiInferenceEngine {
    private val environment by lazy { OrtEnvironment.getEnvironment("VaultGallery") }

    override suspend fun run(
        modelId: String,
        inputs: Map<String, FloatTensorInput>,
        acceleratorOrder: List<AiAccelerator>,
    ): Map<String, Any> = withContext(Dispatchers.Default) {
        val model = modelResolver(modelId)
        require(model.isFile && model.length() > 0L) { "Model $modelId is not installed" }
        coroutineContext.ensureActive()
        val failures = mutableListOf<String>()
        acceleratorOrder.distinct().forEach { accelerator ->
            coroutineContext.ensureActive()
            val attempt = runCatching { runOnce(model, inputs, accelerator) }
            attempt.getOrNull()?.let { return@withContext it }
            failures += "$accelerator: ${attempt.exceptionOrNull()?.message ?: "unsupported"}"
        }
        error("No compatible inference backend. ${failures.joinToString("; ")}")
    }

    private fun runOnce(
        model: File,
        inputs: Map<String, FloatTensorInput>,
        accelerator: AiAccelerator,
    ): Map<String, Any> {
        OrtSession.SessionOptions().use { options ->
            options.setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
            options.setMemoryPatternOptimization(true)
            options.setIntraOpNumThreads(Runtime.getRuntime().availableProcessors().coerceIn(2, 6))
            when (accelerator) {
                AiAccelerator.NNAPI -> options.addNnapi()
                AiAccelerator.XNNPACK -> options.addXnnpack(mapOf("intra_op_num_threads" to "4"))
                AiAccelerator.CPU -> options.addCPU(true)
            }
            environment.createSession(model.absolutePath, options).use { session ->
                val tensors = inputs.mapValues { (_, input) ->
                    require(input.shape.fold(1L, Long::times) == input.values.size.toLong()) { "Tensor shape does not match its values" }
                    OnnxTensor.createTensor(environment, FloatBuffer.wrap(input.values), input.shape)
                }
                try {
                    session.run(tensors).use { result ->
                        return result.associate { entry -> entry.key to entry.value.value }
                    }
                } finally {
                    tensors.values.forEach(OnnxTensor::close)
                }
            }
        }
    }
}
