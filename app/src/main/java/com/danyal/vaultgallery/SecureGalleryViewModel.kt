package com.danyal.vaultgallery

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.datasource.DataSource
import com.danyal.vaultgallery.security.SecureItem
import com.danyal.vaultgallery.security.SecureVault
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SecureGalleryState(
    val configured: Boolean = false,
    val unlocked: Boolean = false,
    val busy: Boolean = false,
    val progress: String? = null,
    val items: List<SecureItem> = emptyList(),
    val error: String? = null,
)

class SecureGalleryViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = SecureVault(application)
    private val attemptPrefs = application.getSharedPreferences("secure-auth-attempts", 0)
    private var master: ByteArray? = null
    private var pendingUris = ArrayList<Uri>()
    private val _state = MutableStateFlow(SecureGalleryState(configured = vault.isConfigured()))
    val state: StateFlow<SecureGalleryState> = _state.asStateFlow()

    fun queueImport(uris: List<Uri>) {
        if (uris.isEmpty()) return
        pendingUris.addAll(uris.filterNot { it in pendingUris })
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
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { vault.setup(secret.toCharArray()) }
                .onSuccess { key ->
                    attemptPrefs.edit().clear().apply()
                    master = key
                    _state.value = SecureGalleryState(configured = true, unlocked = true, items = vault.list(key))
                    importPending()
                }
                .onFailure { _state.value = _state.value.copy(error = it.message ?: "Secure Gallery setup failed") }
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
                    _state.value = _state.value.copy(unlocked = true, busy = false, items = vault.list(key), error = null)
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
        master?.fill(0)
        master = null
        _state.value = _state.value.copy(unlocked = false, busy = false, progress = null, items = emptyList(), error = null)
    }

    fun import(uris: List<Uri>) {
        queueImport(uris)
    }

    private fun importPending() {
        val key = master ?: return
        if (pendingUris.isEmpty() || _state.value.busy) return
        val imports = ArrayList(pendingUris).also { pendingUris.clear() }
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            val failures = ArrayList<String>()
            imports.forEachIndexed { index, uri ->
                _state.value = _state.value.copy(progress = "Encrypting ${index + 1} of ${imports.size}")
                runCatching { vault.importUri(key, getApplication<Application>().contentResolver, uri) }
                    .onFailure { failures += (it.message ?: "Import failed") }
            }
            val latest = runCatching { vault.list(key) }.getOrDefault(_state.value.items)
            _state.value = _state.value.copy(
                busy = false,
                progress = null,
                items = latest,
                error = failures.takeIf { it.isNotEmpty() }?.joinToString("; "),
            )
        }
    }

    fun delete(item: SecureItem) {
        val key = master ?: return
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = _state.value.copy(busy = true, error = null)
            runCatching { vault.delete(key, item) }
                .onSuccess { _state.value = _state.value.copy(busy = false, items = vault.list(key)) }
                .onFailure { _state.value = _state.value.copy(busy = false, error = it.message ?: "Delete failed") }
        }
    }

    suspend fun previewBytes(item: SecureItem): ByteArray? = withContext(Dispatchers.IO) {
        val key = master ?: return@withContext null
        runCatching { vault.decryptBytes(key, item) }.getOrNull()
    }

    fun dataSource(item: SecureItem): DataSource.Factory? = master?.let { vault.dataSourceFactory(it, item) }

    suspend fun shareFile(item: SecureItem): File? = withContext(Dispatchers.IO) {
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

    override fun onCleared() {
        lock()
        super.onCleared()
    }
}
