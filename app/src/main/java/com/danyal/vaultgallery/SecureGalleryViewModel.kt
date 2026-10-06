package com.danyal.vaultgallery

import android.app.Application
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.DataSource
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.danyal.vaultgallery.core.GalleryLogic
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.security.SecureItem
import com.danyal.vaultgallery.security.SecureVault
import com.danyal.vaultgallery.security.SecureStoragePolicy
import com.danyal.vaultgallery.security.SecureUnlockPolicy
import com.danyal.vaultgallery.security.SecureAccessSession
import com.danyal.vaultgallery.security.VaultIntegrityReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.danyal.vaultgallery.search.InMemoryVectorIndex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.File
import java.io.ByteArrayOutputStream
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.security.MessageDigest
import javax.crypto.Cipher
import android.util.LruCache
import kotlin.coroutines.coroutineContext

data class SecureGalleryState(
    val configured: Boolean = false,
    val unlocked: Boolean = false,
    val busy: Boolean = false,
    val progress: String? = null,
    val items: List<SecureItem> = emptyList(),
    val trashItems: List<SecureItem> = emptyList(),
    val biometricEnabled: Boolean = false,
    val unlockPolicy: SecureUnlockPolicy = SecureUnlockPolicy.BIOMETRIC_OR_PIN,
    val lockTimeoutMs: Long = 0L,
    val lockOnBackground: Boolean = true,
    val lockOnScreenOff: Boolean = true,
    val pendingImports: Int = 0,
    val lastImportSucceeded: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
    val operationProgress: Int? = null,
    val operationCancellable: Boolean = false,
    val defaultStoragePolicy: SecureStoragePolicy = SecureStoragePolicy.LOCKED_ONLY,
    val encryptedCount: Int = 0,
    val lockedOnlyCount: Int = 0,
    val integrityScanning: Boolean = false,
    val integrityReport: VaultIntegrityReport? = null,
)

class SecureGalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = SecureVault(application)
    private val attemptPrefs = application.getSharedPreferences("secure-auth-attempts", 0)
    private val userPrefs = application.getSharedPreferences("secure-user-settings", 0)
    private var master: ByteArray? = null
    private var lockJob: Job? = null
    private var createJob: Job? = null
    private var externalAccessUntilMs: Long = 0L
    private var legacyMigrationStarted = false
    private var pendingUris = ArrayList<Uri>()
    private var pendingImportMove = false
    private var pendingImportAlbum: String? = null
    private val thumbnailCache = object : LruCache<String, Bitmap>(64) {}
    private val thumbnailGenerationSlots = Semaphore(3)
    private val _state = MutableStateFlow(
        SecureGalleryState(
            configured = vault.isConfigured(),
            biometricEnabled = vault.isBiometricEnabled(),
            unlockPolicy = runCatching {
                SecureUnlockPolicy.valueOf(userPrefs.getString("unlock_policy", null) ?: SecureUnlockPolicy.BIOMETRIC_OR_PIN.name)
            }.getOrDefault(SecureUnlockPolicy.BIOMETRIC_OR_PIN),
            lockTimeoutMs = userPrefs.getLong("lock_timeout_ms", 0L),
            lockOnBackground = userPrefs.getBoolean("lock_on_background", true),
            lockOnScreenOff = userPrefs.getBoolean("lock_on_screen_off", true),
            defaultStoragePolicy = runCatching { SecureStoragePolicy.valueOf(userPrefs.getString("default_storage_policy", null) ?: "LOCKED_ONLY") }.getOrDefault(SecureStoragePolicy.LOCKED_ONLY),
        ),
    )
    val state: StateFlow<SecureGalleryState> = _state.asStateFlow()

    fun queueImport(uris: List<Uri>, move: Boolean = false, targetAlbum: String? = null) {
        if (uris.isEmpty()) return
        pendingUris.addAll(uris.filterNot { it in pendingUris })
        pendingImportMove = pendingImportMove || move
        pendingImportAlbum = targetAlbum?.takeIf { it.isNotBlank() } ?: pendingImportAlbum
        _state.value = _state.value.copy(pendingImports = pendingUris.size, lastImportSucceeded = false)
        if (_state.value.unlocked) importPending()
    }

    fun setup(secret: String, confirmation: String) {
        if (secret != confirmation) {
            _state.value = _state.value.copy(error = "PIN or passphrase confirmation does not match")
            return
        }
        if (secret.length != 4 || secret.any { !it.isDigit() }) {
            _state.value = _state.value.copy(error = "Use exactly four digits")
            return
        }
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vault.setup(secret.toCharArray()) }
                .onSuccess { key ->
                    attemptPrefs.edit().clear().apply()
                    master = key
                    SecureAccessSession.install(key)
                    publishUnlocked(key)
                    importPending()
                }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "Secure Gallery setup failed") }
        }
    }

    fun unlock(secret: String) {
        val now = System.currentTimeMillis()
        val blockedUntil = attemptPrefs.getLong("blocked_until", 0L)
        if (now < blockedUntil) {
            val seconds = ((blockedUntil - now + 999) / 1000).coerceAtLeast(1)
            _state.value = _state.value.copy(error = "Try again in $seconds seconds")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.unlock(secret.toCharArray()) }
                .onSuccess { key ->
                    attemptPrefs.edit().clear().apply()
                    master = key
                    SecureAccessSession.install(key)
                    publishUnlocked(key)
                    importPending()
                }
                .onFailure {
                    val failures = (attemptPrefs.getInt("failures", 0) + 1).coerceAtMost(10)
                    val delaySeconds = (1L shl failures.coerceAtMost(5)).coerceAtMost(30L)
                    attemptPrefs.edit().putInt("failures", failures).putLong("blocked_until", System.currentTimeMillis() + delaySeconds * 1000).apply()
                    _state.value = _state.value.copy(unlocked = false, busy = false, error = "Incorrect PIN or passphrase. Try again in $delaySeconds seconds")
                }
        }
    }

    fun lock() {
        lockJob?.cancel()
        // Publish the locked UI before any best-effort cleanup. A cache/provider cleanup failure
        // must never leave decrypted thumbnails or the vault grid visible.
        val keyToClear = master
        master = null
        externalAccessUntilMs = 0L
        _state.value = _state.value.copy(unlocked = false, busy = false, progress = null, items = emptyList(), trashItems = emptyList(), error = null)
        runCatching { SecureAccessSession.clear() }
        runCatching { vault.closeMetadata() }
        runCatching { thumbnailCache.evictAll() }
        keyToClear?.fill(0)
    }

    fun onBackgrounded() {
        if (!_state.value.lockOnBackground) return
        lockJob?.cancel()
        // External read grants never extend the unlocked UI lifetime. Locking revokes them.
        val timeout = _state.value.lockTimeoutMs
        if (timeout == 0L) {
            lock()
        } else {
            lockJob = viewModelScope.launch {
                delay(timeout)
                lock()
            }
        }
    }

    fun authorizeExternalAccess(durationMs: Long) {
        externalAccessUntilMs = System.currentTimeMillis() + durationMs.coerceIn(60_000L, 60 * 60_000L)
    }

    fun onForegrounded() {
        lockJob?.cancel()
        lockJob = null
        externalAccessUntilMs = 0L
    }

    fun setLockTimeout(timeoutMs: Long) {
        require(timeoutMs in setOf(0L, 30_000L, 60_000L, 300_000L, 900_000L))
        userPrefs.edit().putLong("lock_timeout_ms", timeoutMs).apply()
        _state.value = _state.value.copy(lockTimeoutMs = timeoutMs)
    }

    fun setLockOnBackground(enabled: Boolean) {
        userPrefs.edit().putBoolean("lock_on_background", enabled).apply()
        _state.value = _state.value.copy(lockOnBackground = enabled)
    }

    fun setLockOnScreenOff(enabled: Boolean) {
        userPrefs.edit().putBoolean("lock_on_screen_off", enabled).apply()
        _state.value = _state.value.copy(lockOnScreenOff = enabled)
    }

    fun onScreenOff() {
        if (_state.value.lockOnScreenOff) lock()
    }

    fun prepareBiometricEnrollment(): Cipher? = runCatching { vault.prepareBiometricEnrollment() }
        .onFailure { reportError(it.message ?: "Biometric setup failed") }
        .getOrNull()

    fun completeBiometricEnrollment(authenticatedCipher: Cipher) {
        val key = master ?: return reportError("Unlock the vault before enabling biometrics")
        runCatching { vault.completeBiometricEnrollment(key, authenticatedCipher) }
            .onSuccess { _state.value = _state.value.copy(biometricEnabled = true, error = null) }
            .onFailure { reportError("Biometric setup failed (${it.javaClass.simpleName}${it.message?.let { message -> ": $message" }.orEmpty()})") }
    }

    fun prepareBiometricUnlock(): Cipher? = runCatching { vault.prepareBiometricUnlock() }
        .onFailure { reportError("Biometric key is unavailable. Unlock with your PIN and set it up again.") }
        .getOrNull()

    fun completeBiometricUnlock(authenticatedCipher: Cipher) {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.completeBiometricUnlock(authenticatedCipher) }
                .onSuccess { key ->
                    attemptPrefs.edit().clear().apply()
                    master = key
                    SecureAccessSession.install(key)
                    publishUnlocked(key)
                    importPending()
                }
                .onFailure { _state.value = _state.value.copy(busy = false, error = "Biometric unlock failed. Use your PIN or passphrase.") }
        }
    }

    fun disableBiometric() {
        runCatching { vault.disableBiometric() }
        val policy = if (_state.value.unlockPolicy == SecureUnlockPolicy.BIOMETRIC_ONLY) {
            SecureUnlockPolicy.PIN_ONLY
        } else _state.value.unlockPolicy
        userPrefs.edit().putString("unlock_policy", policy.name).apply()
        _state.value = _state.value.copy(biometricEnabled = false, unlockPolicy = policy)
    }

    fun setUnlockPolicy(policy: SecureUnlockPolicy) {
        if (policy == SecureUnlockPolicy.BIOMETRIC_ONLY && !_state.value.biometricEnabled) {
            return reportError("Enable biometric unlock before choosing biometric-only")
        }
        userPrefs.edit().putString("unlock_policy", policy.name).apply()
        _state.value = _state.value.copy(unlockPolicy = policy, error = null)
    }

    fun changeSecret(current: String, replacement: String, confirmation: String) {
        if (replacement.length != 4 || replacement.any { !it.isDigit() }) return reportError("Use exactly four digits")
        if (replacement != confirmation) return reportError("New PIN confirmation does not match")
        val active = master ?: return reportError("Vault is locked")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, progress = "Updating credentials")
            runCatching {
                val verified = vault.unlock(current.toCharArray())
                try {
                    if (!MessageDigest.isEqual(verified, active)) throw SecurityException("Current PIN or passphrase is incorrect")
                    vault.changeSecret(active, replacement.toCharArray())
                } finally {
                    verified.fill(0)
                }
            }.onSuccess {
                attemptPrefs.edit().clear().apply()
                _state.value = _state.value.copy(busy = false, progress = null, error = null)
            }.onFailure {
                _state.value = _state.value.copy(busy = false, progress = null, error = it.message ?: "Credential change failed")
            }
        }
    }

    fun reportError(message: String) {
        _state.value = _state.value.copy(error = message)
    }

    fun import(uris: List<Uri>) {
        queueImport(uris)
    }

    /** Verifies Secure Gallery duplicates from authenticated plaintext bytes. Size/dimensions are
     * only a cheap candidate filter and can never make an item eligible for deletion. */
    suspend fun exactDuplicateItems(items: List<SecureItem>): List<SecureItem> = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext emptyList()
        items.asSequence()
            .filter { it.sizeBytes > 0L }
            .groupBy(SecureItem::sizeBytes)
            .values.asSequence()
            .filter { it.size > 1 }
            .flatMap { candidates ->
                candidates.asSequence().mapNotNull { item ->
                    val digest = runCatching {
                        MessageDigest.getInstance("SHA-256").also { hash ->
                            vault.streamPlain(key, item) { bytes, count -> hash.update(bytes, 0, count) }
                        }.digest().joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
                    }.getOrNull() ?: return@mapNotNull null
                    digest to item
                }
            }
            .groupBy(keySelector = { it.first }, valueTransform = { it.second })
            .values.asSequence()
            .filter { it.size > 1 }
            .flatten()
            .sortedByDescending(SecureItem::dateTakenMs)
            .toList()
    }

    /** Conservative Secure Gallery review collections. Thumbnails are decrypted only in memory;
     * findings are suggestions and never trigger automatic deletion. */
    internal suspend fun qualityReviewItems(items: List<SecureItem>): Map<QualityReviewReason, List<SecureItem>> = withContext(Dispatchers.IO) {
        val findings = LinkedHashMap<QualityReviewReason, MutableList<SecureItem>>()
        QualityReviewReason.entries.filterNot { it == QualityReviewReason.BURST }.forEach { findings[it] = ArrayList() }
        items.forEach { item ->
            coroutineContext.ensureActive()
            var sharpness: Double? = null
            var meanLuma: Double? = null
            if (!item.isVideo) {
                val thumbnail = previewImageThumbnail(item)
                if (thumbnail != null) try {
                    val metrics = imageQualityMetrics(thumbnail)
                    sharpness = metrics.first
                    meanLuma = metrics.second
                } finally {
                    thumbnail.recycle()
                }
            }
            qualityReviewReasons(
                width = item.width,
                height = item.height,
                sizeBytes = item.sizeBytes,
                kind = if (item.isVideo) MediaKind.VIDEO else MediaKind.IMAGE,
                sharpness = sharpness,
                meanLuma = meanLuma,
            ).forEach { reason -> findings.getOrPut(reason, ::ArrayList).add(item) }
        }
        findings.mapValues { (_, values) -> values.sortedByDescending(SecureItem::dateTakenMs) }
    }

    private fun importPending() {
        val key = master ?: return
        if (pendingUris.isEmpty() || _state.value.busy) return
        val imports = ArrayList(pendingUris).also { pendingUris.clear() }
        val move = pendingImportMove.also { pendingImportMove = false }
        val album = pendingImportAlbum.also { pendingImportAlbum = null }
        runCatching {
            val session = vault.createTransferSession(key)
            MediaTransferCoordinator.enqueueSecureImport(getApplication(), imports, album, move, session, _state.value.defaultStoragePolicy)
        }.onSuccess { workId ->
            _state.value = _state.value.copy(
                busy = false,
                progress = null,
                pendingImports = 0,
                lastImportSucceeded = false,
                notice = "${if (move) "Move" else "Copy"} continues in the background",
                error = null,
            )
            watchTransfer(workId, "Secure import")
        }.onFailure {
            _state.value = _state.value.copy(busy = false, progress = null, pendingImports = 0, error = it.message ?: "Could not start secure transfer")
        }
    }

    fun consumeImportResult() {
        _state.value = _state.value.copy(lastImportSucceeded = false)
    }

    fun delete(item: SecureItem) {
        val key = master ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.moveToTrash(key, item) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "Delete failed") }
        }
    }

    fun delete(items: List<SecureItem>) {
        val key = master ?: return
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, progress = "Moving to recycle bin")
            runCatching { items.forEach { vault.moveToTrash(key, it) } }
                .onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, progress = null, error = it.message ?: "Delete failed") }
        }
    }

    fun setFavourite(item: SecureItem, favourite: Boolean) {
        val key = master ?: return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vault.setFavourite(key, item, favourite) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { reportError(it.message ?: "Could not update favourite") }
        }
    }

    fun setFavourite(items: List<SecureItem>, favourite: Boolean) {
        val key = master ?: return reportError("Vault is locked")
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vault.setFavourite(key, items, favourite) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { reportError(it.message ?: "Could not update favourites") }
        }
    }

    fun updateMetadata(
        items: List<SecureItem>,
        tag: String? = null,
        dateTakenMs: Long? = null,
        latitude: Double? = null,
        longitude: Double? = null,
        replaceLocation: Boolean = false,
        rating: Int? = null,
    ) {
        val key = master ?: return reportError("Vault is locked")
        if (items.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vault.updateMetadata(key, items, tag, dateTakenMs, latitude, longitude, replaceLocation, rating) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { reportError(it.message ?: "Could not update secure metadata") }
        }
    }

    fun export(items: List<SecureItem>, move: Boolean, targetAlbum: String? = null) {
        val key = master ?: return reportError("Vault is locked")
        if (items.isEmpty()) return
        runCatching {
            val session = vault.createTransferSession(key)
            MediaTransferCoordinator.enqueueSecureExport(getApplication(), items.map { it.id }, targetAlbum, move, session)
        }.onSuccess { workId ->
            _state.value = _state.value.copy(notice = "${if (move) "Move" else "Copy"} continues in the background", error = null)
            watchTransfer(workId, "Gallery transfer")
        }.onFailure { reportError(it.message ?: "Could not start Gallery transfer") }
    }

    fun consumeNotice() {
        _state.value = _state.value.copy(notice = null)
    }

    fun scanIntegrity() {
        val key = master ?: return reportError("Vault is locked")
        if (_state.value.integrityScanning) return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(integrityScanning = true, progress = "Checking secure media", error = null)
            runCatching {
                vault.scanIntegrity(key) { completed, total ->
                    _state.value = _state.value.copy(progress = "Checking secure media • $completed of $total")
                }
            }.onSuccess { report ->
                _state.value = _state.value.copy(
                    integrityScanning = false,
                    progress = null,
                    integrityReport = report,
                    notice = if (report.healthy) "Secure Gallery integrity check passed" else "Integrity check found ${report.issues.size + report.orphanFiles.size} item${if (report.issues.size + report.orphanFiles.size == 1) "" else "s"} to review",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(integrityScanning = false, progress = null, error = error.message ?: "Integrity check failed")
            }
        }
    }

    fun repairVaultSafely() {
        val key = master ?: return reportError("Vault is locked")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, progress = "Repairing recoverable metadata", error = null)
            runCatching { vault.repairSafe(key) }
                .onSuccess { removed ->
                    publishUnlocked(key)
                    _state.value = _state.value.copy(notice = "Repair completed; removed $removed abandoned partial file${if (removed == 1) "" else "s"}. Orphans were preserved.")
                    scanIntegrity()
                }
                .onFailure { error -> _state.value = _state.value.copy(busy = false, progress = null, error = error.message ?: "Repair failed") }
        }
    }

    fun dismissIntegrityReport() {
        _state.value = _state.value.copy(integrityReport = null)
    }

    fun exportRecoveryKey(destination: Uri, recoveryPassphrase: String) {
        val key = master ?: return reportError("Vault is locked")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, progress = "Protecting recovery key", error = null)
            runCatching {
                val bytes = vault.exportRecoveryKey(key, recoveryPassphrase.toCharArray())
                try {
                    getApplication<Application>().contentResolver.openOutputStream(destination, "w")?.use { output ->
                        output.write(bytes)
                        output.flush()
                    } ?: error("Android could not open the selected destination")
                } finally {
                    bytes.fill(0)
                }
            }.onSuccess {
                _state.value = _state.value.copy(busy = false, progress = null, notice = "Encrypted recovery key exported")
            }.onFailure { error ->
                _state.value = _state.value.copy(busy = false, progress = null, error = error.message ?: "Recovery key export failed")
            }
        }
    }

    fun exportEncryptedBackup(destination: Uri, backupPassphrase: String) {
        val key = master ?: return reportError("Vault is locked")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, operationProgress = 0, progress = "Preparing encrypted backup", error = null)
            runCatching {
                getApplication<Application>().contentResolver.openOutputStream(destination, "w")?.use { output ->
                    vault.exportEncryptedBackup(key, backupPassphrase.toCharArray(), output) { completed, total ->
                        val percent = if (total <= 0L) 100 else ((completed * 100L) / total).toInt().coerceIn(0, 100)
                        _state.value = _state.value.copy(operationProgress = percent, progress = "Backing up Secure Gallery • $percent%")
                    }
                } ?: error("Android could not open the selected destination")
            }.onSuccess { result ->
                _state.value = _state.value.copy(
                    busy = false,
                    operationProgress = null,
                    progress = null,
                    notice = "Encrypted backup saved • ${result.itemCount} item${if (result.itemCount == 1) "" else "s"}",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(busy = false, operationProgress = null, progress = null, error = error.message ?: "Encrypted backup failed")
            }
        }
    }

    fun restoreEncryptedBackup(source: Uri, backupPassphrase: String) {
        val key = master ?: return reportError("Vault is locked")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, operationProgress = 0, progress = "Opening encrypted backup", error = null)
            runCatching {
                getApplication<Application>().contentResolver.openInputStream(source)?.use { input ->
                    vault.restoreEncryptedBackup(key, backupPassphrase.toCharArray(), input) { completed, total ->
                        val percent = if (total <= 0L) 100 else ((completed * 100L) / total).toInt().coerceIn(0, 100)
                        _state.value = _state.value.copy(operationProgress = percent, progress = "Restoring Secure Gallery • $percent%")
                    }
                } ?: error("Android could not open the selected backup")
            }.onSuccess { result ->
                publishUnlocked(key)
                _state.value = _state.value.copy(
                    operationProgress = null,
                    notice = "Restore completed • ${result.itemCount} item${if (result.itemCount == 1) "" else "s"} added",
                )
            }.onFailure { error ->
                _state.value = _state.value.copy(busy = false, operationProgress = null, progress = null, error = error.message ?: "Encrypted backup restore failed")
            }
        }
    }

    fun saveEditedImage(source: SecureItem, bitmap: Bitmap, replaceOriginal: Boolean = false, exportOptions: PhotoExportOptions? = null) {
        val key = master ?: return reportError("Vault is locked")
        val safeReplace = replaceOriginal && canSafelyOverwritePhoto(source.mimeType)
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, progress = "Saving encrypted edit")
            runCatching {
                val output = ByteArrayOutputStream()
                val preservePng = exportOptions == null && source.mimeType.equals("image/png", true)
                val preserveWebp = exportOptions == null && Build.VERSION.SDK_INT >= 30 && source.mimeType.equals("image/webp", true)
                val requestedFormat = exportOptions?.format
                val format = when (requestedFormat) {
                    PhotoOutputFormat.PNG -> Bitmap.CompressFormat.PNG
                    PhotoOutputFormat.WEBP -> if (Build.VERSION.SDK_INT >= 30) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
                    PhotoOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
                    PhotoOutputFormat.HEIC, PhotoOutputFormat.AVIF -> null
                    null -> if (preservePng) Bitmap.CompressFormat.PNG else if (preserveWebp) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.JPEG
                }
                val mimeType = requestedFormat?.mimeType ?: if (preservePng) "image/png" else if (preserveWebp) "image/webp" else "image/jpeg"
                val extension = requestedFormat?.extension ?: if (preservePng) "png" else if (preserveWebp) "webp" else "jpg"
                val quality = exportOptions?.quality?.coerceIn(1, 100) ?: if (format == Bitmap.CompressFormat.JPEG) 95 else 100
                val temporary = if (requestedFormat == PhotoOutputFormat.HEIC || requestedFormat == PhotoOutputFormat.AVIF) {
                    ModernImageEncoder.encodeToPrivateTemporaryFile(
                        getApplication<Application>().cacheDir,
                        bitmap,
                        requestedFormat,
                        quality,
                    )
                } else null
                if (temporary == null) {
                    check(bitmap.compress(checkNotNull(format), quality, output)) { "Could not encode edited image" }
                }
                val encoded = temporary?.readBytes() ?: output.toByteArray()
                val preserved = if (mimeType == "image/jpeg" && exportOptions?.stripMetadata != true) {
                    runCatching { vault.preservableJpegMetadata(key, source) }.getOrDefault(emptyList())
                } else emptyList()
                val bytes = if (preserved.isEmpty()) encoded else mergeJpegMetadata(encoded, preserved).also {
                    if (it !== encoded) encoded.fill(0)
                }
                try {
                    vault.importEditedImage(key, source, bytes, bitmap.width, bitmap.height, mimeType, extension, retainSourceName = safeReplace)
                    if (safeReplace) vault.delete(key, source)
                } finally {
                    bytes.fill(0)
                    temporary?.let(ModernImageEncoder::securelyDelete)
                    bitmap.recycle()
                }
            }.onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, progress = null, error = it.message ?: "Could not save edited image") }
        }
    }

    fun saveEditedVideo(source: SecureItem, editedFile: File, width: Int, height: Int, durationMs: Long, replaceOriginal: Boolean = false) {
        val key = master ?: run {
            editedFile.delete()
            return reportError("Vault is locked")
        }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, progress = "Encrypting edited video")
            runCatching {
                val base = source.name.substringBeforeLast('.', source.name)
                vault.importGeneratedFile(
                    key,
                    editedFile,
                    if (replaceOriginal) source.name else "${base}_edited_${System.currentTimeMillis()}.mp4",
                    "video/mp4",
                    source.albumName,
                    width,
                    height,
                    durationMs,
                    source.storagePolicy,
                    source.dateTakenMs,
                    source.tag,
                    source.latitude,
                    source.longitude,
                )
                if (replaceOriginal) vault.delete(key, source)
            }.onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, progress = null, error = it.message ?: "Could not save edited video") }
            editedFile.delete()
        }
    }

    fun placeInAlbum(items: List<SecureItem>, albumName: String, copy: Boolean) {
        val key = master ?: return reportError("Vault is locked")
        if (items.isEmpty() || albumName.isBlank()) return
        runCatching {
            val session = vault.createTransferSession(key)
            MediaTransferCoordinator.enqueueSecureAlbum(getApplication(), items.map { it.id }, albumName, copy, session)
        }.onSuccess { workId ->
            _state.value = _state.value.copy(notice = "${if (copy) "Copy" else "Move"} continues in the background", error = null)
            watchTransfer(workId, "Secure album transfer")
        }.onFailure { reportError(it.message ?: "Could not start secure album transfer") }
    }

    private fun watchTransfer(workId: java.util.UUID, label: String) {
        viewModelScope.launch {
            val workManager = WorkManager.getInstance(getApplication<Application>())
            var info: WorkInfo
            do {
                info = withContext(Dispatchers.IO) { requireNotNull(workManager.getWorkInfoById(workId).get()) }
                if (!info.state.isFinished) delay(500)
            } while (!info.state.isFinished)
            val activeKey = master
            if (activeKey != null && _state.value.unlocked) withContext(Dispatchers.IO) { publishUnlocked(activeKey) }
            val message = info.outputData.getString(MediaTransferWorker.KEY_MESSAGE)
            _state.value = if (info.state == WorkInfo.State.SUCCEEDED) {
                _state.value.copy(notice = message ?: "$label complete", error = null, lastImportSucceeded = label == "Secure import")
            } else {
                _state.value.copy(error = message ?: "$label did not finish", lastImportSucceeded = false)
            }
        }
    }

    internal fun create(items: List<SecureItem>, type: CreationType, options: CreativeOptions = CreativeOptions()) {
        val key = master ?: return reportError("Vault is locked")
        val sources = items.distinctBy { it.id }.take(if (type == CreationType.COLLAGE) 9 else 16)
        val sourceItem = sources.firstOrNull() ?: return reportError("Select at least one picture or video")
        createJob?.cancel()
        createJob = viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(
                busy = true,
                error = null,
                progress = "Creating secure ${type.title}",
                operationProgress = 0,
                operationCancellable = true,
            )
            val bitmaps = ArrayList<Bitmap>()
            val temporarySources = ArrayList<File>()
            val temp = File(getApplication<Application>().cacheDir, "secure-create-${System.nanoTime()}.${if (type == CreationType.MOVIE) "mp4" else if (type == CreationType.GIF) "gif" else "jpg"}")
            runCatching {
                val now = System.currentTimeMillis()
                val album = sourceItem.albumName
                if (type == CreationType.MOVIE) {
                    val movieSources = sources.mapNotNull { item ->
                        val sourceFile = if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) vault.storageFile(item)
                        else vault.createShareFile(key, item).also(temporarySources::add)
                        if (item.isVideo) {
                            CreativeMovieSource(videoUri = Uri.fromFile(sourceFile), durationMs = item.durationMs)
                        } else {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
                            var sample = 1
                            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > options.movie.height.coerceAtLeast(720) * 2) sample *= 2
                            BitmapFactory.decodeFile(sourceFile.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })
                                ?.also(bitmaps::add)?.let { CreativeMovieSource(still = it) }
                        }
                    }
                    require(movieSources.isNotEmpty()) { "Selected media could not be decoded" }
                    val height = options.movie.height.coerceIn(360, 1080)
                    val width = ((height.toFloat() * options.movie.ratio.widthScale / options.movie.ratio.heightScale).toInt() / 2 * 2).coerceAtLeast(2)
                    val duration = if (options.movie.backgroundAudioUri == null) {
                        RandomAccessFile(temp, "rw").use {
                            encodeMixedMovie(getApplication(), it.fd, movieSources, width, height, options = options.movie) { completed, total ->
                                _state.value = _state.value.copy(operationProgress = (completed * 95 / total.coerceAtLeast(1)).coerceIn(0, 95))
                            }
                        }
                    } else {
                        val silent = File(getApplication<Application>().cacheDir, "secure-movie-video-${System.nanoTime()}.mp4")
                        try {
                            val measured = RandomAccessFile(silent, "rw").use {
                                encodeMixedMovie(getApplication(), it.fd, movieSources, width, height, options = options.movie) { completed, total ->
                                    _state.value = _state.value.copy(operationProgress = (completed * 85 / total.coerceAtLeast(1)).coerceIn(0, 85))
                                }
                            }
                            _state.value = _state.value.copy(progress = "Adding music", operationProgress = 90)
                            RandomAccessFile(temp, "rw").use {
                                muxMovieWithBackgroundAudio(
                                    getApplication(),
                                    silent,
                                    requireNotNull(options.movie.backgroundAudioUri),
                                    it.fd,
                                    measured,
                                )
                            }
                            measured
                        } finally { silent.delete() }
                    }
                    vault.importGeneratedFile(key, temp, "Movie_$now.mp4", "video/mp4", album, width, height, duration, sourceItem.storagePolicy)
                } else {
                    sources.forEachIndexed { sourceIndex, item ->
                        val sourceFile = if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) vault.storageFile(item)
                        else vault.createShareFile(key, item).also(temporarySources::add)
                        if (type == CreationType.GIF && item.isVideo) {
                            bitmaps += decodeVideoGifFrames(getApplication(), Uri.fromFile(sourceFile), (16 / sources.size.coerceAtLeast(1)).coerceIn(4, 8))
                        } else if (item.isVideo) {
                            decodeVideoPoster(getApplication(), Uri.fromFile(sourceFile), 1600)?.let(bitmaps::add)
                        } else {
                            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(sourceFile.absolutePath, bounds)
                            var sample = 1
                            while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 1600) sample *= 2
                            BitmapFactory.decodeFile(sourceFile.absolutePath, BitmapFactory.Options().apply { inSampleSize = sample })?.let(bitmaps::add)
                        }
                        _state.value = _state.value.copy(operationProgress = ((sourceIndex + 1) * 70 / sources.size.coerceAtLeast(1)).coerceIn(0, 70))
                    }
                    if (type == CreationType.GIF && bitmaps.size > 16) {
                        bitmaps.drop(16).forEach(Bitmap::recycle)
                        while (bitmaps.size > 16) bitmaps.removeAt(bitmaps.lastIndex)
                    }
                    require(bitmaps.isNotEmpty()) { "Selected media could not be decoded" }
                    when (type) {
                        CreationType.COLLAGE -> {
                        val collage = renderCollageBitmap(bitmaps, options.collage)
                        try { FileOutputStream(temp).use { check(collage.compress(Bitmap.CompressFormat.JPEG, 95, it)) } }
                        finally { collage.recycle() }
                        vault.importGeneratedFile(key, temp, "Collage_$now.jpg", "image/jpeg", album, options.collage.ratio.width, options.collage.ratio.height, storagePolicy = sourceItem.storagePolicy)
                        }
                        CreationType.GIF -> {
                        val size = options.gifSize.coerceIn(360, 720)
                        BufferedOutputStream(FileOutputStream(temp)).use { writeAnimatedGif(it, bitmaps, size, options.gifDelayMs) }
                        vault.importGeneratedFile(key, temp, "Animation_$now.gif", "image/gif", album, size, size, storagePolicy = sourceItem.storagePolicy)
                        }
                        CreationType.MOVIE -> error("Movie handled by mixed-media path")
                    }
                }
            }.onSuccess {
                _state.value = _state.value.copy(operationProgress = 100)
                publishUnlocked(key)
            }.onFailure {
                if (it is kotlinx.coroutines.CancellationException) {
                    _state.value = _state.value.copy(
                        busy = false,
                        progress = null,
                        operationProgress = null,
                        operationCancellable = false,
                        notice = "Creation cancelled",
                    )
                } else {
                    _state.value = _state.value.copy(
                        busy = false,
                        progress = null,
                        operationProgress = null,
                        operationCancellable = false,
                        error = it.message ?: "Could not create secure media",
                    )
                }
            }
            bitmaps.forEach { it.recycle() }
            temporarySources.forEach(File::delete)
            temp.delete()
            createJob = null
        }
    }

    fun cancelCreate() {
        createJob?.cancel()
    }

    fun restore(item: SecureItem) {
        val key = master ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.restore(key, item) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "Restore failed") }
        }
    }

    fun deletePermanently(item: SecureItem) {
        val key = master ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.delete(key, item) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "Permanent delete failed") }
        }
    }

    fun emptyTrash() {
        val key = master ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, progress = "Emptying recycle bin")
            runCatching { vault.emptyTrash(key) }
                .onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, progress = null, error = it.message ?: "Could not empty recycle bin") }
        }
    }

    suspend fun previewBytes(item: SecureItem): ByteArray? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.decryptBytes(key, item) }.getOrNull()
    }

    suspend fun previewImageThumbnail(item: SecureItem): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(thumbnailCache) { thumbnailCache.get(item.id)?.let { return@withContext it } }
        thumbnailGenerationSlots.withPermit {
            synchronized(thumbnailCache) { thumbnailCache.get(item.id)?.let { return@withPermit it } }
            val key = master ?: return@withPermit null
            runCatching { vault.loadOrCreateThumbnail(key, item) }.getOrNull()
                ?.also { synchronized(thumbnailCache) { thumbnailCache.put(item.id, it) } }
        }
    }

    /** On-demand visual retrieval without exporting or materializing secure plaintext files. */
    suspend fun visuallySimilarItems(source: SecureItem, candidates: List<SecureItem>, limit: Int = 80): List<SecureItem> =
        withContext(Dispatchers.IO) {
            require(!source.isVideo) { "Visual similarity currently supports photos" }
            val sourceBitmap = previewImageThumbnail(source) ?: return@withContext emptyList()
            val query = try { visualDescriptor(sourceBitmap) } finally { sourceBitmap.recycle() }
            val index = InMemoryVectorIndex()
            val byId = HashMap<String, SecureItem>()
            candidates.asSequence().filter { !it.isVideo && it.id != source.id }.forEach { item ->
                kotlinx.coroutines.currentCoroutineContext().ensureActive()
                previewImageThumbnail(item)?.let { bitmap ->
                    try {
                        index.upsert(item.id, visualDescriptor(bitmap))
                        byId[item.id] = item
                    } finally { bitmap.recycle() }
                }
            }
            index.nearest(query, limit.coerceIn(1, 200))
                .filter { it.score >= .72f }
                .mapNotNull { byId[it.id] }
        }

    suspend fun previewVideoThumbnail(item: SecureItem): Bitmap? = withContext(Dispatchers.IO) {
        synchronized(thumbnailCache) { thumbnailCache.get(item.id)?.let { return@withContext it } }
        thumbnailGenerationSlots.withPermit {
            synchronized(thumbnailCache) { thumbnailCache.get(item.id)?.let { return@withPermit it } }
            val key = master ?: return@withPermit null
            runCatching { vault.loadOrCreateThumbnail(key, item) }.getOrNull()
                ?.also { synchronized(thumbnailCache) { thumbnailCache.put(item.id, it) } }
        }
    }

    suspend fun previewVideoContactSheet(item: SecureItem): Bitmap? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.loadOrCreateVideoContactSheet(key, item) }.getOrNull()
    }

    fun captureVideoFrame(item: SecureItem, positionMs: Long) {
        val key = master ?: return reportError("Vault is locked")
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null, progress = "Capturing video frame")
            val image = File(getApplication<Application>().cacheDir, "secure-frame-${System.nanoTime()}.jpg")
            runCatching {
                val source = if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) vault.storageFile(item)
                else vault.createShareFile(key, item)
                val retriever = MediaMetadataRetriever()
                val bitmap = try {
                    retriever.setDataSource(source.absolutePath)
                    retriever.getFrameAtTime(positionMs.coerceAtLeast(0L) * 1_000L, MediaMetadataRetriever.OPTION_CLOSEST)
                        ?: error("This video frame could not be decoded")
                } finally {
                    retriever.release()
                    if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) source.delete()
                }
                try { FileOutputStream(image).use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 96, it)) } }
                finally { bitmap.recycle() }
                val frameTakenMs = item.dateTakenMs.takeIf { it > 0L }?.plus(positionMs.coerceAtLeast(0L))
                    ?: System.currentTimeMillis()
                androidx.exifinterface.media.ExifInterface(image.absolutePath).apply {
                    val timestamp = java.text.SimpleDateFormat("yyyy:MM:dd HH:mm:ss", java.util.Locale.US)
                        .format(java.util.Date(frameTakenMs))
                    setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME, timestamp)
                    setAttribute(androidx.exifinterface.media.ExifInterface.TAG_DATETIME_ORIGINAL, timestamp)
                    setAttribute(
                        androidx.exifinterface.media.ExifInterface.TAG_USER_COMMENT,
                        "Captured from ${item.name} at ${positionMs.coerceAtLeast(0L)} ms; secure source ${item.id}",
                    )
                    setAttribute(androidx.exifinterface.media.ExifInterface.TAG_IMAGE_DESCRIPTION, "Video snapshot from ${item.name}")
                    saveAttributes()
                }
                val stamp = GalleryLogic.durationLabel(positionMs).replace(':', '-')
                vault.importGeneratedFile(
                    key,
                    image,
                    "${item.name.substringBeforeLast('.')}_$stamp.jpg",
                    "image/jpeg",
                    item.albumName,
                    item.width,
                    item.height,
                    dateTakenMs = frameTakenMs,
                    storagePolicy = item.storagePolicy,
                )
            }.onSuccess { publishUnlocked(key) }
                .onFailure { _state.value = _state.value.copy(busy = false, progress = null, error = it.message ?: "Could not capture frame") }
            image.delete()
        }
    }

    fun dataSource(item: SecureItem): DataSource.Factory? = master?.let { vault.dataSourceFactory(it, item) }

    suspend fun shareFile(item: SecureItem): File? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.createShareFile(key, item) }.getOrNull()
    }

    suspend fun shareFiles(items: List<SecureItem>): List<Pair<SecureItem, File>> = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext emptyList()
        items.mapNotNull { item -> runCatching { item to vault.createShareFile(key, item) }.getOrNull() }
    }

    suspend fun playbackFile(item: SecureItem): File? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching {
            if (item.storagePolicy == SecureStoragePolicy.LOCKED_ONLY) vault.storageFile(item)
            else vault.createShareFile(key, item)
        }.getOrNull()
    }

    fun releasePlaybackFile(item: SecureItem, file: File?) {
        if (item.storagePolicy == SecureStoragePolicy.ENCRYPTED) file?.delete()
    }

    suspend fun previewImage(item: SecureItem, maximumDimension: Int = 3072): Bitmap? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.decodeImagePreview(key, item, maximumDimension) }.getOrNull()
    }

    fun clearTemporaryFiles() {
        vault.cleanupShareCache()
    }

    fun setDefaultStoragePolicy(policy: SecureStoragePolicy) {
        userPrefs.edit().putString("default_storage_policy", policy.name).apply()
        _state.value = _state.value.copy(defaultStoragePolicy = policy)
    }

    fun convertStoragePolicy(items: List<SecureItem>, policy: SecureStoragePolicy) {
        val key = master ?: return reportError("Vault is locked")
        val targets = items.filter { it.storagePolicy != policy }
        if (targets.isEmpty()) return
        runCatching {
            val session = vault.createTransferSession(key)
            MediaTransferCoordinator.enqueueSecureStorageConversion(getApplication(), targets.map { it.id }, policy, session)
        }.onSuccess { workId ->
            _state.value = _state.value.copy(notice = "Storage conversion continues in the background", error = null)
            watchTransfer(workId, "Storage conversion")
        }.onFailure { reportError(it.message ?: "Storage conversion could not start") }
    }

    fun reset() {
        val key = master ?: return
        vault.reset(key)
        master = null
        pendingUris.clear()
        _state.value = SecureGalleryState(configured = false)
    }

    private fun publishUnlocked(key: ByteArray) {
        val all = vault.list(key, includeTrashed = true)
        val visible = all.filterNot { it.isTrashed }
        _state.value = _state.value.copy(
            configured = true,
            unlocked = true,
            busy = false,
            progress = null,
            operationProgress = null,
            operationCancellable = false,
            items = visible,
            trashItems = all.filter { it.isTrashed },
            encryptedCount = visible.count { it.storagePolicy == SecureStoragePolicy.ENCRYPTED },
            lockedOnlyCount = visible.count { it.storagePolicy == SecureStoragePolicy.LOCKED_ONLY },
            biometricEnabled = vault.isBiometricEnabled(),
            error = null,
        )
        if (!legacyMigrationStarted && visible.any { it.storagePolicy == SecureStoragePolicy.ENCRYPTED }) {
            legacyMigrationStarted = true
            viewModelScope.launch(Dispatchers.IO) {
                runCatching {
                    vault.migrateLegacyEncryption(key) { completed, total ->
                        _state.value = _state.value.copy(progress = if (total > 0) "Upgrading secure encryption $completed of $total" else null)
                    }
                }.onSuccess { migrated ->
                    _state.value = _state.value.copy(
                        progress = null,
                        notice = if (migrated > 0) "$migrated secure item${if (migrated == 1) "" else "s"} upgraded to Tink Streaming AEAD" else _state.value.notice,
                    )
                }.onFailure { error ->
                    _state.value = _state.value.copy(progress = null, notice = "Secure encryption upgrade paused: ${error.message ?: "unknown error"}")
                }
            }
        }
    }

    override fun onCleared() {
        lock()
        super.onCleared()
    }
}
