@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery.security

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

data class SecureItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val dateTakenMs: Long,
    val sizeBytes: Long,
    val isTrashed: Boolean = false,
    val trashedAtMs: Long = 0L,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

class SecureVault(private val context: Context) {
    private val prefs = context.getSharedPreferences("secure-vault-auth", Context.MODE_PRIVATE)
    private val root = File(context.filesDir, "secure-vault")
    private val mediaRoot = File(root, "encrypted-media")
    private val indexFile = File(root, "index.vgi")
    private val random = SecureRandom()
    private val alias = "${context.packageName}.vault-device-wrap-v1"
    private val biometricAlias = "${context.packageName}.vault-biometric-wrap-v1"

    init {
        mediaRoot.mkdirs()
        cleanupShareCache()
    }

    fun isConfigured(): Boolean = prefs.contains("salt") && prefs.contains("wrapped_master")

    fun setup(secret: CharArray): ByteArray {
        require(secret.size >= 6) { "Use at least six characters" }
        check(!isConfigured()) { "Secure Gallery is already configured" }
        val salt = randomBytes(16)
        val master = randomBytes(32)
        val pinKey = derive(secret, salt)
        val pinEnvelope = seal(SecretKeySpec(pinKey, "AES"), master, AUTH_AAD)
        pinKey.fill(0)
        val wrapped = sealWithProviderIv(deviceKey(), pinEnvelope, DEVICE_AAD)
        prefs.edit()
            .putString("salt", b64(salt))
            .putString("wrapped_master", b64(wrapped))
            .putInt("argon_time", ARGON_TIME)
            .putInt("argon_memory_kib", ARGON_MEMORY_KIB)
            .apply()
        writeIndex(master, emptyList())
        return master
    }

    fun unlock(secret: CharArray): ByteArray {
        val salt = unb64(prefs.getString("salt", null) ?: throw SecurityException("Vault is not configured"))
        val wrapped = unb64(prefs.getString("wrapped_master", null) ?: throw SecurityException("Vault is not configured"))
        val pinEnvelope = open(deviceKey(), wrapped, DEVICE_AAD)
        val pinKey = derive(secret, salt)
        return try {
            open(SecretKeySpec(pinKey, "AES"), pinEnvelope, AUTH_AAD)
        } catch (_: Exception) {
            throw SecurityException("Incorrect PIN or passphrase")
        } finally {
            pinKey.fill(0)
            pinEnvelope.fill(0)
        }
    }

    fun changeSecret(master: ByteArray, newSecret: CharArray) {
        require(newSecret.size >= 6) { "Use at least six characters" }
        require(master.size == 32) { "Vault is locked" }
        val salt = randomBytes(16)
        val pinKey = derive(newSecret, salt)
        val pinEnvelope = try {
            seal(SecretKeySpec(pinKey, "AES"), master, AUTH_AAD)
        } finally {
            pinKey.fill(0)
        }
        val wrapped = try {
            sealWithProviderIv(deviceKey(), pinEnvelope, DEVICE_AAD)
        } finally {
            pinEnvelope.fill(0)
        }
        prefs.edit()
            .putString("salt", b64(salt))
            .putString("wrapped_master", b64(wrapped))
            .putInt("argon_time", ARGON_TIME)
            .putInt("argon_memory_kib", ARGON_MEMORY_KIB)
            .apply()
    }

    fun isBiometricEnabled(): Boolean = prefs.contains("biometric_wrapped_master")

