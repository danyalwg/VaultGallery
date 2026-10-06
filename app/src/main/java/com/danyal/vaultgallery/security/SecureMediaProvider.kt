package com.danyal.vaultgallery.security

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.ParcelFileDescriptor
import android.os.ProxyFileDescriptorCallback
import android.os.storage.StorageManager
import android.provider.OpenableColumns
import java.io.FileNotFoundException

/** Process-memory authorization. Locking the vault destroys this key immediately, so subsequent
 * provider reads fail even if another app still holds a granted content URI. */
object SecureAccessSession {
    private val monitor = Any()
    private var key: ByteArray? = null

    fun install(master: ByteArray) = synchronized(monitor) { key?.fill(0); key = master.copyOf() }
    fun clear() = synchronized(monitor) { key?.fill(0); key = null }
    fun copyKey(): ByteArray? = synchronized(monitor) { key?.copyOf() }
    fun isActive(): Boolean = synchronized(monitor) { key != null }
}

/** Kept under the original preference name so existing approvals survive upgrades. Approvals now
 * apply to any explicitly selected external app, not only video players. */
class ApprovedPlayerStore(context: Context) {
    private val prefs = context.getSharedPreferences("secure-approved-players", Context.MODE_PRIVATE)
    fun packages(): Set<String> = prefs.getStringSet("packages", emptySet()).orEmpty().toSet()
    fun setApproved(packageName: String, approved: Boolean) {
        val values = packages().toMutableSet().apply { if (approved) add(packageName) else remove(packageName) }
        prefs.edit().putStringSet("packages", values).apply()
    }
    fun isApproved(packageName: String) = packageName in packages()
    fun accessDurationMs(): Long = prefs.getLong("access_duration_ms", 15 * 60_000L).coerceIn(60_000L, 60 * 60_000L)
    fun setAccessDurationMs(value: Long) { prefs.edit().putLong("access_duration_ms", value.coerceIn(60_000L, 60 * 60_000L)).apply() }
}

/** Random-access, zero-plaintext-copy gateway used by the built-in player and explicitly approved
 * external players. Encrypted files are decrypted by authenticated chunks only as requested. */
class SecureMediaProvider : ContentProvider() {
    private lateinit var workerThread: HandlerThread
    private lateinit var workerHandler: Handler
    @Volatile private var vault: SecureVault? = null

    override fun onCreate(): Boolean {
        workerThread = HandlerThread("secure-media-provider").also { it.start() }
        workerHandler = Handler(workerThread.looper)
        // A ContentProvider is created before the first Activity. A revoked/missing SAF grant
        // must not crash the whole Gallery process; the Secure UI can then guide recovery.
        vault = runCatching { SecureVault(requireNotNull(context)) }.getOrNull()
        return true
    }

    override fun getType(uri: Uri): String? {
        val pair = itemFor(uri) ?: return null
        return pair.second.mimeType.also { pair.first.fill(0) }
    }

    override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? {
        val (key, item) = itemFor(uri) ?: return null
        val columns = projection?.toList().orEmpty().ifEmpty { listOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE) }
        return MatrixCursor(columns.toTypedArray(), 1).apply {
            addRow(columns.map { column -> when (column) { OpenableColumns.DISPLAY_NAME -> item.name; OpenableColumns.SIZE -> item.sizeBytes; else -> null } })
        }.also { key.fill(0) }
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Secure media is read-only")
        val caller = enforceCaller()
        val activeVault = vaultOrNull() ?: throw FileNotFoundException("Secure Gallery storage access must be restored")
        val (key, item) = itemFor(uri) ?: throw FileNotFoundException("Secure media is unavailable or the vault is locked")
        SecureAccessHistoryStore(requireNotNull(context)).record(caller, item.id, true)
        val storage = requireNotNull(context).getSystemService(StorageManager::class.java)
        return storage.openProxyFileDescriptor(
            ParcelFileDescriptor.MODE_READ_ONLY,
            object : ProxyFileDescriptorCallback() {
                override fun onGetSize(): Long = item.sizeBytes.coerceAtLeast(0L)
                override fun onRead(offset: Long, size: Int, data: ByteArray): Int {
                    if (!SecureAccessSession.isActive()) return -1
                    return activeVault.readAt(key, item, offset, size, data)
                }
                override fun onRelease() { key.fill(0) }
            },
            workerHandler,
        )
    }

    private fun itemFor(uri: Uri): Pair<ByteArray, SecureItem>? {
        val id = uri.pathSegments.takeIf { it.size == 2 && it[0] == "item" }?.get(1) ?: return null
        val key = SecureAccessSession.copyKey() ?: return null
        val activeVault = vaultOrNull()
        val item = runCatching { activeVault?.list(key, includeTrashed = true)?.firstOrNull { it.id == id && !it.isTrashed } }.getOrNull()
        if (item == null) key.fill(0)
        return item?.let { key to it }
    }

    private fun vaultOrNull(): SecureVault? {
        vault?.let { return it }
        val restored = runCatching { SecureVault(requireNotNull(context)) }.getOrNull() ?: return null
        vault = restored
        return restored
    }

    private fun enforceCaller(): String {
        val app = requireNotNull(context)
        if (Binder.getCallingUid() == app.applicationInfo.uid) return app.packageName
        val callerPackages = app.packageManager.getPackagesForUid(Binder.getCallingUid()).orEmpty()
        callerPackages.firstOrNull(ApprovedPlayerStore(app)::isApproved)?.let { return it }
        SecureAccessHistoryStore(app).record(callerPackages.firstOrNull() ?: "uid:${Binder.getCallingUid()}", null, false)
        throw SecurityException("This app is not approved for Secure Gallery")
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = throw UnsupportedOperationException()
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int = throw UnsupportedOperationException()

    companion object {
        fun uri(context: Context, item: SecureItem): Uri = Uri.Builder().scheme("content").authority("${context.packageName}.secure.media").appendPath("item").appendPath(item.id).build()
    }
}
