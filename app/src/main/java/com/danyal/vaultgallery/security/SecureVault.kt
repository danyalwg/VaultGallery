@file:androidx.annotation.OptIn(markerClass = [androidx.media3.common.util.UnstableApi::class])

package com.danyal.vaultgallery.security

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.provider.MediaStore
import android.content.ContentValues
import android.os.Build
import com.danyal.vaultgallery.extractPreservableJpegMetadata
import android.os.Environment
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.media.MediaMetadataRetriever
import androidx.media3.common.C
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DataSourceInputStream
import com.danyal.vaultgallery.imageDecodeSafetyIssue
import com.lambdapioneer.argon2kt.Argon2Kt
import com.lambdapioneer.argon2kt.Argon2Mode
import com.google.crypto.tink.BinaryKeysetReader
import com.google.crypto.tink.BinaryKeysetWriter
import com.google.crypto.tink.KeysetHandle
import com.google.crypto.tink.StreamingAead
import com.google.crypto.tink.aead.internal.InsecureNonceAesGcmJce
import com.google.crypto.tink.streamingaead.StreamingAeadConfig
import com.google.crypto.tink.streamingaead.StreamingAeadKeyTemplates
import com.google.crypto.tink.subtle.AesGcmJce
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.ByteArrayInputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.SeekableByteChannel
import java.nio.file.StandardOpenOption
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.Mac
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

enum class SecureStoragePolicy { ENCRYPTED, LOCKED_ONLY }

enum class SecureUnlockPolicy {
    BIOMETRIC_OR_PIN,
    BIOMETRIC_ONLY,
    PIN_ONLY,
}

data class SecureItem(
    val id: String,
    val name: String,
    val mimeType: String,
    val dateTakenMs: Long,
    val sizeBytes: Long,
    val albumName: String = "Imported",
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0L,
    val isFavourite: Boolean = false,
    val isTrashed: Boolean = false,
    val trashedAtMs: Long = 0L,
    val tag: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val storagePolicy: SecureStoragePolicy = SecureStoragePolicy.ENCRYPTED,
    val rating: Int = 0,
) {
    val isVideo: Boolean get() = mimeType.startsWith("video/")
}

data class VaultIntegrityIssue(
    val itemId: String?,
    val itemName: String,
    val problem: String,
)

data class VaultIntegrityReport(
    val checkedItems: Int,
    val verifiedItems: Int,
    val issues: List<VaultIntegrityIssue>,
    val orphanFiles: List<String>,
    val completedAtMs: Long,
) {
    val healthy: Boolean get() = issues.isEmpty() && orphanFiles.isEmpty()
}

data class VaultBackupResult(
    val itemCount: Int,
    val plainBytes: Long,
)

internal const val SECURE_TEMP_FILE_RETENTION_MS = 24L * 60L * 60L * 1000L

internal fun isAbandonedSecureTemp(lastModifiedMs: Long, nowMs: Long): Boolean =
    lastModifiedMs < nowMs - SECURE_TEMP_FILE_RETENTION_MS

