package com.danyal.vaultgallery.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Process
import com.google.crypto.tink.subtle.Ed25519Verify
import java.io.File
import java.io.FileInputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

/**
 * Atomic, removable model-pack storage. No downloaded bytes become executable until their
 * expected length, SHA-256 and detached Ed25519 signature have all been verified.
 */
class SignedModelPackRepository(
    context: Context,
    private val catalogue: List<ModelPack>,
) : ModelPackRepository {
    private val appContext = context.applicationContext
    private val modelRoot = File(appContext.noBackupFilesDir, "ai-model-packs").apply { mkdirs() }
    private val transient = ConcurrentHashMap<String, ModelPackStatus>()

    override suspend fun statuses(): List<ModelPackStatus> = withContext(Dispatchers.IO) {
        catalogue.map { pack ->
            transient[pack.id] ?: when {
                !isCompatible(pack) -> ModelPackStatus(pack, ModelPackState.INCOMPATIBLE, error = incompatibility(pack))
                installedModel(pack).let { it.isFile && it.length() == pack.sizeBytes } -> ModelPackStatus(pack, ModelPackState.READY, pack.sizeBytes)
                else -> ModelPackStatus(pack, ModelPackState.NOT_INSTALLED)
            }
        }
    }

    override suspend fun install(id: String) = withContext(Dispatchers.IO) {
        val pack = catalogue.singleOrNull { it.id == id } ?: error("Unknown model pack: $id")
        require(isCompatible(pack)) { incompatibility(pack) }
        require(pack.downloadUrl.startsWith("https://")) { "Model packs require HTTPS" }
        require(pack.sha256.matches(Regex("[0-9a-fA-F]{64}"))) { "Invalid expected SHA-256" }
        val destination = installedModel(pack)
        val partial = File(modelRoot, ".${safeName(pack.id)}-${safeName(pack.version)}.download")
        partial.delete()
        transient[id] = ModelPackStatus(pack, ModelPackState.DOWNLOADING)
        try {
            val connection = URL(pack.downloadUrl).openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 15_000
            connection.readTimeout = 30_000
            connection.setRequestProperty("Accept", "application/octet-stream")
            connection.connect()
            connection.useConnection {
                require(responseCode == HttpURLConnection.HTTP_OK) { "Download failed with HTTP $responseCode" }
                val declaredLength = contentLengthLong
                require(declaredLength < 0L || declaredLength == pack.sizeBytes) { "Server length does not match the signed catalogue" }
                inputStream.use { input ->
                    partial.outputStream().buffered().use { output ->
                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
                        var total = 0L
                        while (true) {
                            coroutineContext.ensureActive()
                            val count = input.read(buffer)
                            if (count < 0) break
                            output.write(buffer, 0, count)
                            total += count
                            require(total <= pack.sizeBytes) { "Downloaded model exceeds expected size" }
                            transient[id] = ModelPackStatus(pack, ModelPackState.DOWNLOADING, total)
                        }
                        output.flush()
                        require(total == pack.sizeBytes) { "Incomplete model download" }
                    }
                }
            }
            transient[id] = ModelPackStatus(pack, ModelPackState.VERIFYING, partial.length())
            verify(partial, pack)
            destination.parentFile?.mkdirs()
            if (destination.exists()) destination.delete()
            require(partial.renameTo(destination)) { "Could not atomically install the verified model" }
            transient[id] = ModelPackStatus(pack, ModelPackState.READY, destination.length())
        } catch (failure: Throwable) {
            partial.delete()
            transient[id] = ModelPackStatus(pack, ModelPackState.FAILED, error = failure.message ?: failure.javaClass.simpleName)
            throw failure
        }
    }

    override suspend fun remove(id: String) = withContext(Dispatchers.IO) {
        val pack = catalogue.singleOrNull { it.id == id } ?: error("Unknown model pack: $id")
        installedModel(pack).delete()
        modelRoot.listFiles()?.filter { it.name.startsWith(".${safeName(id)}-") }?.forEach(File::delete)
        transient[id] = ModelPackStatus(pack, if (isCompatible(pack)) ModelPackState.NOT_INSTALLED else ModelPackState.INCOMPATIBLE)
    }

    override suspend fun preferredAccelerators(): List<AiAccelerator> = buildList {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) add(AiAccelerator.NNAPI)
        add(AiAccelerator.XNNPACK)
        add(AiAccelerator.CPU)
    }

    fun modelFile(id: String): File {
        val pack = catalogue.singleOrNull { it.id == id } ?: error("Unknown model pack: $id")
        return installedModel(pack).also { require(it.isFile) { "Model pack $id is not installed" } }
    }

    private fun verify(file: File, pack: ModelPack) {
        require(file.length() == pack.sizeBytes) { "Model size mismatch" }
        val actualHash = sha256(file)
        require(actualHash.equals(pack.sha256, ignoreCase = true)) { "Model checksum mismatch" }
        val publicKey = Base64.getDecoder().decode(pack.signaturePublicKeyBase64)
        val signature = Base64.getDecoder().decode(pack.signatureBase64)
        require(publicKey.size == 32) { "Invalid Ed25519 public key" }
        require(signature.size == 64) { "Invalid Ed25519 signature" }
        Ed25519Verify(publicKey).verify(signature, actualHash.lowercase().toByteArray(Charsets.UTF_8))
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE * 4)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun isCompatible(pack: ModelPack): Boolean = incompatibility(pack).isEmpty()

    private fun incompatibility(pack: ModelPack): String {
        val requirements = pack.requirements
        if (Build.VERSION.SDK_INT < requirements.minimumAndroidApi) return "Requires Android API ${requirements.minimumAndroidApi}"
        if (requirements.arm64Only && Build.SUPPORTED_ABIS.none { it == "arm64-v8a" }) return "Requires a 64-bit ARM device"
        val memoryInfo = ActivityManager.MemoryInfo()
        (appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager).getMemoryInfo(memoryInfo)
        if (memoryInfo.totalMem / (1024L * 1024L) < requirements.minimumRamMb) return "Requires ${requirements.minimumRamMb} MB RAM"
        return ""
    }

    private fun installedModel(pack: ModelPack) = File(modelRoot, "${safeName(pack.id)}-${safeName(pack.version)}.onnx")
    private fun safeName(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private inline fun <T> HttpURLConnection.useConnection(block: HttpURLConnection.() -> T): T = try {
        block()
    } finally {
        disconnect()
    }
}
