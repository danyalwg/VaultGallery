package com.danyal.vaultgallery.ai

enum class AiAccelerator { NNAPI, XNNPACK, CPU }
enum class ModelPackState { NOT_INSTALLED, DOWNLOADING, VERIFYING, READY, INCOMPATIBLE, FAILED }
data class ModelRequirements(val minimumRamMb: Int, val minimumAndroidApi: Int, val arm64Only: Boolean = true)
data class ModelPack(
    val id: String,
    val displayName: String,
    val version: String,
    val sizeBytes: Long,
    val sha256: String,
    val signatureBase64: String,
    /** A compiled-in trust root. The signature covers the lowercase SHA-256 text. */
    val signaturePublicKeyBase64: String,
    val sourceLicense: String,
    val weightsLicense: String,
    val requirements: ModelRequirements,
    val downloadUrl: String,
)
data class ModelPackStatus(val pack: ModelPack, val state: ModelPackState, val downloadedBytes: Long = 0, val error: String? = null)
interface ModelPackRepository {
    suspend fun statuses(): List<ModelPackStatus>
    suspend fun install(id: String)
    suspend fun remove(id: String)
    suspend fun preferredAccelerators(): List<AiAccelerator>
}
data class FloatTensorInput(val values: FloatArray, val shape: LongArray)
interface AiInferenceEngine {
    suspend fun run(modelId: String, inputs: Map<String, FloatTensorInput>, acceleratorOrder: List<AiAccelerator> = listOf(AiAccelerator.NNAPI, AiAccelerator.XNNPACK, AiAccelerator.CPU)): Map<String, Any>
}