class SecureVault(
    private val context: Context,
    rootOverride: File? = null,
    authenticationPreferencesName: String = "secure-vault-auth",
) {
    private val prefs = context.getSharedPreferences(authenticationPreferencesName, Context.MODE_PRIVATE)
    private val privateRoot = File(context.filesDir, "secure-vault")
    private val root = rootOverride?.apply { mkdirs() } ?: persistentRoot().also { persistent ->
            require(hasPersistentStorageAccess(context)) {
                "Persistent storage access is required so Secure Gallery media survives app uninstall"
            }
            migratePrivateVault(privateRoot, persistent)
        }
    // The production vault keeps the historical database name. Isolated/custom
    // vaults must never open that database with their own unrelated key: doing so
    // can produce SQLCipher's "file is not a database" error and, more
    // importantly, couples independent vault lifecycles together.
    private val metadataDatabaseName = if (
        rootOverride == null && authenticationPreferencesName == "secure-vault-auth"
    ) {
        SecureMetadataDatabase.NAME
    } else {
        val identity = "${root.absolutePath}|$authenticationPreferencesName"
        val suffix = MessageDigest.getInstance("SHA-256")
            .digest(identity.toByteArray(Charsets.UTF_8))
            .take(12)
            .joinToString("") { "%02x".format(it) }
        "secure-gallery-metadata-$suffix.db"
    }
    private val mediaRoot = File(root, "encrypted-media")
    private val lockedRoot = File(root, "locked-media")
    private val thumbnailRoot = File(root, "protected-thumbnails")
    private val indexFile = File(root, "index.vgi")
    private val portableAuthFile = File(root, "vault-auth.vga")
    private val random = SecureRandom()
    private val alias = "${context.packageName}.vault-device-wrap-v1"
    private val legacyBiometricAlias = "${context.packageName}.vault-biometric-wrap-v1"
    private val biometricAlias = "${context.packageName}.vault-biometric-wrap-v2"
    private val metadataLock = Any()
    @Volatile private var metadataDb: SecureMetadataDatabase? = null
    @Volatile private var metadataPassphrase: ByteArray? = null

    init {
        mediaRoot.mkdirs()
        lockedRoot.mkdirs()
        thumbnailRoot.mkdirs()
        runCatching { File(root, ".nomedia").let { if (!it.exists()) it.writeBytes(ByteArray(0)) } }
        cleanupShareCache()
    }

    fun isConfigured(): Boolean =
        (prefs.contains("salt") && prefs.contains("wrapped_master")) || portableAuthFile.isFile

    fun setup(secret: CharArray): ByteArray {
        require(secret.size == 4 && secret.all(Char::isDigit)) { "Use exactly four digits" }
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
        writePortableAuth(secret, salt, master)
        writeIndex(master, emptyList())
        return master
    }

    fun unlock(secret: CharArray): ByteArray {
        val master = if (prefs.contains("salt") && prefs.contains("wrapped_master")) {
            val salt = unb64(prefs.getString("salt", null) ?: throw SecurityException("Vault is not configured"))
            val wrapped = unb64(prefs.getString("wrapped_master", null) ?: throw SecurityException("Vault is not configured"))
            val pinEnvelope = open(deviceKey(), wrapped, DEVICE_AAD)
            val pinKey = derive(secret, salt)
            try {
                open(SecretKeySpec(pinKey, "AES"), pinEnvelope, AUTH_AAD)
            } catch (_: Exception) {
                throw SecurityException("Incorrect PIN or passphrase")
            } finally {
                pinKey.fill(0)
                pinEnvelope.fill(0)
            }
        } else {
            unlockPortable(secret)
        }
        // A successful PIN unlock is the point at which the app can safely create both an
        // uninstall-resistant recovery header and a current encrypted metadata snapshot. Neither
        // contains plaintext media. This is what makes a future reinstall recoverable.
        writePortableAuth(secret, null, master)
        snapshotPortableIndex(master)
        return master
    }

    fun changeSecret(master: ByteArray, newSecret: CharArray) {
        require(newSecret.size == 4 && newSecret.all(Char::isDigit)) { "Use exactly four digits" }
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
        writePortableAuth(newSecret, salt, master)
        snapshotPortableIndex(master)
    }

    /** Creates a portable recovery key protected by a user-chosen high-entropy passphrase. */
    fun exportRecoveryKey(master: ByteArray, recoveryPassphrase: CharArray): ByteArray {
        require(master.size == 32) { "Vault is locked" }
        require(recoveryPassphrase.size >= 12) { "Use at least 12 characters for the recovery passphrase" }
        val salt = randomBytes(16)
        val wrappingKey = derive(recoveryPassphrase, salt, ARGON_TIME, ARGON_MEMORY_KIB)
        val envelope = try {
            seal(SecretKeySpec(wrappingKey, "AES"), master, RECOVERY_KEY_AAD)
        } finally {
            wrappingKey.fill(0)
        }
        return try {
            JSONObject().apply {
                put("format", "Vault Gallery recovery key")
                put("version", RECOVERY_KEY_VERSION)
                put("salt", b64(salt))
                put("envelope", b64(envelope))
                put("argon_time", ARGON_TIME)
                put("argon_memory_kib", ARGON_MEMORY_KIB)
                put("created_at", System.currentTimeMillis())
            }.toString(2).toByteArray(Charsets.UTF_8)
        } finally {
            salt.fill(0)
            envelope.fill(0)
        }
    }

    /** Authenticates and opens an exported recovery key. Used by the restore workflow. */
    fun openRecoveryKey(payload: ByteArray, recoveryPassphrase: CharArray): ByteArray {
        require(recoveryPassphrase.size >= 12) { "Use the recovery passphrase used during export" }
        val json = runCatching { JSONObject(payload.toString(Charsets.UTF_8)) }
            .getOrElse { throw SecurityException("This is not a Vault Gallery recovery-key file") }
        require(json.optInt("version", 0) == RECOVERY_KEY_VERSION) { "Unsupported recovery-key version" }
        val salt = unb64(json.getString("salt"))
        val envelope = unb64(json.getString("envelope"))
        val time = json.optInt("argon_time", ARGON_TIME).coerceAtLeast(1)
        val memory = json.optInt("argon_memory_kib", ARGON_MEMORY_KIB).coerceAtLeast(8 * 1024)
        val wrappingKey = derive(recoveryPassphrase, salt, time, memory)
        return try {
            open(SecretKeySpec(wrappingKey, "AES"), envelope, RECOVERY_KEY_AAD).also {
                require(it.size == 32) { "The recovery key is damaged" }
            }
        } catch (_: Exception) {
            throw SecurityException("Incorrect recovery passphrase or damaged recovery-key file")
        } finally {
            wrappingKey.fill(0); salt.fill(0); envelope.fill(0)
        }
    }

    /** Creates a self-contained passphrase-encrypted backup without plaintext temp files. */
    fun exportEncryptedBackup(
        master: ByteArray,
        recoveryPassphrase: CharArray,
        destination: OutputStream,
        onProgress: ((Long, Long) -> Unit)? = null,
    ): VaultBackupResult {
        require(master.size == 32) { "Vault is locked" }
        require(recoveryPassphrase.size >= 12) { "Use at least 12 characters for the backup passphrase" }
        val items = list(master, includeTrashed = true)
        val totalBytes = items.sumOf(SecureItem::sizeBytes)
        val salt = randomBytes(16)
        val wrappingKey = derive(recoveryPassphrase, salt, ARGON_TIME, ARGON_MEMORY_KIB)
        StreamingAeadConfig.register()
        val keyset = KeysetHandle.generateNew(StreamingAeadKeyTemplates.AES256_GCM_HKDF_1MB)
        val encryptedKeyset = try {
            ByteArrayOutputStream().use { encoded ->
                keyset.writeWithAssociatedData(
                    BinaryKeysetWriter.withOutputStream(encoded),
                    AesGcmJce(wrappingKey),
                    BACKUP_KEYSET_AAD,
                )
                encoded.toByteArray()
            }
        } finally {
            wrappingKey.fill(0)
        }
        try {
            val header = DataOutputStream(BufferedOutputStream(destination))
            header.write(BACKUP_MAGIC)
            header.writeInt(BACKUP_FORMAT_VERSION)
            header.writeInt(ARGON_TIME)
            header.writeInt(ARGON_MEMORY_KIB)
            header.writeInt(salt.size)
            header.write(salt)
            header.writeInt(encryptedKeyset.size)
            header.write(encryptedKeyset)
            val encryptedOutput = keyset.getPrimitive(StreamingAead::class.java)
                .newEncryptingStream(header, BACKUP_STREAM_AAD)
            ZipOutputStream(BufferedOutputStream(encryptedOutput)).use { zip ->
                zip.setLevel(0)
                val manifest = JSONObject().apply {
                    put("format", "Vault Gallery encrypted backup")
                    put("version", BACKUP_FORMAT_VERSION)
                    put("created_at", System.currentTimeMillis())
                    put("item_count", items.size)
                    put("plain_bytes", totalBytes)
                    put("items", JSONArray().apply { items.forEach { put(it.toPortableJson()) } })
                }.toString().toByteArray(Charsets.UTF_8)
                require(manifest.size <= MAX_BACKUP_MANIFEST_SIZE) { "Vault metadata is too large to back up safely" }
                zip.putNextEntry(ZipEntry(BACKUP_MANIFEST_ENTRY))
                zip.write(manifest)
                zip.closeEntry()
                manifest.fill(0)
                var completed = 0L
                items.forEach { item ->
                    zip.putNextEntry(ZipEntry("$BACKUP_MEDIA_PREFIX${item.id}"))
                    var itemBytes = 0L
                    readPlain(master, item) { buffer, count ->
                        zip.write(buffer, 0, count)
                        itemBytes += count
                        onProgress?.invoke(completed + itemBytes, totalBytes)
                    }
                    require(itemBytes == item.sizeBytes) { "${item.name} changed while the backup was being created" }
                    zip.closeEntry()
                    completed += itemBytes
                }
            }
            return VaultBackupResult(items.size, totalBytes)
        } finally {
            salt.fill(0)
            encryptedKeyset.fill(0)
        }
    }

    /** Restores into the active vault without replacing any existing item. */
    fun restoreEncryptedBackup(
        master: ByteArray,
        recoveryPassphrase: CharArray,
        source: InputStream,
        onProgress: ((Long, Long) -> Unit)? = null,
    ): VaultBackupResult {
        require(master.size == 32) { "Vault is locked" }
        require(recoveryPassphrase.size >= 12) { "Use the passphrase used when the backup was created" }
        val header = DataInputStream(BufferedInputStream(source))
        val magic = ByteArray(BACKUP_MAGIC.size).also(header::readFully)
        require(magic.contentEquals(BACKUP_MAGIC)) { "This is not a Vault Gallery encrypted backup" }
        require(header.readInt() == BACKUP_FORMAT_VERSION) { "Unsupported Vault Gallery backup version" }
        val time = header.readInt().coerceAtLeast(1)
        val memory = header.readInt().coerceAtLeast(8 * 1024)
        val saltLength = header.readInt()
        require(saltLength in 16..64) { "Invalid backup salt" }
        val salt = ByteArray(saltLength).also(header::readFully)
        val encryptedKeysetLength = header.readInt()
        require(encryptedKeysetLength in 1..MAX_ENCRYPTED_KEYSET_SIZE) { "Invalid backup key header" }
        val encryptedKeyset = ByteArray(encryptedKeysetLength).also(header::readFully)
        val wrappingKey = derive(recoveryPassphrase, salt, time, memory)
        StreamingAeadConfig.register()
        val keyset = try {
            KeysetHandle.readWithAssociatedData(
                BinaryKeysetReader.withBytes(encryptedKeyset),
                AesGcmJce(wrappingKey),
                BACKUP_KEYSET_AAD,
            )
        } catch (_: Exception) {
            throw SecurityException("Incorrect backup passphrase or damaged backup")
        } finally {
            wrappingKey.fill(0)
            salt.fill(0)
            encryptedKeyset.fill(0)
        }
        val staged = ArrayList<Pair<File, File>>()
        return try {
            val decrypted = try {
                keyset.getPrimitive(StreamingAead::class.java).newDecryptingStream(header, BACKUP_STREAM_AAD)
            } catch (_: Exception) {
                throw SecurityException("Incorrect backup passphrase or damaged backup")
            }
            ZipInputStream(BufferedInputStream(decrypted)).use { zip ->
                require(zip.nextEntry?.name == BACKUP_MANIFEST_ENTRY) { "Backup metadata is missing" }
                val manifestBytes = zip.readLimited(MAX_BACKUP_MANIFEST_SIZE)
                val manifest = try { JSONObject(manifestBytes.toString(Charsets.UTF_8)) }
                finally { manifestBytes.fill(0) }
                require(manifest.optInt("version", 0) == BACKUP_FORMAT_VERSION) { "Unsupported backup metadata version" }
                val itemArray = manifest.getJSONArray("items")
                val itemCount = manifest.getInt("item_count")
                require(itemCount == itemArray.length() && itemCount in 0..MAX_BACKUP_ITEMS) { "Invalid backup item count" }
                val declaredBytes = manifest.getLong("plain_bytes")
                require(declaredBytes >= 0L && declaredBytes == (0 until itemArray.length()).sumOf { itemArray.getJSONObject(it).getLong("size") }) {
                    "Backup size metadata is inconsistent"
                }
                val available = android.os.StatFs(root.absolutePath).availableBytes
                require(declaredBytes <= (available - BACKUP_FREE_SPACE_RESERVE).coerceAtLeast(0L)) {
                    "Not enough free space to restore this backup"
                }
                zip.closeEntry()
                val existing = list(master, includeTrashed = true)
                val usedIds = existing.mapTo(HashSet()) { it.id }
                val restored = ArrayList<SecureItem>(itemCount)
                var completed = 0L
                repeat(itemCount) { index ->
                    val original = itemArray.getJSONObject(index).toSecureItem()
                    require(zip.nextEntry?.name == "$BACKUP_MEDIA_PREFIX${original.id}") { "Backup media order is invalid" }
                    val id = if (usedIds.add(original.id)) original.id else UUID.randomUUID().toString().also(usedIds::add)
                    val restoredItem = original.copy(id = id)
                    val targetRoot = if (restoredItem.storagePolicy == SecureStoragePolicy.ENCRYPTED) mediaRoot else lockedRoot
                    val temp = File(targetRoot, "$id.restore.tmp")
                    val target = File(targetRoot, if (restoredItem.storagePolicy == SecureStoragePolicy.ENCRYPTED) "$id.vg" else "$id.media")
                    require(!target.exists()) { "A restore destination already exists" }
                    temp.delete()
                    val exact = ExactSizeInputStream(zip, restoredItem.sizeBytes) { itemBytes ->
                        onProgress?.invoke(completed + itemBytes, declaredBytes)
                    }
                    if (restoredItem.storagePolicy == SecureStoragePolicy.ENCRYPTED) {
                        encryptStream(master, id, exact, temp)
                        exact.verifyComplete()
                        verifyFile(master, id, temp)
                    } else {
                        BufferedOutputStream(FileOutputStream(temp)).use { output -> exact.copyTo(output, STREAM_COPY_BUFFER_SIZE) }
                        exact.verifyComplete()
                    }
                    zip.closeEntry()
                    staged += temp to target
                    restored += restoredItem
                    completed += restoredItem.sizeBytes
                }
                require(zip.nextEntry == null) { "Backup contains unexpected files" }
                staged.forEach { (temp, target) ->
                    if (!temp.renameTo(target)) {
                        temp.copyTo(target, overwrite = false)
                        temp.delete()
                    }
                }
                try {
                    writeIndex(master, existing + restored)
                } catch (error: Throwable) {
                    staged.forEach { (_, target) -> target.delete() }
                    throw error
                }
                VaultBackupResult(restored.size, declaredBytes)
            }
        } catch (error: Throwable) {
            staged.forEach { (temp, target) -> temp.delete(); target.delete() }
            if (error is SecurityException) throw error
            throw IOException(error.message ?: "The encrypted backup could not be restored", error)
        }
    }

    fun isBiometricEnabled(): Boolean =
        prefs.contains(BIOMETRIC_WRAPPED_MASTER_V2) || prefs.contains(BIOMETRIC_WRAPPED_MASTER_V1)

    fun prepareBiometricEnrollment(): Cipher {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(biometricAlias)) keyStore.deleteEntry(biometricAlias)
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val builder = KeyGenParameterSpec.Builder(
            biometricAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setKeySize(256)
            .setUserAuthenticationRequired(true)
            .setInvalidatedByBiometricEnrollment(true)
        if (android.os.Build.VERSION.SDK_INT >= 30) {
            builder.setUserAuthenticationParameters(
                0,
                KeyProperties.AUTH_BIOMETRIC_STRONG,
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setUserAuthenticationValidityDurationSeconds(-1)
        }
        generator.init(builder.build())
        generator.generateKey()
        return Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
            init(Cipher.ENCRYPT_MODE, biometricKey())
        }
    }

    fun completeBiometricEnrollment(master: ByteArray, authenticatedCipher: Cipher) {
        require(master.size == 32) { "Vault is locked" }
        val nonce = authenticatedCipher.iv
        require(nonce.size == BIOMETRIC_CBC_IV_SIZE) { "Unsupported biometric key IV" }
        val envelope = nonce + authenticatedCipher.doFinal(master)
        prefs.edit()
            .putString(BIOMETRIC_WRAPPED_MASTER_V2, b64(envelope))
            .remove(BIOMETRIC_WRAPPED_MASTER_V1)
            .apply()
        deleteKeyIfPresent(legacyBiometricAlias)
    }

    fun prepareBiometricUnlock(): Cipher {
        val v2Envelope = prefs.getString(BIOMETRIC_WRAPPED_MASTER_V2, null)?.let(::unb64)
        if (v2Envelope != null) {
            require(v2Envelope.size > BIOMETRIC_CBC_IV_SIZE) { "Invalid biometric key envelope" }
            return Cipher.getInstance("AES/CBC/PKCS7Padding").apply {
                init(
                    Cipher.DECRYPT_MODE,
                    biometricKey(),
                    javax.crypto.spec.IvParameterSpec(v2Envelope.copyOfRange(0, BIOMETRIC_CBC_IV_SIZE)),
                )
            }
        }
        val envelope = unb64(prefs.getString(BIOMETRIC_WRAPPED_MASTER_V1, null) ?: throw SecurityException("Biometric unlock is not enabled"))
        require(envelope.size > GCM_NONCE_SIZE + GCM_TAG_SIZE) { "Invalid biometric key envelope" }
        return Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, biometricKey(legacyBiometricAlias), GCMParameterSpec(128, envelope.copyOfRange(0, GCM_NONCE_SIZE)))
            updateAAD(BIOMETRIC_AAD)
        }
    }

    fun completeBiometricUnlock(authenticatedCipher: Cipher): ByteArray {
        prefs.getString(BIOMETRIC_WRAPPED_MASTER_V2, null)?.let { encoded ->
            val envelope = unb64(encoded)
            require(envelope.size > BIOMETRIC_CBC_IV_SIZE) { "Invalid biometric key envelope" }
            return authenticatedCipher.doFinal(
                envelope,
                BIOMETRIC_CBC_IV_SIZE,
                envelope.size - BIOMETRIC_CBC_IV_SIZE,
            )
        }
        val envelope = unb64(prefs.getString(BIOMETRIC_WRAPPED_MASTER_V1, null) ?: throw SecurityException("Biometric unlock is not enabled"))
        require(envelope.size > GCM_NONCE_SIZE + GCM_TAG_SIZE) { "Invalid biometric key envelope" }
        return authenticatedCipher.doFinal(envelope, GCM_NONCE_SIZE, envelope.size - GCM_NONCE_SIZE)
    }

    fun disableBiometric() {
        prefs.edit()
            .remove(BIOMETRIC_WRAPPED_MASTER_V2)
            .remove(BIOMETRIC_WRAPPED_MASTER_V1)
            .apply()
        deleteKeyIfPresent(biometricAlias)
        deleteKeyIfPresent(legacyBiometricAlias)
    }

    fun list(master: ByteArray, includeTrashed: Boolean = false): List<SecureItem> {
        val all = synchronized(metadataLock) {
            metadataDatabase(master).items().all().map(SecureItemEntity::toItem)
        }
        val expiry = System.currentTimeMillis() - TRASH_RETENTION_MS
        val expired = all.filter { it.isTrashed && it.trashedAtMs in 1..expiry }
        if (expired.isNotEmpty()) {
            expired.forEach { storageFile(it).delete(); thumbnailFile(it).delete(); contactSheetFile(it).delete() }
            writeIndex(master, all - expired.toSet())
        }
        return all.filterNot { it in expired }.filter { includeTrashed || !it.isTrashed }
    }

    private fun readLegacyIndex(master: ByteArray): List<SecureItem> {
        if (!indexFile.exists()) return emptyList()
        val plain = open(SecretKeySpec(master, "AES"), indexFile.readBytes(), INDEX_AAD)
        return try {
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
                            albumName = item.optString("album", "Imported").ifBlank { "Imported" },
                            width = item.optInt("width", 0),
                            height = item.optInt("height", 0),
                            durationMs = item.optLong("duration", 0L),
                            isFavourite = item.optBoolean("favourite", false),
                            isTrashed = item.optBoolean("trashed", false),
                            trashedAtMs = item.optLong("trashed_at", 0L),
                            tag = item.optString("tag"),
                            latitude = item.optDouble("latitude").takeUnless(Double::isNaN),
                            longitude = item.optDouble("longitude").takeUnless(Double::isNaN),
                            storagePolicy = runCatching { SecureStoragePolicy.valueOf(item.optString("storage_policy", "ENCRYPTED")) }.getOrDefault(SecureStoragePolicy.ENCRYPTED),
                            rating = item.optInt("rating", 0).coerceIn(0, 5),
                        ),
                    )
                }
            }.sortedByDescending { it.dateTakenMs }
        } finally {
            plain.fill(0)
        }
    }

    fun importUri(
        master: ByteArray,
        resolver: ContentResolver,
        uri: Uri,
        albumOverride: String? = null,
        onProgress: ((Long, Long) -> Unit)? = null,
        storagePolicy: SecureStoragePolicy = SecureStoragePolicy.ENCRYPTED,
        preferredId: String? = null,
        displayNameOverride: String? = null,
    ): SecureItem {
        val id = preferredId?.also { require(UUID.fromString(it).toString() == it) { "Invalid secure media identifier" } }
            ?: UUID.randomUUID().toString()
        // A background move may be stopped after the secure write completes but before its
        // checkpoint is persisted. A stable per-transfer id makes that retry return the already
        // imported item instead of creating a duplicate.
        list(master, includeTrashed = true).firstOrNull { it.id == id }?.let { return it }
        val metadata = resolver.query(uri, arrayOf(
            OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE,
            MediaStore.MediaColumns.WIDTH, MediaStore.MediaColumns.HEIGHT,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
            MediaStore.Images.ImageColumns.DATE_TAKEN,
        ), null, null, null)?.use { cursor ->
            if (!cursor.moveToFirst()) null else ImportMetadata(
                name = cursor.stringOrNull(OpenableColumns.DISPLAY_NAME).orEmpty(),
                size = cursor.longOrNull(OpenableColumns.SIZE) ?: -1L,
                album = cursor.stringOrNull(MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME).orEmpty(),
                width = (cursor.longOrNull(MediaStore.MediaColumns.WIDTH) ?: 0L).toInt(),
                height = (cursor.longOrNull(MediaStore.MediaColumns.HEIGHT) ?: 0L).toInt(),
                duration = cursor.longOrNull(MediaStore.Video.VideoColumns.DURATION) ?: 0L,
                dateTaken = cursor.longOrNull(MediaStore.Images.ImageColumns.DATE_TAKEN) ?: 0L,
            )
        }
        val name = displayNameOverride?.trim()?.takeIf { it.isNotBlank() }
            ?: metadata?.name?.ifBlank { "Private media" }
            ?: "Private media"
        val size = metadata?.size ?: -1L
        val mime = resolver.getType(uri).orEmpty().ifBlank { "application/octet-stream" }
        require(mime.startsWith("image/") || mime.startsWith("video/")) { "Unsupported media type" }
        val targetRoot = if (storagePolicy == SecureStoragePolicy.ENCRYPTED) mediaRoot else lockedRoot
        val temp = File(targetRoot, "$id.tmp")
        val destination = File(targetRoot, if (storagePolicy == SecureStoragePolicy.ENCRYPTED) "$id.vg" else "$id.media")
        // Recover a file-only partial transaction left before the encrypted index commit.
        temp.delete()
        destination.delete()
        var copiedBytes = 0L
        try {
            resolver.openInputStream(uri)?.use { input ->
                if (storagePolicy == SecureStoragePolicy.ENCRYPTED) {
                    encryptStream(master, id, BufferedInputStream(input), temp) { transferred ->
                        copiedBytes = transferred
                        onProgress?.invoke(transferred, size)
                    }
                } else {
                    BufferedInputStream(input).use { source ->
                        BufferedOutputStream(FileOutputStream(temp)).use { output ->
                            val buffer = ByteArray(1024 * 1024)
                            var transferred = 0L
                            while (true) {
                                val count = source.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                transferred += count
                                copiedBytes = transferred
                                onProgress?.invoke(transferred, size)
                            }
                            output.flush()
                        }
                    }
                }
            } ?: throw IOException("Source is unavailable")
            require(size <= 0L || copiedBytes == size) {
                "The source changed during secure import ($copiedBytes of $size bytes read)"
            }
            if (storagePolicy == SecureStoragePolicy.ENCRYPTED) verifyFile(master, id, temp)
            if (!temp.renameTo(destination)) {
                temp.copyTo(destination, overwrite = false)
                temp.delete()
            }
        } catch (error: Throwable) {
            temp.delete()
            destination.delete()
            throw error
        }
        val item = SecureItem(
            id = id,
            name = name,
            mimeType = mime,
            dateTakenMs = metadata?.dateTaken?.takeIf { it > 0 } ?: System.currentTimeMillis(),
            sizeBytes = copiedBytes.takeIf { it > 0L } ?: size.coerceAtLeast(0L),
            albumName = albumOverride?.trim()?.takeIf { it.isNotBlank() }
                ?: metadata?.album?.ifBlank { "Imported" }
                ?: "Imported",
            width = metadata?.width ?: 0,
            height = metadata?.height ?: 0,
            durationMs = metadata?.duration ?: 0L,
            storagePolicy = storagePolicy,
        )
        val items = list(master, includeTrashed = true).toMutableList().apply { add(item) }
        writeIndex(master, items)
        return item
    }

    fun importEditedImage(
        master: ByteArray,
        source: SecureItem,
        encoded: ByteArray,
        width: Int,
        height: Int,
        mimeType: String = "image/jpeg",
        extension: String = "jpg",
        retainSourceName: Boolean = false,
    ): SecureItem {
        val id = UUID.randomUUID().toString()
        val targetRoot = if (source.storagePolicy == SecureStoragePolicy.ENCRYPTED) mediaRoot else lockedRoot
        val temp = File(targetRoot, "$id.tmp")
        val destination = File(targetRoot, if (source.storagePolicy == SecureStoragePolicy.ENCRYPTED) "$id.vg" else "$id.media")
        if (source.storagePolicy == SecureStoragePolicy.ENCRYPTED) {
            BufferedInputStream(ByteArrayInputStream(encoded)).use { input -> encryptStream(master, id, input, temp) }
            verifyFile(master, id, temp)
        } else temp.writeBytes(encoded)
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = false)
            temp.delete()
        }
        val base = source.name.substringBeforeLast('.', source.name)
        val item = SecureItem(
            id = id,
            name = if (retainSourceName) source.name else "${base}_edited_${System.currentTimeMillis()}.$extension",
            mimeType = mimeType,
            dateTakenMs = source.dateTakenMs,
            sizeBytes = encoded.size.toLong(),
            albumName = source.albumName,
            width = width,
            height = height,
            tag = source.tag,
            latitude = source.latitude,
            longitude = source.longitude,
            storagePolicy = source.storagePolicy,
        )
        writeIndex(master, list(master, includeTrashed = true) + item)
        return item
    }

    fun importGeneratedFile(
        master: ByteArray,
        sourceFile: File,
        name: String,
        mimeType: String,
        albumName: String,
        width: Int,
        height: Int,
        durationMs: Long = 0L,
        storagePolicy: SecureStoragePolicy = SecureStoragePolicy.ENCRYPTED,
        dateTakenMs: Long = System.currentTimeMillis(),
        tag: String = "",
        latitude: Double? = null,
        longitude: Double? = null,
    ): SecureItem {
        val id = UUID.randomUUID().toString()
        val targetRoot = if (storagePolicy == SecureStoragePolicy.ENCRYPTED) mediaRoot else lockedRoot
        val temp = File(targetRoot, "$id.tmp")
        val destination = File(targetRoot, if (storagePolicy == SecureStoragePolicy.ENCRYPTED) "$id.vg" else "$id.media")
        if (storagePolicy == SecureStoragePolicy.ENCRYPTED) {
            BufferedInputStream(FileInputStream(sourceFile)).use { input -> encryptStream(master, id, input, temp) }
            verifyFile(master, id, temp)
        } else sourceFile.copyTo(temp, overwrite = true)
        if (!temp.renameTo(destination)) { temp.copyTo(destination, overwrite = false); temp.delete() }
        val item = SecureItem(
            id = id, name = name, mimeType = mimeType, dateTakenMs = dateTakenMs,
            sizeBytes = sourceFile.length(), albumName = albumName, width = width, height = height, durationMs = durationMs,
            tag = tag, latitude = latitude, longitude = longitude,
            storagePolicy = storagePolicy,
        )
        writeIndex(master, list(master, includeTrashed = true) + item)
        return item
    }

    fun delete(master: ByteArray, item: SecureItem) {
        val remaining = list(master, includeTrashed = true).filterNot { it.id == item.id }
        val file = storageFile(item)
        val tombstone = File(file.parentFile, "${item.id}.delete")
        if (file.exists() && !file.renameTo(tombstone)) throw IOException("Could not prepare secure deletion")
        try {
            writeIndex(master, remaining)
            tombstone.delete()
            thumbnailFile(item).delete()
            contactSheetFile(item).delete()
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

    fun setFavourite(master: ByteArray, item: SecureItem, favourite: Boolean) {
        writeIndex(master, list(master, includeTrashed = true).map { if (it.id == item.id) it.copy(isFavourite = favourite) else it })
    }

    fun setFavourite(master: ByteArray, items: Collection<SecureItem>, favourite: Boolean) {
        val ids = items.mapTo(HashSet()) { it.id }
        writeIndex(master, list(master, includeTrashed = true).map { if (it.id in ids) it.copy(isFavourite = favourite) else it })
    }

    fun updateMetadata(
        master: ByteArray,
        items: Collection<SecureItem>,
        tag: String? = null,
        dateTakenMs: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        replaceLocation: Boolean = false,
        rating: Int? = null,
    ) {
        val ids = items.mapTo(HashSet()) { it.id }
        writeIndex(master, list(master, includeTrashed = true).map { current ->
            if (current.id !in ids) current else current.copy(
                tag = tag?.trim()?.take(60) ?: current.tag,
                dateTakenMs = dateTakenMs ?: current.dateTakenMs,
                latitude = if (replaceLocation) latitude else current.latitude,
                longitude = if (replaceLocation) longitude else current.longitude,
                rating = rating?.coerceIn(0, 5) ?: current.rating,
            )
        })
    }

    fun moveToAlbum(master: ByteArray, selected: List<SecureItem>, albumName: String, displayNameOverride: String? = null) {
        val ids = selected.mapTo(HashSet()) { it.id }
        val safeName = albumName.trim().ifBlank { "Imported" }
        writeIndex(master, list(master, includeTrashed = true).map {
            if (it.id in ids) it.copy(albumName = safeName, name = displayNameOverride?.trim()?.takeIf(String::isNotBlank) ?: it.name) else it
        })
    }

    fun copyToAlbum(
        master: ByteArray,
        selected: List<SecureItem>,
        albumName: String,
        onProgress: ((Long, Long) -> Unit)? = null,
        displayNameOverride: String? = null,
    ) {
        val safeName = albumName.trim().ifBlank { "Imported" }
        val copies = selected.map { source ->
            val id = UUID.randomUUID().toString()
            if (source.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) {
                val destination = File(lockedRoot, "$id.media")
                lockedFile(source).inputStream().buffered().use { input ->
                    destination.outputStream().buffered().use { output ->
                        val buffer = ByteArray(1024 * 1024); var transferred = 0L
                        while (true) { val count = input.read(buffer); if (count < 0) break; output.write(buffer, 0, count); transferred += count; onProgress?.invoke(transferred, source.sizeBytes) }
                    }
                }
                return@map source.copy(id = id, name = displayNameOverride ?: source.name, albumName = safeName, isTrashed = false, trashedAtMs = 0L)
            }
            val encrypted = File(mediaRoot, "$id.tmp")
            val expected = source.sizeBytes.coerceAtLeast(1L)
            try {
                val dataSource = dataSourceFactory(master, source).createDataSource()
                DataSourceInputStream(dataSource, DataSpec(Uri.EMPTY)).use { sourceInput ->
                    BufferedInputStream(sourceInput).use { input -> encryptStream(master, id, input, encrypted) { transferred ->
                        onProgress?.invoke(transferred.coerceAtMost(expected), expected)
                    } }
                }
                verifyFile(master, id, encrypted)
                val destination = File(mediaRoot, "$id.vg")
                if (!encrypted.renameTo(destination)) { encrypted.copyTo(destination, overwrite = false); encrypted.delete() }
                source.copy(id = id, name = displayNameOverride ?: source.name, albumName = safeName, isTrashed = false, trashedAtMs = 0L)
            } finally {
                encrypted.delete()
            }
        }
        writeIndex(master, list(master, includeTrashed = true) + copies)
    }

    /** Converts selected items transactionally. The original is only removed after the replacement
     * has been fully written and, for encrypted output, authenticated from beginning to end. */
    fun convertStoragePolicy(
        master: ByteArray,
        selected: Collection<SecureItem>,
        target: SecureStoragePolicy,
        onProgress: ((Int, Int) -> Unit)? = null,
    ) {
        val originals = list(master, includeTrashed = true)
        var updated = originals
        selected.forEachIndexed { index, item ->
            if (item.storagePolicy == target) { onProgress?.invoke(index + 1, selected.size); return@forEachIndexed }
            val destination = if (target == SecureStoragePolicy.ENCRYPTED) encryptedFile(item) else lockedFile(item)
            val temp = File(destination.parentFile, "${item.id}.convert")
            try {
                if (target == SecureStoragePolicy.ENCRYPTED) {
                    BufferedInputStream(FileInputStream(lockedFile(item))).use { encryptStream(master, item.id, it, temp) }
                    verifyFile(master, item.id, temp)
                } else {
                    BufferedOutputStream(FileOutputStream(temp)).use { output ->
                        decryptStream(master, item.id, encryptedFile(item)) { bytes, count -> output.write(bytes, 0, count) }
                    }
                    require(item.sizeBytes < 0 || temp.length() == item.sizeBytes) { "Converted media size did not match" }
                }
                if (destination.exists()) destination.delete()
                if (!temp.renameTo(destination)) { temp.copyTo(destination, overwrite = true); temp.delete() }
                updated = updated.map { if (it.id == item.id) it.copy(storagePolicy = target) else it }
                writeIndex(master, updated)
                if (target == SecureStoragePolicy.ENCRYPTED) lockedFile(item).delete() else encryptedFile(item).delete()
                thumbnailFile(item).delete()
                contactSheetFile(item).delete()
                onProgress?.invoke(index + 1, selected.size)
            } catch (error: Throwable) {
                temp.delete()
                throw error
            }
        }
    }

    /** Small previews persist inside the app sandbox. Encrypted items get an authenticated encrypted
     * thumbnail; locked-only items get a private plaintext thumbnail for maximum scrolling speed. */
    fun loadOrCreateThumbnail(master: ByteArray, item: SecureItem, maxDimension: Int = 512): Bitmap? {
        val cache = thumbnailFile(item)
        if (cache.exists()) {
            val encoded = cache.readBytes()
            val jpeg = if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) {
                runCatching { open(SecretKeySpec(master, "AES"), encoded, "vault-thumb-v1:${item.id}".toByteArray()) }.getOrNull()
            } else encoded
            if (jpeg != null) return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size).also { if (jpeg !== encoded) jpeg.fill(0) }
        }
        val bitmap = if (item.isVideo) {
            val retriever = MediaMetadataRetriever()
            try {
                if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) retriever.setDataSource(lockedFile(item).absolutePath)
                else retriever.setDataSource(context, SecureMediaProvider.uri(context, item))
                retriever.getFrameAtTime(0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } finally {
                retriever.release()
            }
        } else {
            val source = if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) lockedFile(item) else createShareFile(master, item)
            try {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(source.absolutePath, bounds)
                var sample = 1
                while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maxDimension) sample *= 2
                BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
            } finally { if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) source.delete() }
        } ?: return null
        val scaled = if (maxOf(bitmap.width, bitmap.height) > maxDimension) {
            val ratio = maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height)
            Bitmap.createScaledBitmap(bitmap, (bitmap.width * ratio).toInt().coerceAtLeast(1), (bitmap.height * ratio).toInt().coerceAtLeast(1), true).also { if (it !== bitmap) bitmap.recycle() }
        } else bitmap
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 86, output)
        val jpeg = output.toByteArray()
        val stored = if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) seal(SecretKeySpec(master, "AES"), jpeg, "vault-thumb-v1:${item.id}".toByteArray()) else jpeg
        cache.writeBytes(stored)
        if (stored !== jpeg) stored.fill(0)
        jpeg.fill(0)
        return scaled
    }

    fun emptyTrash(master: ByteArray) {
        val items = list(master, includeTrashed = true)
        items.filter { it.isTrashed }.forEach { storageFile(it).delete(); thumbnailFile(it).delete(); contactSheetFile(it).delete() }
        writeIndex(master, items.filterNot { it.isTrashed })
    }

    fun decryptBytes(master: ByteArray, item: SecureItem, maxBytes: Int = 64 * 1024 * 1024): ByteArray {
        require(item.sizeBytes < 0 || item.sizeBytes <= maxBytes) { "Media is too large for an image preview" }
        val out = ByteArrayOutputStream(if (item.sizeBytes in 1..maxBytes.toLong()) item.sizeBytes.toInt() else 32 * 1024)
        readPlain(master, item) { bytes, count ->
            if (out.size() + count > maxBytes) throw IOException("Preview exceeds memory limit")
            out.write(bytes, 0, count)
        }
        return out.toByteArray()
    }

    internal fun preservableJpegMetadata(master: ByteArray, item: SecureItem): List<com.danyal.vaultgallery.JpegMetadataSegment> {
        if (!item.mimeType.equals("image/jpeg", ignoreCase = true)) return emptyList()
        return if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) {
            lockedFile(item).inputStream().use { extractPreservableJpegMetadata(it) }
        } else {
            val source = dataSourceFactory(master, item).createDataSource()
            DataSourceInputStream(source, DataSpec(Uri.EMPTY)).use { stream ->
                stream.open()
                BufferedInputStream(stream).use { extractPreservableJpegMetadata(it) }
            }
        }
    }

    /** Decodes an encrypted image directly from its authenticated stream. No plaintext file is
     * created in cache, external storage, or the MediaStore. */
    fun decodeImagePreview(master: ByteArray, item: SecureItem, maximumDimension: Int = 3072): Bitmap? {
        require(!item.isVideo) { "Video frames use the media data source" }
        if (Build.VERSION.SDK_INT >= 29 && com.danyal.vaultgallery.isRawImage(item.mimeType, item.name)) {
            val bytes = decryptBytes(master, item, maxBytes = 128 * 1024 * 1024)
            return try {
                val source = android.graphics.ImageDecoder.createSource(java.nio.ByteBuffer.wrap(bytes))
                android.graphics.ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                    decoder.allocator = android.graphics.ImageDecoder.ALLOCATOR_SOFTWARE
                    val longest = maxOf(info.size.width, info.size.height).coerceAtLeast(1)
                    if (longest > maximumDimension) {
                        val scale = maximumDimension.toFloat() / longest
                        decoder.setTargetSize(
                            (info.size.width * scale).toInt().coerceAtLeast(1),
                            (info.size.height * scale).toInt().coerceAtLeast(1),
                        )
                    }
                }
            } finally {
                bytes.fill(0)
            }
        }
        if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) {
            val source = lockedFile(item)
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(source.absolutePath, bounds)
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val safetyIssue = imageDecodeSafetyIssue(bounds.outWidth, bounds.outHeight, source.length(), bounds.outMimeType ?: item.mimeType)
            require(safetyIssue == null) { safetyIssue ?: "Unsafe image payload" }
            var sample = 1
            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maximumDimension) sample *= 2
            return BitmapFactory.decodeFile(source.absolutePath, BitmapFactory.Options().apply {
                inSampleSize = sample
                inPreferredConfig = Bitmap.Config.ARGB_8888
            })
        }
        fun decode(options: BitmapFactory.Options): Bitmap? {
            val source = dataSourceFactory(master, item).createDataSource()
            return DataSourceInputStream(source, DataSpec(Uri.EMPTY)).use { input ->
                BufferedInputStream(input).use { BitmapFactory.decodeStream(it, null, options) }
            }
        }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        decode(bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val safetyIssue = imageDecodeSafetyIssue(bounds.outWidth, bounds.outHeight, item.sizeBytes, bounds.outMimeType ?: item.mimeType)
        require(safetyIssue == null) { safetyIssue ?: "Unsafe image payload" }
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > maximumDimension) sample *= 2
        return decode(BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        })
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
            readPlain(master, item) { bytes, count -> stream.write(bytes, 0, count) }
            stream.flush()
        }
        return output
    }

    /** Restores an encrypted item to shared storage, optionally into an explicitly selected album. */
    fun exportToGallery(
        master: ByteArray,
        resolver: ContentResolver,
        item: SecureItem,
        albumOverride: String? = null,
        onProgress: ((Long, Long) -> Unit)? = null,
        onDestinationCreated: ((Uri) -> Unit)? = null,
        displayNameOverride: String? = null,
    ): Uri {
        val collection = if (item.isVideo) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val safeAlbum = (albumOverride?.takeIf { it.isNotBlank() } ?: item.albumName)
            .replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "Imported" }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayNameOverride?.takeIf { it.isNotBlank() } ?: item.name)
            put(MediaStore.MediaColumns.MIME_TYPE, item.mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/$safeAlbum/")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            put(MediaStore.MediaColumns.DATE_ADDED, System.currentTimeMillis() / 1000L)
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
            if (item.dateTakenMs > 0) put(MediaStore.Images.ImageColumns.DATE_TAKEN, item.dateTakenMs)
        }
        val uri = resolver.insert(collection, values) ?: throw IOException("Could not create the destination media item")
        onDestinationCreated?.invoke(uri)
        try {
            resolver.openOutputStream(uri, "w")?.use { output ->
                var transferred = 0L
                readPlain(master, item) { bytes, count ->
                    output.write(bytes, 0, count)
                    transferred += count
                    onProgress?.invoke(transferred, item.sizeBytes)
                }
                output.flush()
            } ?: throw IOException("Could not open the destination media item")
            resolver.update(uri, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            return uri
        } catch (error: Throwable) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    fun dataSourceFactory(master: ByteArray, item: SecureItem): androidx.media3.datasource.DataSource.Factory {
        if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) {
            val file = lockedFile(item)
            return androidx.media3.datasource.DataSource.Factory { PlainFileDataSource(file) }
        }
        val keyCopy = fileKey(master, item.id)
        val file = encryptedFile(item)
        val streaming = isStreamingFile(file)
        return androidx.media3.datasource.DataSource.Factory {
            if (streaming) TinkStreamingDataSource(file, keyCopy.copyOf(), item.id)
            else LegacySecureChunkDataSource(file, keyCopy.copyOf())
        }
    }

    fun cleanupShareCache() {
        val now = System.currentTimeMillis()
        File(context.cacheDir, "secure-share").listFiles { file -> isAbandonedSecureTemp(file.lastModified(), now) }
            ?.forEach { it.delete() }
        mediaRoot.listFiles { file -> file.extension == "tmp" && isAbandonedSecureTemp(file.lastModified(), now) }
            ?.forEach { it.delete() }
    }

    /** Transactionally upgrades legacy custom-chunk encrypted media to Tink Streaming AEAD.
     * Plaintext only flows through memory; the original encrypted file is retained until the new
     * stream has authenticated and its plaintext length has been verified. */
    fun migrateLegacyEncryption(master: ByteArray, onProgress: ((Int, Int) -> Unit)? = null): Int {
        val legacyItems = list(master, includeTrashed = true).filter { item ->
            item.storagePolicy == SecureStoragePolicy.ENCRYPTED && encryptedFile(item).let { it.exists() && !isStreamingFile(it) }
        }
        var migrated = 0
        legacyItems.forEachIndexed { index, item ->
            val source = encryptedFile(item)
            val replacement = File(mediaRoot, "${item.id}.streaming.tmp")
            val backup = File(mediaRoot, "${item.id}.legacy.backup")
            replacement.delete()
            backup.delete()
            val legacyKey = fileKey(master, item.id)
            try {
                val legacyDataSource = LegacySecureChunkDataSource(source, legacyKey)
                DataSourceInputStream(
                    legacyDataSource,
                    DataSpec.Builder().setUri(Uri.fromFile(source)).build(),
                ).use { plain ->
                    plain.open()
                    encryptStream(master, item.id, BufferedInputStream(plain), replacement)
                }
                verifyFile(master, item.id, replacement)
                require(plaintextLength(master, item, replacement) == item.sizeBytes || item.sizeBytes < 0L) {
                    "Secure migration length mismatch for ${item.name}"
                }
                require(source.renameTo(backup)) { "Could not preserve legacy secure file" }
                if (!replacement.renameTo(source)) {
                    backup.renameTo(source)
                    throw IOException("Could not commit Tink secure stream")
                }
                backup.delete()
                migrated++
            } finally {
                legacyKey.fill(0)
                replacement.delete()
                if (backup.exists() && !source.exists()) backup.renameTo(source)
                onProgress?.invoke(index + 1, legacyItems.size)
            }
        }
        return migrated
    }

    private fun plaintextLength(master: ByteArray, item: SecureItem, source: File): Long {
        var length = 0L
        decryptStream(master, item.id, source) { _, count -> length += count }
        return length
    }

    /**
     * Temporarily wraps the already-unlocked master key for a user-requested background transfer.
     * The worker removes this envelope in a finally block; stale envelopes also expire on read.
     */
    fun createTransferSession(master: ByteArray): String {
        require(master.size == 32) { "Vault is locked" }
        val token = UUID.randomUUID().toString()
        val created = System.currentTimeMillis()
        val payload = ByteBuffer.allocate(8 + master.size).putLong(created).put(master).array()
        val envelope = try {
            sealWithProviderIv(deviceKey(), payload, transferAad(token))
        } finally {
            payload.fill(0)
        }
        val sessionRoot = File(root, "transfer-sessions").apply { mkdirs() }
        File(sessionRoot, "$token.key").writeBytes(envelope)
        envelope.fill(0)
        return token
    }

    fun openTransferSession(token: String): ByteArray {
        require(UUID.fromString(token).toString() == token) { "Invalid secure transfer authorization" }
        val file = File(File(root, "transfer-sessions"), "$token.key")
        val payload = open(deviceKey(), file.readBytes(), transferAad(token))
        try {
            require(payload.size == 40) { "Invalid secure transfer authorization" }
            val buffer = ByteBuffer.wrap(payload)
            val created = buffer.long
            require(System.currentTimeMillis() - created <= TRANSFER_SESSION_LIFETIME_MS) { "Secure transfer authorization expired" }
            return ByteArray(32).also(buffer::get)
        } finally {
            payload.fill(0)
        }
    }

    fun destroyTransferSession(token: String) {
        runCatching {
            if (UUID.fromString(token).toString() == token) File(File(root, "transfer-sessions"), "$token.key").delete()
        }
    }

    fun reset(master: ByteArray) {
        master.fill(0)
        synchronized(metadataLock) {
            metadataDb?.close()
            metadataDb = null
            metadataPassphrase?.fill(0)
            metadataPassphrase = null
            context.deleteDatabase(metadataDatabaseName)
        }
        root.deleteRecursively()
        prefs.edit().clear().apply()
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(alias)) keyStore.deleteEntry(alias)
        if (keyStore.containsAlias(biometricAlias)) keyStore.deleteEntry(biometricAlias)
        if (keyStore.containsAlias(legacyBiometricAlias)) keyStore.deleteEntry(legacyBiometricAlias)
        mediaRoot.mkdirs()
        lockedRoot.mkdirs()
        thumbnailRoot.mkdirs()
    }

    private fun encryptedFile(item: SecureItem) = File(mediaRoot, "${item.id}.vg")
    private fun lockedFile(item: SecureItem) = File(lockedRoot, "${item.id}.media")
    internal fun storageFile(item: SecureItem) = if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) encryptedFile(item) else lockedFile(item)
    private fun thumbnailFile(item: SecureItem) = File(thumbnailRoot, "${item.id}.thumb")
    private fun contactSheetFile(item: SecureItem) = File(thumbnailRoot, "${item.id}.frames")

    fun streamPlain(master: ByteArray, item: SecureItem, consumer: (ByteArray, Int) -> Unit) = readPlain(master, item, consumer)

    /** Authenticates every encrypted stream and checks every plaintext length without creating a
     * plaintext file. Suspicious/orphan files are reported and never deleted automatically. */
    fun scanIntegrity(master: ByteArray, onProgress: ((Int, Int) -> Unit)? = null): VaultIntegrityReport {
        require(master.size == 32) { "Vault is locked" }
        val items = list(master, includeTrashed = true)
        val issues = ArrayList<VaultIntegrityIssue>()
        var verified = 0
        items.forEachIndexed { index, item ->
            val storage = storageFile(item)
            if (!storage.isFile) {
                issues += VaultIntegrityIssue(item.id, item.name, "Storage file is missing")
            } else {
                runCatching {
                    var bytes = 0L
                    readPlain(master, item) { _, count -> bytes += count }
                    require(bytes == item.sizeBytes) { "Expected ${item.sizeBytes} bytes but authenticated $bytes" }
                }.onSuccess { verified++ }
                    .onFailure { error ->
                        issues += VaultIntegrityIssue(item.id, item.name, error.message ?: "Authentication failed")
                    }
            }
            onProgress?.invoke(index + 1, items.size)
        }
        val knownIds = items.mapTo(HashSet()) { it.id }
        val orphanFiles = buildList {
            mediaRoot.listFiles().orEmpty().filter { file ->
                file.isFile && file.extension == "vg" && file.nameWithoutExtension !in knownIds
            }.forEach { add("encrypted-media/${it.name}") }
            lockedRoot.listFiles().orEmpty().filter { file ->
                file.isFile && file.extension == "media" && file.nameWithoutExtension !in knownIds
            }.forEach { add("locked-media/${it.name}") }
        }.sorted()
        return VaultIntegrityReport(items.size, verified, issues, orphanFiles, System.currentTimeMillis())
    }

    /** Safe repair only removes abandoned partial files and regenerates authenticated metadata.
     * It deliberately preserves every orphan for later salvage instead of guessing and deleting. */
    fun repairSafe(master: ByteArray): Int {
        require(master.size == 32) { "Vault is locked" }
        val now = System.currentTimeMillis()
        var removedPartials = 0
        listOf(mediaRoot, lockedRoot, thumbnailRoot).forEach { directory ->
            directory.listFiles().orEmpty().filter { file ->
                file.isFile && (file.name.endsWith(".tmp") || file.name.endsWith(".migration")) &&
                    isAbandonedSecureTemp(file.lastModified(), now)
            }.forEach { partial -> if (partial.delete()) removedPartials++ }
        }
        snapshotPortableIndex(master)
        return removedPartials
    }

    fun readAt(master: ByteArray, item: SecureItem, offset: Long, requested: Int, destination: ByteArray): Int {
        if (requested <= 0 || offset >= item.sizeBytes) return -1
        val count = minOf(requested, destination.size, (item.sizeBytes - offset).toInt())
        if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) {
            return RandomAccessFile(lockedFile(item), "r").use { file -> file.seek(offset); file.read(destination, 0, count) }
        }
        val rangeKey = fileKey(master, item.id)
        val encrypted = encryptedFile(item)
        val source = if (isStreamingFile(encrypted)) {
            TinkStreamingDataSource(encrypted, rangeKey, item.id)
        } else {
            LegacySecureChunkDataSource(encrypted, rangeKey)
        }
        return try {
            source.open(DataSpec.Builder().setUri(Uri.EMPTY).setPosition(offset).setLength(count.toLong()).build())
            source.read(destination, 0, count)
        } finally { source.close(); rangeKey.fill(0) }
    }

    private fun readPlain(master: ByteArray, item: SecureItem, consumer: (ByteArray, Int) -> Unit) {
        if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) {
            decryptStream(master, item.id, encryptedFile(item), consumer)
        } else {
            BufferedInputStream(FileInputStream(lockedFile(item))).use { input ->
                val buffer = ByteArray(1024 * 1024)
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    consumer(buffer, count)
                }
            }
        }
    }

    fun loadOrCreateVideoContactSheet(master: ByteArray, item: SecureItem, frameCount: Int = 12): Bitmap? {
        require(item.isVideo)
        val cache = contactSheetFile(item)
        if (cache.exists()) {
            val encoded = cache.readBytes()
            val jpeg = if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) {
                runCatching { open(SecretKeySpec(master, "AES"), encoded, "vault-frames-v1:${item.id}".toByteArray()) }.getOrNull()
            } else encoded
            if (jpeg != null) return BitmapFactory.decodeByteArray(jpeg, 0, jpeg.size).also { if (jpeg !== encoded) jpeg.fill(0) }
        }
        val frames = ArrayList<Bitmap>()
        val retriever = MediaMetadataRetriever()
        try {
            if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) retriever.setDataSource(lockedFile(item).absolutePath)
            else retriever.setDataSource(context, SecureMediaProvider.uri(context, item))
            val duration = item.durationMs.coerceAtLeast(1L)
            repeat(frameCount.coerceIn(8, 16)) { index ->
                retriever.getFrameAtTime(duration * 1_000L * index / (frameCount - 1).coerceAtLeast(1), MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                    val height = 96
                    val width = (frame.width * (height.toFloat() / frame.height.coerceAtLeast(1))).toInt().coerceIn(96, 192)
                    frames += Bitmap.createScaledBitmap(frame, width, height, true).also { if (it !== frame) frame.recycle() }
                }
            }
        } finally {
            retriever.release()
        }
        if (frames.isEmpty()) return null
        val sheet = Bitmap.createBitmap(frames.sumOf(Bitmap::getWidth), 96, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(sheet); var x = 0f
        frames.forEach { frame -> canvas.drawBitmap(frame, x, 0f, null); x += frame.width; frame.recycle() }
        val output = ByteArrayOutputStream(); sheet.compress(Bitmap.CompressFormat.JPEG, 82, output)
        val jpeg = output.toByteArray()
        val stored = if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) seal(SecretKeySpec(master, "AES"), jpeg, "vault-frames-v1:${item.id}".toByteArray()) else jpeg
        cache.writeBytes(stored)
        if (stored !== jpeg) stored.fill(0); jpeg.fill(0)
        return sheet
    }

    private fun writeIndex(master: ByteArray, items: List<SecureItem>) {
        synchronized(metadataLock) {
            metadataDatabase(master).items().replace(items.map(SecureItem::toEntity))
            writePortableIndex(master, items)
        }
    }

    private fun snapshotPortableIndex(master: ByteArray) {
        synchronized(metadataLock) {
            val items = metadataDatabase(master).items().all().map(SecureItemEntity::toItem)
            writePortableIndex(master, items)
        }
    }

    private fun writePortableIndex(master: ByteArray, items: List<SecureItem>) {
        val json = JSONArray()
        items.forEach { item ->
            json.put(JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("mime", item.mimeType)
                put("date", item.dateTakenMs)
                put("size", item.sizeBytes)
                put("album", item.albumName)
                put("width", item.width)
                put("height", item.height)
                put("duration", item.durationMs)
                put("favourite", item.isFavourite)
                put("trashed", item.isTrashed)
                put("trashed_at", item.trashedAtMs)
                put("tag", item.tag)
                item.latitude?.let { put("latitude", it) }
                item.longitude?.let { put("longitude", it) }
                put("storage_policy", item.storagePolicy.name)
                put("rating", item.rating)
            })
        }
        val plain = json.toString().toByteArray(Charsets.UTF_8)
        val envelope = try {
            seal(SecretKeySpec(master, "AES"), plain, INDEX_AAD)
        } finally {
            plain.fill(0)
        }
        try {
            atomicWrite(indexFile, envelope)
        } finally {
            envelope.fill(0)
        }
    }

    private fun SecureItem.toPortableJson() = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("mime", mimeType)
        put("date", dateTakenMs)
        put("size", sizeBytes)
        put("album", albumName)
        put("width", width)
        put("height", height)
        put("duration", durationMs)
        put("favourite", isFavourite)
        put("trashed", isTrashed)
        put("trashed_at", trashedAtMs)
        put("tag", tag)
        latitude?.let { put("latitude", it) }
        longitude?.let { put("longitude", it) }
        put("storage_policy", storagePolicy.name)
        put("rating", rating)
    }

    private fun JSONObject.toSecureItem(): SecureItem {
        val id = getString("id")
        require(UUID.fromString(id).toString() == id) { "Invalid media identifier in backup" }
        val mime = getString("mime")
        require(mime.startsWith("image/") || mime.startsWith("video/")) { "Unsupported media type in backup" }
        val size = getLong("size")
        require(size in 0..MAX_BACKUP_ITEM_BYTES) { "Invalid media size in backup" }
        return SecureItem(
            id = id,
            name = getString("name").take(MAX_BACKUP_TEXT_LENGTH).ifBlank { "Restored media" },
            mimeType = mime.take(MAX_BACKUP_TEXT_LENGTH),
            dateTakenMs = getLong("date"),
            sizeBytes = size,
            albumName = optString("album", "Restored").take(MAX_BACKUP_TEXT_LENGTH).ifBlank { "Restored" },
            width = optInt("width", 0).coerceAtLeast(0),
            height = optInt("height", 0).coerceAtLeast(0),
            durationMs = optLong("duration", 0L).coerceAtLeast(0L),
            isFavourite = optBoolean("favourite", false),
            isTrashed = optBoolean("trashed", false),
            trashedAtMs = optLong("trashed_at", 0L).coerceAtLeast(0L),
            tag = optString("tag").take(MAX_BACKUP_TEXT_LENGTH),
            latitude = optDouble("latitude").takeUnless(Double::isNaN),
            longitude = optDouble("longitude").takeUnless(Double::isNaN),
            storagePolicy = runCatching { SecureStoragePolicy.valueOf(optString("storage_policy")) }
                .getOrDefault(SecureStoragePolicy.ENCRYPTED),
            rating = optInt("rating", 0).coerceIn(0, 5),
        )
    }

    private fun unlockPortable(secret: CharArray): ByteArray {
        val json = runCatching { JSONObject(portableAuthFile.readText(Charsets.UTF_8)) }
            .getOrElse { throw SecurityException("The persistent vault recovery header is damaged") }
        require(json.optInt("version", 0) == PORTABLE_AUTH_VERSION) { "Unsupported vault recovery header" }
        val salt = unb64(json.getString("salt"))
        val envelope = unb64(json.getString("pin_envelope"))
        val time = json.optInt("argon_time", ARGON_TIME).coerceAtLeast(1)
        val memory = json.optInt("argon_memory_kib", ARGON_MEMORY_KIB).coerceAtLeast(8 * 1024)
        val pinKey = derive(secret, salt, time, memory)
        return try {
            open(SecretKeySpec(pinKey, "AES"), envelope, PORTABLE_AUTH_AAD)
        } catch (_: Exception) {
            throw SecurityException("Incorrect PIN or passphrase")
        } finally {
            pinKey.fill(0)
            envelope.fill(0)
            salt.fill(0)
        }
    }

    private fun writePortableAuth(secret: CharArray, existingSalt: ByteArray?, master: ByteArray) {
        val salt = existingSalt?.copyOf() ?: randomBytes(16)
        val pinKey = derive(secret, salt, ARGON_TIME, ARGON_MEMORY_KIB)
        val envelope = try {
            seal(SecretKeySpec(pinKey, "AES"), master, PORTABLE_AUTH_AAD)
        } finally {
            pinKey.fill(0)
        }
        try {
            val json = JSONObject().apply {
                put("version", PORTABLE_AUTH_VERSION)
                put("salt", b64(salt))
                put("pin_envelope", b64(envelope))
                put("argon_time", ARGON_TIME)
                put("argon_memory_kib", ARGON_MEMORY_KIB)
                put("created_by", "Vault Gallery")
            }.toString()
            atomicWrite(portableAuthFile, json.toByteArray(Charsets.UTF_8))
        } finally {
            salt.fill(0)
            envelope.fill(0)
        }
    }

    private fun atomicWrite(destination: File, bytes: ByteArray) {
        destination.parentFile?.mkdirs()
        val temp = File(destination.parentFile, "${destination.name}.tmp")
        FileOutputStream(temp).use { output ->
            output.write(bytes)
            output.flush()
            output.fd.sync()
        }
        if (destination.exists() && !destination.delete()) throw IOException("Could not replace ${destination.name}")
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }

    private fun metadataDatabase(master: ByteArray): SecureMetadataDatabase {
        metadataDb?.let { return it }
        return synchronized(metadataLock) {
            metadataDb?.let { return@synchronized it }
            val passphrase = fileKey(master, "metadata-database")
            try {
                val opened = SecureMetadataDatabase.open(context, passphrase, metadataDatabaseName)
                // Force SQLCipher to authenticate before releasing the derived passphrase.
                opened.openHelper.writableDatabase
                if (opened.items().all().isEmpty() && indexFile.exists()) {
                    val legacy = readLegacyIndex(master)
                    opened.items().replace(legacy.map(SecureItem::toEntity))
                }
                opened.also {
                    metadataPassphrase?.fill(0)
                    metadataPassphrase = passphrase
                    metadataDb = it
                }
            } catch (error: Throwable) {
                passphrase.fill(0)
                throw error
            }
        }
    }

    fun closeMetadata() {
        synchronized(metadataLock) {
            metadataDb?.close()
            metadataDb = null
            metadataPassphrase?.fill(0)
            metadataPassphrase = null
        }
    }

    private fun encryptStream(
        master: ByteArray,
        objectId: String,
        input: InputStream,
        destination: File,
        onProgress: ((Long) -> Unit)? = null,
    ) {
        val fileKey = fileKey(master, objectId)
        try {
            StreamingAeadConfig.register()
            val keyset = KeysetHandle.generateNew(StreamingAeadKeyTemplates.AES256_GCM_HKDF_1MB)
            val encryptedKeyset = ByteArrayOutputStream().use { encoded ->
                keyset.writeWithAssociatedData(
                    BinaryKeysetWriter.withOutputStream(encoded),
                    AesGcmJce(fileKey),
                    secureKeysetAad(objectId),
                )
                encoded.toByteArray()
            }
            DataOutputStream(BufferedOutputStream(FileOutputStream(destination))).use { fileOutput ->
                fileOutput.write(STREAM_MAGIC)
                fileOutput.writeInt(STREAM_FORMAT_VERSION)
                fileOutput.writeInt(encryptedKeyset.size)
                fileOutput.write(encryptedKeyset)
                val stream = keyset.getPrimitive(StreamingAead::class.java)
                    .newEncryptingStream(fileOutput, secureStreamAad(objectId))
                stream.use { encryptedOutput ->
                    val buffer = ByteArray(STREAM_COPY_BUFFER_SIZE)
                    var transferred = 0L
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        encryptedOutput.write(buffer, 0, count)
                        transferred += count
                        onProgress?.invoke(transferred)
                    }
                    buffer.fill(0)
                }
            }
            encryptedKeyset.fill(0)
        } finally {
            fileKey.fill(0)
        }
    }

    private fun decryptStream(master: ByteArray, objectId: String, source: File, consumer: (ByteArray, Int) -> Unit) {
        val fileKey = fileKey(master, objectId)
        try {
            if (isStreamingFile(source)) decryptTinkStream(fileKey, objectId, source, consumer)
            else decryptLegacyStream(fileKey, source, consumer)
        } finally {
            fileKey.fill(0)
        }
    }

    private fun decryptTinkStream(
        fileKey: ByteArray,
        objectId: String,
        source: File,
        consumer: (ByteArray, Int) -> Unit,
    ) {
        StreamingAeadConfig.register()
        FileInputStream(source).use { raw ->
            val header = DataInputStream(BufferedInputStream(raw))
            val magic = ByteArray(STREAM_MAGIC.size).also(header::readFully)
            require(magic.contentEquals(STREAM_MAGIC)) { "Invalid secure media" }
            require(header.readInt() == STREAM_FORMAT_VERSION) { "Unsupported secure media version" }
            val keysetLength = header.readInt()
            require(keysetLength in 1..MAX_ENCRYPTED_KEYSET_SIZE) { "Invalid secure key header" }
            val encryptedKeyset = ByteArray(keysetLength).also(header::readFully)
            val keyset = try {
                KeysetHandle.readWithAssociatedData(
                    BinaryKeysetReader.withBytes(encryptedKeyset),
                    AesGcmJce(fileKey),
                    secureKeysetAad(objectId),
                )
            } finally {
                encryptedKeyset.fill(0)
            }
            keyset.getPrimitive(StreamingAead::class.java)
                .newDecryptingStream(header, secureStreamAad(objectId)).use { plain ->
                    val buffer = ByteArray(STREAM_COPY_BUFFER_SIZE)
                    while (true) {
                        val count = plain.read(buffer)
                        if (count < 0) break
                        consumer(buffer, count)
                    }
                    buffer.fill(0)
                }
        }
    }

    /** Compatibility reader for media written by releases before the Tink Streaming AEAD format.
     * It is intentionally read-only: every new write and policy conversion uses the Tink format. */
    private fun decryptLegacyStream(fileKey: ByteArray, source: File, consumer: (ByteArray, Int) -> Unit) {
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
                val plain = InsecureNonceAesGcmJce(fileKey)
                    .decrypt(nonce(prefix, index), encrypted, chunkAad(index, count))
                consumer(plain, plain.size)
                plain.fill(0)
                encrypted.fill(0)
                index++
            }
        }
    }

    private fun isStreamingFile(source: File): Boolean = RandomAccessFile(source, "r").use { input ->
        if (input.length() < STREAM_MAGIC.size) false
        else ByteArray(STREAM_MAGIC.size).also(input::readFully).contentEquals(STREAM_MAGIC)
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

    private fun derive(
        secret: CharArray,
        salt: ByteArray,
        timeCost: Int = prefs.getInt("argon_time", ARGON_TIME),
        memoryCostKib: Int = prefs.getInt("argon_memory_kib", ARGON_MEMORY_KIB),
    ): ByteArray {
        val encoded = Charsets.UTF_8.encode(CharBuffer.wrap(secret))
        val password = ByteArray(encoded.remaining()).also(encoded::get)
        return try {
            Argon2Kt().hash(
                mode = Argon2Mode.ARGON2_ID,
                password = password,
                salt = salt,
                tCostInIterations = timeCost,
                mCostInKibibyte = memoryCostKib,
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

    private fun biometricKey(keyAlias: String = biometricAlias): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(keyAlias, null) as? SecretKey ?: throw SecurityException("Biometric key is unavailable")
    }

    private fun deleteKeyIfPresent(keyAlias: String) {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) keyStore.deleteEntry(keyAlias)
    }

    private fun seal(key: SecretKey, plain: ByteArray, aad: ByteArray): ByteArray {
        val rawKey = requireNotNull(key.encoded) { "Raw key material is required" }
        return AesGcmJce(rawKey).encrypt(plain, aad)
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
        key.encoded?.let { rawKey -> return AesGcmJce(rawKey).decrypt(envelope, aad) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, envelope.copyOfRange(0, 12)))
        cipher.updateAAD(aad)
        return cipher.doFinal(envelope, 12, envelope.size - 12)
    }

    private fun nonce(prefix: ByteArray, index: Int): ByteArray = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
    private fun transferAad(token: String) = "vault-transfer-v1:$token".toByteArray(Charsets.US_ASCII)
    private fun chunkAad(index: Int, count: Int) = ByteBuffer.allocate(MAGIC.size + 8).put(MAGIC).putInt(index).putInt(count).array()
    private fun randomBytes(size: Int) = ByteArray(size).also(random::nextBytes)
    private fun b64(bytes: ByteArray) = Base64.encodeToString(bytes, Base64.NO_WRAP)
    private fun unb64(value: String) = Base64.decode(value, Base64.NO_WRAP)

    companion object {
        @Suppress("DEPRECATION")
        fun persistentRoot(): File = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS),
            "VaultGallery/SecureVault",
        )

        fun hasPersistentStorageAccess(context: Context): Boolean = when {
            Build.VERSION.SDK_INT >= 30 -> Environment.isExternalStorageManager()
            Build.VERSION.SDK_INT >= 23 -> context.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
            else -> true
        }

        private fun migratePrivateVault(source: File, destination: File) {
            destination.mkdirs()
            if (!source.isDirectory) return
            source.walkTopDown().forEach { input ->
                val relative = input.relativeTo(source).path
                if (relative.isBlank()) return@forEach
                val output = File(destination, relative)
                if (input.isDirectory) {
                    output.mkdirs()
                } else if (!output.exists()) {
                    output.parentFile?.mkdirs()
                    val partial = File(output.parentFile, "${output.name}.migration")
                    input.inputStream().buffered().use { from ->
                        FileOutputStream(partial).buffered().use { to -> from.copyTo(to, 1024 * 1024) }
                    }
                    if (partial.length() != input.length()) {
                        partial.delete()
                        throw IOException("Persistent vault migration verification failed for ${input.name}")
                    }
                    if (!partial.renameTo(output)) {
                        partial.copyTo(output, overwrite = false)
                        partial.delete()
                    }
                } else if (output.length() != input.length()) {
                    // Never overwrite either copy when a prior migration and the private fallback
                    // disagree. Preserve the private file under a recovery name for manual repair.
                    val recovered = File(output.parentFile, "${output.name}.recovered-${System.currentTimeMillis()}")
                    input.copyTo(recovered, overwrite = false)
                }
            }
        }

        private val MAGIC = "VLTGAL01".toByteArray(Charsets.US_ASCII)
        private val STREAM_MAGIC = "VLTGSA02".toByteArray(Charsets.US_ASCII)
        private val AUTH_AAD = "vault-auth-v1".toByteArray()
        private val PORTABLE_AUTH_AAD = "vault-portable-auth-v1".toByteArray()
        private val DEVICE_AAD = "vault-device-v1".toByteArray()
        private val INDEX_AAD = "vault-index-v1".toByteArray()
        private val BIOMETRIC_AAD = "vault-biometric-v1".toByteArray()
        private const val BIOMETRIC_WRAPPED_MASTER_V1 = "biometric_wrapped_master"
        private const val BIOMETRIC_WRAPPED_MASTER_V2 = "biometric_wrapped_master_v2"
        private const val BIOMETRIC_CBC_IV_SIZE = 16
        private const val GCM_NONCE_SIZE = 12
        private val RECOVERY_KEY_AAD = "vault-recovery-key-v1".toByteArray()
        private val BACKUP_MAGIC = "VLTGBK01".toByteArray(Charsets.US_ASCII)
        private val BACKUP_KEYSET_AAD = "vault-backup-keyset-v1".toByteArray()
        private val BACKUP_STREAM_AAD = "vault-backup-stream-v1".toByteArray()
        private const val FORMAT_VERSION = 1
        private const val STREAM_FORMAT_VERSION = 2
        private const val MAX_ENCRYPTED_KEYSET_SIZE = 64 * 1024
        private const val STREAM_COPY_BUFFER_SIZE = 256 * 1024
        private const val CHUNK_SIZE = 1024 * 1024
        private const val GCM_TAG_SIZE = 16
        private const val ARGON_TIME = 3
        private const val ARGON_MEMORY_KIB = 65_536
        private const val TRASH_RETENTION_MS = 30L * 24 * 60 * 60 * 1000
        private const val TRANSFER_SESSION_LIFETIME_MS = 24L * 60 * 60 * 1000
        private const val PORTABLE_AUTH_VERSION = 1
        private const val RECOVERY_KEY_VERSION = 1
        private const val BACKUP_FORMAT_VERSION = 1
        private const val BACKUP_MANIFEST_ENTRY = "manifest.json"
        private const val BACKUP_MEDIA_PREFIX = "media/"
        private const val MAX_BACKUP_MANIFEST_SIZE = 16 * 1024 * 1024
        private const val MAX_BACKUP_ITEMS = 100_000
        private const val MAX_BACKUP_ITEM_BYTES = 1L shl 42
        private const val MAX_BACKUP_TEXT_LENGTH = 512
        private const val BACKUP_FREE_SPACE_RESERVE = 64L * 1024 * 1024
    }
}