    fun prepareBiometricEnrollment() {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(biometricAlias)) keyStore.deleteEntry(biometricAlias)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            biometricAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            builder.setUserAuthenticationParameters(BIOMETRIC_AUTH_WINDOW_SECONDS, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(BIOMETRIC_AUTH_WINDOW_SECONDS)
        }
        generator.init(builder.build())
        generator.generateKey()
    }

    fun completeBiometricEnrollment(master: ByteArray) {
        require(master.size == 32) { "Vault is locked" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, biometricKey())
        cipher.updateAAD(BIOMETRIC_AAD)
        val nonce = cipher.iv
        require(nonce.size == 12) { "Unsupported biometric key IV" }
        prefs.edit().putString("biometric_wrapped_master", b64(nonce + cipher.doFinal(master))).apply()
    }

    fun prepareBiometricUnlock() {
        require(prefs.contains("biometric_wrapped_master")) { "Biometric unlock is not enabled" }
        biometricKey()
    }

    fun completeBiometricUnlock(): ByteArray {
        val envelope = unb64(prefs.getString("biometric_wrapped_master", null) ?: throw SecurityException("Biometric unlock is not enabled"))
        require(envelope.size > 12 + GCM_TAG_SIZE) { "Invalid biometric key envelope" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, biometricKey(), GCMParameterSpec(128, envelope.copyOfRange(0, 12)))
        cipher.updateAAD(BIOMETRIC_AAD)
        return cipher.doFinal(envelope, 12, envelope.size - 12)
    }

    fun disableBiometric() {
        prefs.edit().remove("biometric_wrapped_master").apply()
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(biometricAlias)) keyStore.deleteEntry(biometricAlias)
    }

    fun list(master: ByteArray, includeTrashed: Boolean = false): List<SecureItem> {
        if (!indexFile.exists()) return emptyList()
        val plain = open(SecretKeySpec(master, "AES"), indexFile.readBytes(), INDEX_AAD)
        val all = try {
            val json = JSONArray(String(plain, Charsets.UTF_8))
            buildList {
                for (index in 0 until json.length()) {
                    val item = json.getJSONObject(index)
                    add(
                        SecureItem(
                            id = item.getString("id"),
                            name = item.getString("name"),
                            mimeType = item.getString("mime"),
                            dateTakenMs = item.getLong("date"),
                            sizeBytes = item.getLong("size"),
                            isTrashed = item.optBoolean("trashed", false),
                            trashedAtMs = item.optLong("trashed_at", 0L),
                        ),
                    )
                }
            }.sortedByDescending { it.dateTakenMs }
        } finally {
            plain.fill(0)
        }
        val expiry = System.currentTimeMillis() - TRASH_RETENTION_MS
        val expired = all.filter { it.isTrashed && it.trashedAtMs in 1..expiry }
        if (expired.isNotEmpty()) {
            expired.forEach { encryptedFile(it).delete() }
            writeIndex(master, all - expired.toSet())
        }
        return all.filterNot { it in expired }.filter { includeTrashed || !it.isTrashed }
    }

    fun importUri(master: ByteArray, resolver: ContentResolver, uri: Uri): SecureItem {
        val metadata = resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) null else Pair(
                cursor.getString(cursor.getColumnIndexOrThrow(OpenableColumns.DISPLAY_NAME)).orEmpty(),
                cursor.getLong(cursor.getColumnIndexOrThrow(OpenableColumns.SIZE)),
            )
        }
        val name = metadata?.first?.ifBlank { "Private media" } ?: "Private media"
        val size = metadata?.second ?: -1L
        val mime = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        require(mime.startsWith("image/") || mime.startsWith("video/")) { "Unsupported media type" }
        val id = UUID.randomUUID().toString()
        val temp = File(mediaRoot, "$id.tmp")
        val destination = File(mediaRoot, "$id.vg")
        resolver.openInputStream(uri)?.use { input ->
            encryptStream(master, id, BufferedInputStream(input), temp)
        } ?: throw IOException("Source is unavailable")
        verifyFile(master, id, temp)
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = false)
            temp.delete()
        }
        val item = SecureItem(id, name, mime, System.currentTimeMillis(), size)
        val items = list(master, includeTrashed = true).toMutableList().apply { add(item) }
        writeIndex(master, items)
        return item
    }

    fun delete(master: ByteArray, item: SecureItem) {
        val remaining = list(master, includeTrashed = true).filterNot { it.id == item.id }
        val file = encryptedFile(item)
        val tombstone = File(mediaRoot, "${item.id}.delete")
        if (file.exists() && !file.renameTo(tombstone)) throw IOException("Could not prepare secure deletion")
        try {
            writeIndex(master, remaining)
            tombstone.delete()
        } catch (error: Throwable) {
            if (tombstone.exists()) tombstone.renameTo(file)
            throw error
        }
    }

    fun moveToTrash(master: ByteArray, item: SecureItem) {
        val items = list(master, includeTrashed = true).map {
            if (it.id == item.id) it.copy(isTrashed = true, trashedAtMs = System.currentTimeMillis()) else it
        }
        writeIndex(master, items)
    }

    fun restore(master: ByteArray, item: SecureItem) {
        val items = list(master, includeTrashed = true).map {
            if (it.id == item.id) it.copy(isTrashed = false, trashedAtMs = 0L) else it
        }
        writeIndex(master, items)
    }

    fun emptyTrash(master: ByteArray) {
        val items = list(master, includeTrashed = true)
        items.filter { it.isTrashed }.forEach { encryptedFile(it).delete() }
        writeIndex(master, items.filterNot { it.isTrashed })
    }

    fun decryptBytes(master: ByteArray, item: SecureItem, maxBytes: Int = 64 * 1024 * 1024): ByteArray {
        require(item.sizeBytes < 0 || item.sizeBytes <= maxBytes) { "Media is too large for an image preview" }
        val out = ByteArrayOutputStream(if (item.sizeBytes in 1..maxBytes.toLong()) item.sizeBytes.toInt() else 32 * 1024)
        decryptStream(master, item.id, encryptedFile(item)) { bytes, count ->
            if (out.size() + count > maxBytes) throw IOException("Preview exceeds memory limit")
            out.write(bytes, 0, count)
        }
        return out.toByteArray()
    }

    fun createShareFile(master: ByteArray, item: SecureItem): File {
        val shareRoot = File(context.cacheDir, "secure-share").apply { mkdirs() }
        val safeExtension = when {
            item.mimeType.startsWith("image/jpeg") -> ".jpg"
            item.mimeType.startsWith("image/png") -> ".png"
            item.mimeType.startsWith("image/webp") -> ".webp"
            item.mimeType.startsWith("video/mp4") -> ".mp4"
            else -> ".bin"
        }
        val output = File(shareRoot, "${UUID.randomUUID()}$safeExtension")
        BufferedOutputStream(FileOutputStream(output)).use { stream ->
            decryptStream(master, item.id, encryptedFile(item)) { bytes, count -> stream.write(bytes, 0, count) }
            stream.flush()
        }
        return output
    }

    fun dataSourceFactory(master: ByteArray, item: SecureItem): androidx.media3.datasource.DataSource.Factory {
        val keyCopy = fileKey(master, item.id)
        val file = encryptedFile(item)
        return androidx.media3.datasource.DataSource.Factory { SecureChunkDataSource(file, keyCopy.copyOf()) }
    }

    fun cleanupShareCache() {
        File(context.cacheDir, "secure-share").listFiles()?.forEach { it.delete() }
        mediaRoot.listFiles { file -> file.extension == "tmp" }?.forEach { it.delete() }
    }

    fun reset(master: ByteArray) {
        master.fill(0)
        root.deleteRecursively()
        prefs.edit().clear().apply()
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        if (keyStore.containsAlias(biometricAlias)) keyStore.deleteEntry(biometricAlias)
        mediaRoot.mkdirs()
    }

    private fun encryptedFile(item: SecureItem) = File(mediaRoot, "${item.id}.vg")

    private fun writeIndex(master: ByteArray, items: List<SecureItem>) {
        val json = JSONArray()
        items.forEach { item ->
            json.put(JSONObject().apply {
                put("id", item.id); put("name", item.name); put("mime", item.mimeType)
                put("date", item.dateTakenMs); put("size", item.sizeBytes)
                put("trashed", item.isTrashed); put("trashed_at", item.trashedAtMs)
            })
        }
        val encrypted = seal(SecretKeySpec(master, "AES"), json.toString().toByteArray(), INDEX_AAD)
        root.mkdirs()
        val temp = File(root, "index.tmp")
        FileOutputStream(temp).use { it.write(encrypted); it.fd.sync() }
        if (indexFile.exists() && !indexFile.delete()) throw IOException("Could not replace secure index")
        if (!temp.renameTo(indexFile)) throw IOException("Could not commit secure index")
    }

    private fun encryptStream(master: ByteArray, objectId: String, input: BufferedInputStream, destination: File) {
        val fileKey = fileKey(master, objectId)
        try {
            val prefix = randomBytes(8)
            DataOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { output ->
                output.write(MAGIC)
                output.writeInt(FORMAT_VERSION)
                output.writeInt(CHUNK_SIZE)
                output.write(prefix)
                val buffer = ByteArray(CHUNK_SIZE)
                var index = 0
                while (true) {
                    var count = 0
                    while (count < buffer.size) {
                        val read = input.read(buffer, count, buffer.size - count)
                        if (read < 0) break
                        count += read
                    }
                    if (count == 0) break
                    val nonce = nonce(prefix, index)
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(fileKey, "AES"), GCMParameterSpec(128, nonce))
                    cipher.updateAAD(chunkAad(index, count))
                    val encrypted = cipher.doFinal(buffer, 0, count)
                    output.writeInt(count)
                    output.write(encrypted)
                    index++
                }
                output.flush()
            }
        } finally {
            fileKey.fill(0)
        }
    }

    private fun decryptStream(master: ByteArray, objectId: String, source: File, consumer: (ByteArray, Int) -> Unit) {
        val fileKey = fileKey(master, objectId)
        try {
            DataInputStream(BufferedInputStream(FileInputStream(source))).use { input ->
                val magic = ByteArray(MAGIC.size).also { input.readFully(it) }
                require(magic.contentEquals(MAGIC)) { "Invalid secure media" }
                require(input.readInt() == FORMAT_VERSION) { "Unsupported secure media version" }
                val chunkSize = input.readInt()
                require(chunkSize in 64 * 1024..4 * 1024 * 1024) { "Invalid secure chunk size" }
                val prefix = ByteArray(8).also { input.readFully(it) }
                var index = 0
                while (true) {
                    val count = try { input.readInt() } catch (_: EOFException) { break }
                    require(count in 1..chunkSize) { "Invalid secure chunk" }
                    val encrypted = ByteArray(count + GCM_TAG_SIZE).also { input.readFully(it) }
                    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                    cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(fileKey, "AES"), GCMParameterSpec(128, nonce(prefix, index)))
                    cipher.updateAAD(chunkAad(index, count))
                    val plain = cipher.doFinal(encrypted)
                    consumer(plain, plain.size)
                    plain.fill(0)
                    encrypted.fill(0)
                    index++
                }
            }
        } finally {
            fileKey.fill(0)
        }
    }

    private fun verifyFile(master: ByteArray, objectId: String, source: File) {
        var verified = 0L
        decryptStream(master, objectId, source) { _, count -> verified += count }
        require(verified >= 0) { "Secure verification failed" }
    }

    private fun fileKey(master: ByteArray, objectId: String): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(master, "HmacSHA256"))
        mac.update("vault-file-key-v1:".toByteArray())
        return mac.doFinal(objectId.toByteArray(Charsets.US_ASCII))
    }

    private fun derive(secret: CharArray, salt: ByteArray): ByteArray {
        val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(secret))
        val password = ByteArray(encoded.remaining()).also(encoded::get)
        return try {
            Argon2Kt().hash(
                mode = Argon2Mode.ARGON2_ID,
                password = password,
                salt = salt,
                tCostInIterations = prefs.getInt("argon_time", ARGON_TIME),
                mCostInKibibyte = prefs.getInt("argon_memory_kib", ARGON_MEMORY_KIB),
            ).rawHashAsByteArray()
        } finally {
            password.fill(0)
        }
    }

    private fun deviceKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build(),
        )
        return generator.generateKey()
    }

    private fun biometricKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(biometricAlias, null) as? SecretKey ?: throw SecurityException("Biometric key is unavailable")
    }

    private fun seal(key: SecretKey, plain: ByteArray, aad: ByteArray): ByteArray {
        val nonce = randomBytes(12)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return nonce + cipher.doFinal(plain)
    }

    private fun sealWithProviderIv(key: SecretKey, plain: ByteArray, aad: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        cipher.updateAAD(aad)
        val nonce = cipher.iv
        require(nonce.size == 12) { "Unsupported Keystore IV size" }
        return nonce + cipher.doFinal(plain)
    }

    private fun open(key: SecretKey, envelope: ByteArray, aad: ByteArray): ByteArray {
        require(envelope.size > 12 + GCM_TAG_SIZE) { "Invalid encrypted envelope" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.copyOfRange(0, 12)))
        cipher.updateAAD(aad)
        return cipher.doFinal(envelope, 12, envelope.size - 12)
    }

    private fun nonce(prefix: ByteArray, index: Int): ByteArray = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
    private fun chunkAad(index: Int, count: Int) = ByteBuffer.allocate(MAGIC.size + 8).put(MAGIC).putInt(index).putInt(count).array()
    private fun randomBytes(size: Int) = ByteArray(size).also(random::nextBytes)
    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(value: String) = Base64.decode(value, Base64.NO_WRAP)

    companion object {
        private val MAGIC = "VLTGAL01".toByteArray(Charsets.US_ASCII)
        private val AUTH_AAD = "vault-auth-v1".toByteArray()
        private val DEVICE_AAD = "vault-device-v1".toByteArray()
        private val INDEX_AAD = "vault-index-v1".toByteArray()
        private val BIOMETRIC_AAD = "vault-biometric-v1".toByteArray()
        private const val FORMAT_VERSION = 1
        private const val CHUNK_SIZE = 1024 * 1024
        private const val GCM_TAG_SIZE = 16
        private const val ARGON_TIME = 3
        private const val ARGON_MEMORY_KIB = 65_536
        private const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        private const val BIOMETRIC_AUTH_WINDOW_SECONDS = 15
    }
}

