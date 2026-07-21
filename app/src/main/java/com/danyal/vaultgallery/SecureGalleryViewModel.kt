package com.danyal.vaultgallery

import android.app.Application
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.DataSource
import com.danyal.vaultgallery.security.SecureItem
import com.danyal.vaultgallery.security.SecureVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

data class SecureGalleryState(
    val configured: Boolean = false,
    val unlocked: Boolean = false,
    val busy: Boolean = false,
    val progress: String? = null,
    val items: List<SecureItem> = emptyList(),
    val trashItems: List<SecureItem> = emptyList(),
    val biometricEnabled: Boolean = false,
    val lockTimeoutMs: Long = 0L,
    val pendingImports: Int = 0,
    val lastImportSucceeded: Boolean = false,
    val error: String? = null,
)

class SecureGalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = SecureVault(application)
    private val attemptPrefs = application.getSharedPreferences("secure-auth-attempts", 0)
    private val userPrefs = application.getSharedPreferences("secure-user-settings", 0)
    private var master: ByteArray? = null
    private var lockJob: Job? = null
    private var pendingUris = ArrayList<Uri>()
    private val _state = MutableStateFlow(
        SecureGalleryState(
            configured = vault.isConfigured(),
            biometricEnabled = vault.isBiometricEnabled(),
            lockTimeoutMs = userPrefs.getLong("lock_timeout_ms", 0L),
        ),
    )
    val state: StateFlow<SecureGalleryState> = _state.asStateFlow()

    fun queueImport(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingUris.addAll(uris.filterNot { it in pendingUris })
        _state.value = _state.value.copy(pendingImports = pendingUris.size, lastImportSucceeded = false)
        if (_state.value.unlocked) importPending()
    }

    fun setup(secret: String, confirmation: String) {
        if (secret != confirmation) {
            _state.value = _state.value.copy(error = "PIN or passphrase confirmation does not match")
            return
        }
        if (secret.length < 6) {
            _state.value = _state.value.copy(error = "Use at least six characters")
            return
        }
        _state.value = _state.value.copy(busy = true, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vault.setup(secret.toCharArray()) }
                .onSuccess { key ->
                    attemptPrefs.edit().clear().apply()
                    master = key
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
        master?.fill(0)
        master = null
        _state.value = _state.value.copy(unlocked = false, busy = false, progress = null, items = emptyList(), trashItems = emptyList(), error = null)
    }

    fun onBackgrounded() {
        lockJob?.cancel()
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

    fun onForegrounded() {
        lockJob?.cancel()
        lockJob = null
    }

    fun setLockTimeout(timeoutMs: Long) {
        require(timeoutMs in setOf(0L, 30_000L, 60_000L, 300_000L, 900_000L))
        userPrefs.edit().putLong("lock_timeout_ms", timeoutMs).apply()
        _state.value = _state.value.copy(lockTimeoutMs = timeoutMs)
    }

    fun prepareBiometricEnrollment(): Boolean = runCatching { vault.prepareBiometricEnrollment() }
        .onFailure { reportError(it.message ?: "Biometric setup failed") }
        .isSuccess

    fun completeBiometricEnrollment() {
        val key = master ?: return reportError("Unlock the vault before enabling biometrics")
        runCatching { vault.completeBiometricEnrollment(key) }
            .onSuccess { _state.value = _state.value.copy(biometricEnabled = true, error = null) }
            .onFailure { reportError("Biometric setup failed (${it.javaClass.simpleName}${it.message?.let { message -> ": $message" }.orEmpty()})") }
    }

    fun prepareBiometricUnlock(): Boolean = runCatching { vault.prepareBiometricUnlock() }
        .onFailure { reportError("Biometric key is unavailable. Unlock with your PIN and set it up again.") }
        .isSuccess

    fun completeBiometricUnlock() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.completeBiometricUnlock() }
                .onSuccess { key ->
                    attemptPrefs.edit().clear().apply()
                    master = key
                    publishUnlocked(key)
                    importPending()
                }
                .onFailure { _state.value = _state.value.copy(busy = false, error = "Biometric unlock failed. Use your PIN or passphrase.") }
        }
    }

    fun disableBiometric() {
        runCatching { vault.disableBiometric() }
        _state.value = _state.value.copy(biometricEnabled = false)
    }

    fun changeSecret(current: String, replacement: String, confirmation: String) {
        if (replacement.length < 6) return reportError("Use at least six characters")
        if (replacement != confirmation) return reportError("New PIN or passphrase confirmation does not match")
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

    private fun importPending() {
        val key = master ?: return
        if (pendingUris.isEmpty() || _state.value.busy) return
        val imports = ArrayList(pendingUris).also { pendingUris.clear() }
        _state.value = _state.value.copy(busy = true, progress = "Preparing secure import", pendingImports = imports.size, lastImportSucceeded = false, error = null)
        viewModelScope.launch(Dispatchers.IO) {
            val failures = ArrayList<String>()
            imports.forEachIndexed { index, uri ->
                _state.value = _state.value.copy(progress = "Encrypting ${index + 1} of ${imports.size}")
                runCatching { vault.importUri(key, getApplication<Application>().contentResolver, uri) }
                    .onFailure { failures += (it.message ?: "Import failed") }
            }
            val latest = runCatching { vault.list(key) }.getOrDefault(_state.value.items)
            val trash = runCatching { vault.list(key, includeTrashed = true).filter { it.isTrashed } }.getOrDefault(_state.value.trashItems)
            _state.value = _state.value.copy(
                busy = false,
                progress = null,
                pendingImports = 0,
                lastImportSucceeded = failures.isEmpty(),
                items = latest,
                trashItems = trash,
                error = failures.takeIf { it.isNotEmpty() }?.joinToString("; "),
            )
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

    suspend fun previewVideoThumbnail(item: SecureItem): Bitmap? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        val temporary = runCatching { vault.createShareFile(key, item) }.getOrNull() ?: return@withContext null
        try {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(temporary.absolutePath)
                retriever.getFrameAtTime(1_000_000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
            } finally {
                retriever.release()
            }
        } finally {
            temporary.delete()
        }
    }

    fun dataSource(item: SecureItem): DataSource.Factory? = master?.let { vault.dataSourceFactory(it, item) }

    suspend fun shareFile(item: SecureItem): File? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.createShareFile(key, item) }.getOrNull()
    }

    suspend fun playbackFile(item: SecureItem): File? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.createShareFile(key, item) }.getOrNull()
    }

    fun clearTemporaryFiles() {
        vault.cleanupShareCache()
    }

    fun reset() {
        val key = master ?: return
        vault.reset(key)
        master = null
        pendingUris.clear()
        _state.value = SecureGalleryState(configured = false)
    }

    private fun publishUnlocked(key: ByteArray) {
        _state.value = _state.value.copy(
            configured = true,
            unlocked = true,
            busy = false,
            progress = null,
            items = vault.list(key),
            trashItems = vault.list(key, includeTrashed = true).filter { it.isTrashed },
            biometricEnabled = vault.isBiometricEnabled(),
            error = null,
        )
    }

    override fun onCleared() {
        lock()
        super.onCleared()
    }
}