private class ExactSizeInputStream(
    private val source: InputStream,
    private val expectedBytes: Long,
    private val onProgress: (Long) -> Unit,
) : InputStream() {
    private var count = 0L

    override fun read(): Int {
        val value = source.read()
        if (value >= 0) record(1)
        return value
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val read = source.read(buffer, offset, length)
        if (read > 0) record(read)
        return read
    }

    private fun record(bytes: Int) {
        count += bytes
        require(count <= expectedBytes) { "Backup media is larger than declared" }
        onProgress(count)
    }

    fun verifyComplete() {
        require(count == expectedBytes) { "Backup media is incomplete ($count of $expectedBytes bytes)" }
    }
}

private fun InputStream.readLimited(maxBytes: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(maxBytes, 64 * 1024))
    val buffer = ByteArray(32 * 1024)
    var total = 0
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        total += count
        require(total <= maxBytes) { "Backup metadata is too large" }
        output.write(buffer, 0, count)
    }
    buffer.fill(0)
    return output.toByteArray()
}

private data class ImportMetadata(
    val name: String,
    val size: Long,
    val album: String,
    val width: Int,
    val height: Int,
    val duration: Long,
    val dateTaken: Long,
)

private fun secureKeysetAad(objectId: String): ByteArray =
    "vault-stream-keyset-v2:$objectId".toByteArray(Charsets.UTF_8)