private data class ChunkRecord(val offset: Long, val plainStart: Long, val plainLength: Int)

private class SecureChunkDataSource(private val file: File, private val master: ByteArray) : BaseDataSource(false) {
    private var raf: RandomAccessFile? = null
    private var prefix = ByteArray(8)
    private var records = emptyList<ChunkRecord>()
    private var totalLength = 0L
    private var readPosition = 0L
    private var bytesRemaining = 0L
    private var cachedIndex = -1
    private var cachedChunk = ByteArray(0)

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val input = RandomAccessFile(file, "r").also { raf = it }
        val magic = ByteArray(8).also(input::readFully)
        if (!magic.contentEquals("VLTGAL01".toByteArray())) throw IOException("Invalid secure video")
        if (input.readInt() != 1) throw IOException("Unsupported secure video")
        val chunkSize = input.readInt()
        if (chunkSize !in 64 * 1024..4 * 1024 * 1024) throw IOException("Invalid secure video chunk size")
        prefix = ByteArray(8).also(input::readFully)
        val found = ArrayList<ChunkRecord>()
        var plainStart = 0L
        while (input.filePointer < input.length()) {
            val plainLength = input.readInt()
            if (plainLength !in 1..chunkSize) throw IOException("Invalid secure video chunk")
            found += ChunkRecord(input.filePointer, plainStart, plainLength)
            input.seek(input.filePointer + plainLength + 16L)
            plainStart += plainLength
        }
        records = found
        totalLength = plainStart
        if (dataSpec.position > totalLength) throw IOException("Position outside secure media")
        readPosition = dataSpec.position
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) totalLength - readPosition else minOf(dataSpec.length, totalLength - readPosition)
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining == 0L) return C.RESULT_END_OF_INPUT
        val index = records.binarySearch { record ->
            when {
                readPosition < record.plainStart -> 1
                readPosition >= record.plainStart + record.plainLength -> -1
                else -> 0
            }
        }.let { if (it >= 0) it else records.indexOfLast { record -> readPosition >= record.plainStart } }
        if (index !in records.indices) return C.RESULT_END_OF_INPUT
        if (cachedIndex != index) decrypt(index)
        val record = records[index]
        val within = (readPosition - record.plainStart).toInt()
        val count = minOf(length, cachedChunk.size - within, bytesRemaining.toInt())
        cachedChunk.copyInto(buffer, offset, within, within + count)
        readPosition += count
        bytesRemaining -= count
        bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri = Uri.fromFile(file)

    override fun close() {
        raf?.close(); raf = null
        cachedChunk.fill(0); cachedChunk = ByteArray(0); cachedIndex = -1
        master.fill(0)
        transferEnded()
    }

    private fun decrypt(index: Int) {
        val input = raf ?: throw IOException("Secure source is closed")
        val record = records[index]
        input.seek(record.offset)
        val encrypted = ByteArray(record.plainLength + 16).also(input::readFully)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val nonce = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
        val aad = ByteBuffer.allocate(16).put("VLTGAL01".toByteArray()).putInt(index).putInt(record.plainLength).array()
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(master, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        cachedChunk.fill(0)
        cachedChunk = cipher.doFinal(encrypted)
        cachedIndex = index
        encrypted.fill(0)
    }
}