private fun secureStreamAad(objectId: String): ByteArray =
    "vault-stream-media-v2:$objectId".toByteArray(Charsets.UTF_8)

private fun android.database.Cursor.stringOrNull(column: String): String? {
    val index = getColumnIndex(column)
    return if (index >= 0 && !isNull(index)) getString(index) else null
}

private fun android.database.Cursor.longOrNull(column: String): Long? {
    val index = getColumnIndex(column)
    return if (index >= 0 && !isNull(index)) getLong(index) else null
}

private data class ChunkRecord(val offset: Long, val plainStart: Long, val plainLength: Int)

private class PlainFileDataSource(private val file: File) : BaseDataSource(false) {
    private var raf: RandomAccessFile? = null
    private var remaining = 0L
    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        val input = RandomAccessFile(file, "r").also { raf = it }
        if (dataSpec.position > input.length()) throw IOException("Position outside locked media")
        input.seek(dataSpec.position)
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) input.length() - dataSpec.position else minOf(dataSpec.length, input.length() - dataSpec.position)
        transferStarted(dataSpec)
        return remaining
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining <= 0) return C.RESULT_END_OF_INPUT
        val count = raf?.read(buffer, offset, minOf(length.toLong(), remaining).toInt()) ?: C.RESULT_END_OF_INPUT
        if (count > 0) { remaining -= count; bytesTransferred(count) }
        return count
    }
    override fun getUri(): Uri = Uri.fromFile(file)
    override fun close() { raf?.close(); raf = null; transferEnded() }
}

private class TinkStreamingDataSource(
    private val file: File,
    private val fileKey: ByteArray,
    private val objectId: String,
) : BaseDataSource(false) {
    private var channel: SeekableByteChannel? = null
    private var remaining = 0L

    override fun open(dataSpec: DataSpec): Long {
        transferInitializing(dataSpec)
        StreamingAeadConfig.register()
        val header = RandomAccessFile(file, "r")
        val encryptedKeyset: ByteArray
        val payloadOffset: Long
        try {
            val magic = ByteArray(8).also(header::readFully)
            if (!magic.contentEquals("VLTGSA02".toByteArray(Charsets.US_ASCII))) throw IOException("Invalid secure video")
            if (header.readInt() != 2) throw IOException("Unsupported secure video")
            val keysetLength = header.readInt()
            if (keysetLength !in 1..64 * 1024) throw IOException("Invalid secure key header")
            encryptedKeyset = ByteArray(keysetLength).also(header::readFully)
            payloadOffset = header.filePointer
        } finally {
            header.close()
        }
        val handle = try {
            KeysetHandle.readWithAssociatedData(
                BinaryKeysetReader.withBytes(encryptedKeyset),
                AesGcmJce(fileKey),
                secureKeysetAad(objectId),
            )
        } finally {
            encryptedKeyset.fill(0)
        }
        val raw = java.nio.channels.FileChannel.open(file.toPath(), StandardOpenOption.READ)
        val offset = OffsetSeekableByteChannel(raw, payloadOffset)
        val decrypted = try {
            handle.getPrimitive(StreamingAead::class.java)
                .newSeekableDecryptingChannel(offset, secureStreamAad(objectId))
        } catch (error: Throwable) {
            offset.close()
            throw error
        }
        val size = decrypted.size()
        if (dataSpec.position > size) {
            decrypted.close()
            throw IOException("Position outside secure media")
        }
        decrypted.position(dataSpec.position)
        channel = decrypted
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) size - dataSpec.position
        else minOf(dataSpec.length, size - dataSpec.position)
        transferStarted(dataSpec)
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (remaining <= 0L) return C.RESULT_END_OF_INPUT
        val requested = minOf(length.toLong(), remaining).toInt()
        val count = channel?.read(ByteBuffer.wrap(buffer, offset, requested)) ?: C.RESULT_END_OF_INPUT
        if (count < 0) return C.RESULT_END_OF_INPUT
        remaining -= count
        bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri = Uri.fromFile(file)

    override fun close() {
        channel?.close()
        channel = null
        remaining = 0L
        transferEnded()
    }
}

private class OffsetSeekableByteChannel(
    private val delegate: SeekableByteChannel,
    private val offset: Long,
) : SeekableByteChannel {
    override fun read(dst: ByteBuffer): Int = delegate.read(dst)
    override fun write(src: ByteBuffer): Int = throw UnsupportedOperationException("Read only")
    override fun position(): Long = delegate.position() - offset
    override fun position(newPosition: Long): SeekableByteChannel {
        require(newPosition >= 0) { "Negative media position" }
        delegate.position(offset + newPosition)
        return this
    }
    override fun size(): Long = (delegate.size() - offset).coerceAtLeast(0L)
    override fun truncate(size: Long): SeekableByteChannel = throw UnsupportedOperationException("Read only")
    override fun isOpen(): Boolean = delegate.isOpen
    override fun close() = delegate.close()
}

private class LegacySecureChunkDataSource(private val file: File, private val master: ByteArray) : BaseDataSource(false) {
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
        // Media3 reuses and reopens a DataSource instance after a seek. Erasing this instance's
        // file key here made the first pass play correctly, then every random-access reopen fail
        // GCM authentication with AEADBadTagException. The factory owns an isolated file-key copy
        // for this source, so it must remain valid for the complete MediaSource lifetime.
        transferEnded()
    }

    private fun decrypt(index: Int) {
        val input = raf ?: throw IOException("Secure source is closed")
        val record = records[index]
        input.seek(record.offset)
        val encrypted = ByteArray(record.plainLength + 16).also(input::readFully)
        val nonce = ByteBuffer.allocate(12).put(prefix).putInt(index).array()
        val aad = ByteBuffer.allocate(16).put("VLTGAL01".toByteArray()).putInt(index).putInt(record.plainLength).array()
        cachedChunk.fill(0)
        cachedChunk = InsecureNonceAesGcmJce(master).decrypt(nonce, encrypted, aad)
        cachedIndex = index
        encrypted.fill(0)
    }
}
