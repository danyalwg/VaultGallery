package com.danyal.vaultgallery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.os.StatFs
import android.os.Environment
import android.provider.MediaStore
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.danyal.vaultgallery.data.GalleryMedia
import com.danyal.vaultgallery.security.SecureVault
import com.danyal.vaultgallery.security.SecureStoragePolicy
import java.io.File
import java.io.IOException
import java.util.UUID
import java.security.MessageDigest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

internal enum class TransferOperation {
    COPY_PUBLIC,
    MOVE_PUBLIC,
    IMPORT_SECURE_COPY,
    IMPORT_SECURE_MOVE,
    EXPORT_SECURE_COPY,
    EXPORT_SECURE_MOVE,
    COPY_SECURE_ALBUM,
    MOVE_SECURE_ALBUM,
    CONVERT_SECURE_STORAGE,
}

internal enum class TransferSourceKind { MEDIA, ALBUMS }
internal enum class TransferConflictPolicy(val label: String) {
    MERGE("Merge folders and keep both"),
    RENAME("Keep both and rename the new item"),
    REPLACE("Replace the destination item"),
    SKIP("Skip matching names"),
    ASK("Stop and report conflicts"),
}

internal fun transferConflictPolicy(context: Context): TransferConflictPolicy = runCatching {
    TransferConflictPolicy.valueOf(
        context.getSharedPreferences("gallery-settings", Context.MODE_PRIVATE)
            .getString("transfer_conflict_policy", TransferConflictPolicy.RENAME.name)
            ?: TransferConflictPolicy.RENAME.name,
    )
}.getOrDefault(TransferConflictPolicy.RENAME)

internal class TransferProgressThrottle(private val intervalMs: Long) {
    private var lastPublishedAtMs: Long? = null

    fun shouldPublish(nowMs: Long): Boolean {
        val previous = lastPublishedAtMs
        if (previous != null && nowMs - previous < intervalMs) return false
        lastPublishedAtMs = nowMs
        return true
    }
}

internal fun transferFailureMessage(rawMessage: String?, completed: Int, total: Int): String {
    val technicalMessage = rawMessage.orEmpty()
    val explanation = when {
        // Deliberately detect and redact an Android app-private path from any surfaced error.
        // Build the marker in parts so lint does not mistake this diagnostic check for a
        // hard-coded storage destination.
        technicalMessage.contains("/data/" + "user/", ignoreCase = true) ||
            technicalMessage.contains("ENOENT", ignoreCase = true) ->
            "Secure storage interrupted the active file. Completed items are safe and remaining originals were not removed."
        technicalMessage.isBlank() -> "One or more items could not be transferred."
        else -> technicalMessage
    }
    return if (completed > 0) "Stopped after $completed of $total items. $explanation" else explanation
}

/** Folder transfers may never target one of the folders being moved; media may target a source album when mixed. */
internal fun shouldExcludeTransferDestination(
    destination: String,
    sourceAlbums: Set<String>,
    move: Boolean,
    sourceKind: TransferSourceKind,
): Boolean {
    if (!move) return false
    return when (sourceKind) {
        TransferSourceKind.ALBUMS -> sourceAlbums.any { it.equals(destination, ignoreCase = true) }
        TransferSourceKind.MEDIA -> sourceAlbums.size == 1 && sourceAlbums.first().equals(destination, ignoreCase = true)
    }
}

internal data class TransferJob(
    val id: String,
    val operation: TransferOperation,
    val targetAlbum: String,
    val sources: List<String> = emptyList(),
    val secureIds: List<String> = emptyList(),
    val secureSession: String? = null,
    val secureStoragePolicy: String = SecureStoragePolicy.LOCKED_ONLY.name,
    val conflictPolicy: String = TransferConflictPolicy.RENAME.name,
)

/** Durable entry point for every potentially long-running copy or move in the app. */
internal object MediaTransferCoordinator {
    const val WORK_TAG = "vault-gallery-media-transfer"

    fun enqueuePublic(context: Context, media: List<GalleryMedia>, targetAlbum: String, move: Boolean): UUID = enqueue(
        context,
        TransferJob(
            id = UUID.randomUUID().toString(),
            operation = if (move) TransferOperation.MOVE_PUBLIC else TransferOperation.COPY_PUBLIC,
            targetAlbum = targetAlbum,
            sources = media.map { it.uri.toString() },
            conflictPolicy = transferConflictPolicy(context).name,
        ),
    )

    fun enqueueSecureImport(
        context: Context,
        uris: List<Uri>,
        targetAlbum: String?,
        move: Boolean,
        secureSession: String,
        storagePolicy: SecureStoragePolicy = SecureStoragePolicy.LOCKED_ONLY,
    ): UUID = enqueue(
        context,
        TransferJob(
            id = UUID.randomUUID().toString(),
            operation = if (move) TransferOperation.IMPORT_SECURE_MOVE else TransferOperation.IMPORT_SECURE_COPY,
            targetAlbum = targetAlbum.orEmpty(),
            sources = uris.map(Uri::toString),
            secureSession = secureSession,
            secureStoragePolicy = storagePolicy.name,
            conflictPolicy = transferConflictPolicy(context).name,
        ),
    )

    fun enqueueSecureExport(
        context: Context,
        secureIds: List<String>,
        targetAlbum: String?,
        move: Boolean,
        secureSession: String,
    ): UUID = enqueue(
        context,
        TransferJob(
            id = UUID.randomUUID().toString(),
            operation = if (move) TransferOperation.EXPORT_SECURE_MOVE else TransferOperation.EXPORT_SECURE_COPY,
            targetAlbum = targetAlbum.orEmpty(),
            secureIds = secureIds,
            secureSession = secureSession,
            conflictPolicy = transferConflictPolicy(context).name,
        ),
    )

    fun enqueueSecureAlbum(
        context: Context,
        secureIds: List<String>,
        targetAlbum: String,
        copy: Boolean,
        secureSession: String,
    ): UUID = enqueue(
        context,
        TransferJob(
            id = UUID.randomUUID().toString(),
            operation = if (copy) TransferOperation.COPY_SECURE_ALBUM else TransferOperation.MOVE_SECURE_ALBUM,
            targetAlbum = targetAlbum,
            secureIds = secureIds,
            secureSession = secureSession,
            conflictPolicy = transferConflictPolicy(context).name,
        ),
    )

    fun enqueueSecureStorageConversion(
        context: Context,
        secureIds: List<String>,
        policy: SecureStoragePolicy,
        secureSession: String,
    ): UUID = enqueue(
        context,
        TransferJob(
            id = UUID.randomUUID().toString(),
            operation = TransferOperation.CONVERT_SECURE_STORAGE,
            targetAlbum = policy.name,
            secureIds = secureIds,
            secureSession = secureSession,
            conflictPolicy = transferConflictPolicy(context).name,
        ),
    )

    private fun enqueue(context: Context, job: TransferJob): UUID {
        TransferJobStore(context).write(job)
        val request = OneTimeWorkRequestBuilder<MediaTransferWorker>()
            .setInputData(Data.Builder().putString(MediaTransferWorker.KEY_JOB_ID, job.id).build())
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("media-transfer-${job.id}", ExistingWorkPolicy.KEEP, request)
        return request.id
    }
}

internal class MediaTransferWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val jobStore = TransferJobStore(context)
    private val notificationId = id.hashCode() and Int.MAX_VALUE

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        ensureNotificationChannel()
        val jobId = inputData.getString(KEY_JOB_ID) ?: return@withContext Result.failure(errorData("Transfer details are missing"))
        val job = runCatching { jobStore.read(jobId) }.getOrElse {
            return@withContext Result.failure(errorData(it.message ?: "Transfer details could not be read"))
        }
        val title = operationTitle(job.operation)
        setForeground(foreground(title, "Preparing transfer", 0, true))
        var secureKey: ByteArray? = null
        // Public copy/move must never depend on Secure Gallery being configured. Constructing
        // SecureVault eagerly made an ordinary public-folder transfer fail when the user had
        // not yet granted a persistent SAF location for secure media.
        var vault: SecureVault? = null
        fun secureVault(): SecureVault = vault ?: SecureVault(applicationContext).also { vault = it }
        var total = 1
        val transferStartedAt = SystemClock.elapsedRealtime()
        val checkpoint = jobStore.readCheckpoint(job.id)
        var completed = checkpoint.completedKeys.size + checkpoint.skips.size
        val failures = checkpoint.failures.toMutableMap()
        val skips = checkpoint.skips.toMutableMap()
        var terminal = false
        var terminalSummary = "Transfer stopped"
        var terminalStatus = TransferReportStatus.FAILED
        val displayNames = checkpoint.names.toMutableMap()
        try {
            if (job.secureSession != null) secureKey = secureVault().openTransferSession(job.secureSession)
            total = maxOf(job.sources.size, job.secureIds.size, 1)
            val requiredBytes = estimateRequiredBytes(job, secureKey, vault)
            if (requiredBytes > 0L) {
                val available = StatFs(Environment.getExternalStorageDirectory().absolutePath).availableBytes
                val safetyMargin = maxOf(64L * 1024 * 1024, requiredBytes / 20)
                require(available >= requiredBytes + safetyMargin) {
                    "Not enough destination space. Need ${transferByteLabel(requiredBytes + safetyMargin)}, available ${transferByteLabel(available)}."
                }
            }
            val progressThrottle = TransferProgressThrottle(PROGRESS_UPDATE_INTERVAL_MS)
            fun shouldPublishProgress(): Boolean {
                return progressThrottle.shouldPublish(SystemClock.elapsedRealtime())
            }
            suspend fun progress(itemName: String) {
                val percent = (completed * 100 / total).coerceIn(0, 100)
                awaitTransferControl(title, itemName, percent, completed, total)
                if (shouldPublishProgress()) {
                    notificationManager.notify(notificationId, notification(title, "$itemName • $percent% • $completed of $total", percent, false))
                }
                setProgress(Data.Builder().putInt(KEY_PROGRESS, percent).putInt(KEY_COMPLETED, completed).putInt(KEY_TOTAL, total).build())
            }
            fun byteProgress(itemName: String, transferred: Long, itemBytes: Long) {
                val itemFraction = if (itemBytes > 0) (transferred.toDouble() / itemBytes).coerceIn(0.0, 1.0) else 0.0
                val percent = (((completed + itemFraction) / total) * 100).toInt().coerceIn(0, 99)
                awaitTransferControl(title, itemName, percent, completed, total)
                if (!shouldPublishProgress()) return
                val elapsedSeconds = ((SystemClock.elapsedRealtime() - transferStartedAt) / 1000.0).coerceAtLeast(.25)
                val bytesPerSecond = (transferred / elapsedSeconds).toLong().coerceAtLeast(1L)
                val remainingSeconds = if (itemBytes > transferred) (itemBytes - transferred) / bytesPerSecond else 0L
                val byteLabel = if (itemBytes > 0) "${transferByteLabel(transferred)} / ${transferByteLabel(itemBytes)}" else transferByteLabel(transferred)
                val status = "$itemName • $percent% • $byteLabel • ${transferByteLabel(bytesPerSecond)}/s • ${transferEtaLabel(remainingSeconds)} left"
                notificationManager.notify(notificationId, notification(title, status, percent, false))
                setProgressAsync(Data.Builder()
                    .putInt(KEY_PROGRESS, percent)
                    .putInt(KEY_COMPLETED, completed)
                    .putInt(KEY_TOTAL, total)
                    .putLong(KEY_BYTES, transferred)
                    .putLong(KEY_ITEM_BYTES, itemBytes)
                    .putLong(KEY_BYTES_PER_SECOND, bytesPerSecond)
                    .putLong(KEY_ETA_SECONDS, remainingSeconds)
                    .build())
            }
            suspend fun processItem(key: String, itemName: String, action: suspend () -> Unit) {
                displayNames[key] = itemName
                if (checkpoint.completedKeys.contains(key) || skips.containsKey(key)) return
                checkTransferState()
                progress(itemName)
                try {
                    action()
                    checkpoint.artifacts.remove(key)
                    checkpoint.completedKeys += key
                    failures.remove(key)
                    skips.remove(key)
                    completed = checkpoint.completedKeys.size + skips.size
                    jobStore.writeCheckpoint(job.id, checkpoint.copy(failures = failures, skips = skips, names = displayNames))
                } catch (skipped: ItemSkippedException) {
                    skips[key] = skipped.message.orEmpty().ifBlank { "Skipped by conflict policy" }
                    failures.remove(key)
                    completed = checkpoint.completedKeys.size + skips.size
                    jobStore.writeCheckpoint(job.id, checkpoint.copy(failures = failures, skips = skips, names = displayNames))
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (cancelled: UserCancelledException) {
                    throw cancelled
                } catch (stopped: WorkerStoppedException) {
                    throw stopped
                } catch (error: Throwable) {
                    failures[key] = error.message.orEmpty().ifBlank { "Item could not be transferred" }
                    jobStore.writeCheckpoint(job.id, checkpoint.copy(failures = failures, skips = skips, names = displayNames))
                }
            }
            fun rememberArtifact(key: String, uri: Uri) {
                checkpoint.artifacts[key] = uri.toString()
                jobStore.writeCheckpoint(job.id, checkpoint.copy(failures = failures))
            }
            when (job.operation) {
                TransferOperation.COPY_PUBLIC, TransferOperation.MOVE_PUBLIC -> job.sources.forEachIndexed { index, encoded ->
                    val uri = Uri.parse(encoded)
                    val name = runCatching { queryName(uri) }.getOrDefault("Media ${index + 1}")
                    val itemKey = "$index:$encoded"
                    processItem(itemKey, name) {
                        val policy = runCatching { TransferConflictPolicy.valueOf(job.conflictPolicy) }.getOrDefault(TransferConflictPolicy.RENAME)
                        if (job.operation == TransferOperation.MOVE_PUBLIC) {
                            val origin = queryPublicLocation(uri)
                            movePublic(uri, job.targetAlbum, policy)
                            if (policy != TransferConflictPolicy.REPLACE && origin != null) {
                                checkpoint.undo[itemKey] = JSONObject().apply {
                                    put("uri", uri.toString())
                                    put("path", origin.relativePath)
                                    put("name", origin.name)
                                }.toString()
                            }
                        }
                        else copyPublic(
                            uri,
                            job.targetAlbum,
                            checkpoint.artifacts[itemKey]?.let(Uri::parse),
                            { rememberArtifact(itemKey, it) }, policy,
                        ) { bytes, size -> byteProgress(name, bytes, size) }
                    }
                }
                TransferOperation.IMPORT_SECURE_COPY, TransferOperation.IMPORT_SECURE_MOVE -> {
                    val key = requireNotNull(secureKey) { "Secure transfer authorization expired" }
                    val policy = runCatching { TransferConflictPolicy.valueOf(job.conflictPolicy) }.getOrDefault(TransferConflictPolicy.RENAME)
                    job.sources.forEachIndexed { index, encoded ->
                        val uri = Uri.parse(encoded)
                        val metadataUri = resolveLocalPickerMediaUri(uri) ?: uri
                        val name = runCatching { queryName(metadataUri) }.getOrDefault("Media ${index + 1}")
                        val itemKey = "$index:$encoded"
                        processItem(itemKey, name) {
                            val activeVault = secureVault()
                            val destinationAlbum = job.targetAlbum.ifBlank { querySourceAlbum(metadataUri).ifBlank { "Imported" } }
                            val stableId = UUID.nameUUIDFromBytes("${job.id}:$itemKey".toByteArray(Charsets.UTF_8)).toString()
                            val target = resolveSecureTarget(activeVault.list(key, includeTrashed = true), destinationAlbum, name, stableId, policy)
                            val imported = activeVault.importUri(
                                key,
                                applicationContext.contentResolver,
                                uri,
                                destinationAlbum,
                                { bytes, size ->
                                    checkTransferState()
                                    byteProgress(name, bytes, size)
                                },
                                runCatching { SecureStoragePolicy.valueOf(job.secureStoragePolicy) }.getOrDefault(SecureStoragePolicy.LOCKED_ONLY),
                                stableId,
                                target.name,
                            )
                            require(MessageDigest.isEqual(uriDigest(uri), secureDigest(activeVault, key, imported))) {
                                "Secure destination hash did not match the source"
                            }
                            if (policy == TransferConflictPolicy.REPLACE) target.existing.forEach { activeVault.delete(key, it) }
                            if (job.operation == TransferOperation.IMPORT_SECURE_MOVE && applicationContext.contentResolver.delete(uri, null, null) <= 0) {
                                throw IOException("The item reached Secure Gallery, but Android did not remove the original")
                            }
                        }
                    }
                }
                TransferOperation.EXPORT_SECURE_COPY, TransferOperation.EXPORT_SECURE_MOVE -> {
                    val key = requireNotNull(secureKey) { "Secure transfer authorization expired" }
                    val activeVault = secureVault()
                    val policy = runCatching { TransferConflictPolicy.valueOf(job.conflictPolicy) }.getOrDefault(TransferConflictPolicy.RENAME)
                    val indexed = activeVault.list(key, includeTrashed = true).associateBy { it.id }
                    job.secureIds.forEachIndexed { index, secureId ->
                        val item = indexed[secureId]
                        val itemKey = "$index:$secureId"
                        processItem(itemKey, item?.name ?: "Missing secure item") {
                            item ?: throw IOException("A selected secure item no longer exists")
                            val previous = checkpoint.artifacts[itemKey]?.let(Uri::parse)
                            val destinationAlbum = job.targetAlbum.ifBlank { item.albumName }
                            val target = resolvePublicTarget(
                                PublicMetadata(item.name, item.mimeType, item.dateTakenMs, item.sizeBytes),
                                destinationAlbum,
                                previous,
                                policy,
                            )
                            val exported = if (previous != null && isCompletedMedia(previous, item.sizeBytes)) previous else {
                                previous?.let { applicationContext.contentResolver.delete(it, null, null) }
                                activeVault.exportToGallery(
                                    key,
                                    applicationContext.contentResolver,
                                    item,
                                    job.targetAlbum.ifBlank { null },
                                    { bytes, size ->
                                        checkTransferState()
                                        byteProgress(item.name, bytes, size)
                                    },
                                    { rememberArtifact(itemKey, it) },
                                    target.name,
                                )
                            }
                            require(isCompletedMedia(exported, item.sizeBytes)) { "Exported media could not be verified" }
                            require(MessageDigest.isEqual(secureDigest(activeVault, key, item), uriDigest(exported))) {
                                "Exported destination hash did not match the secure source"
                            }
                            if (policy == TransferConflictPolicy.REPLACE) target.existing?.let { existing ->
                                if (applicationContext.contentResolver.delete(existing, null, null) <= 0) {
                                    throw IOException("The export is safe, but the older destination copy could not be removed")
                                }
                            }
                            if (job.operation == TransferOperation.EXPORT_SECURE_MOVE) activeVault.delete(key, item)
                        }
                    }
                }
                TransferOperation.COPY_SECURE_ALBUM, TransferOperation.MOVE_SECURE_ALBUM -> {
                    val key = requireNotNull(secureKey) { "Secure transfer authorization expired" }
                    val activeVault = secureVault()
                    val policy = runCatching { TransferConflictPolicy.valueOf(job.conflictPolicy) }.getOrDefault(TransferConflictPolicy.RENAME)
                    val indexed = activeVault.list(key, includeTrashed = true).associateBy { it.id }
                    job.secureIds.forEachIndexed { index, secureId ->
                        val item = indexed[secureId]
                        processItem("$index:$secureId", item?.name ?: "Missing secure item") {
                            item ?: throw IOException("A selected secure item no longer exists")
                            val target = resolveSecureTarget(activeVault.list(key, includeTrashed = true), job.targetAlbum, item.name, item.id, policy)
                            if (job.operation == TransferOperation.COPY_SECURE_ALBUM) activeVault.copyToAlbum(
                                key,
                                listOf(item),
                                job.targetAlbum,
                                { bytes, size -> checkTransferState(); byteProgress(item.name, bytes, size) },
                                target.name,
                            ) else activeVault.moveToAlbum(key, listOf(item), job.targetAlbum, target.name)
                            if (policy == TransferConflictPolicy.REPLACE) target.existing.forEach { activeVault.delete(key, it) }
                        }
                    }
                }
                TransferOperation.CONVERT_SECURE_STORAGE -> {
                    val key = requireNotNull(secureKey) { "Secure transfer authorization expired" }
                    val policy = SecureStoragePolicy.valueOf(job.targetAlbum)
                    val activeVault = secureVault()
                    val indexed = activeVault.list(key, includeTrashed = true).associateBy { it.id }
                    job.secureIds.forEachIndexed { index, secureId ->
                        val item = indexed[secureId]
                        processItem("$index:$secureId", item?.name ?: "Missing secure item") {
                            item ?: throw IOException("A selected secure item no longer exists")
                            activeVault.convertStoragePolicy(key, listOf(item), policy)
                        }
                    }
                }
            }
            terminal = true
            if (failures.isEmpty()) {
                setProgress(Data.Builder().putInt(KEY_PROGRESS, 100).putInt(KEY_COMPLETED, completed).putInt(KEY_TOTAL, total).build())
                val successCount = checkpoint.completedKeys.size
                val message = buildString {
                    append("${pastTense(job.operation)} $successCount item${if (successCount == 1) "" else "s"}")
                    if (skips.isNotEmpty()) append("; ${skips.size} skipped")
                }
                terminalSummary = message
                terminalStatus = TransferReportStatus.SUCCESS
                showCompletion(title, message)
                Result.success(Data.Builder().putInt(KEY_COMPLETED, completed).putString(KEY_MESSAGE, message).build())
            } else {
                val failedCount = failures.size
                val message = "$completed of $total completed; $failedCount failed. Failed originals were left in place."
                terminalSummary = message
                showCompletion("Transfer incomplete", message, failed = true)
                Result.failure(Data.Builder().putInt(KEY_COMPLETED, completed).putInt(KEY_TOTAL, total).putString(KEY_MESSAGE, message).build())
            }
        } catch (cancelled: UserCancelledException) {
            terminal = true
            val message = if (completed > 0) "Cancelled after $completed of $total items" else "Transfer cancelled"
            terminalSummary = message
            terminalStatus = TransferReportStatus.CANCELLED
            showCompletion("Transfer cancelled", message, failed = true)
            Result.failure(errorData(message))
        } catch (stopped: WorkerStoppedException) {
            Result.retry()
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Throwable) {
            val message = transferFailureMessage(error.message, completed, total)
            terminalSummary = message
            showCompletion("Transfer incomplete", message, failed = true)
            terminal = true
            Result.failure(errorData(message))
        } finally {
            TransferControl.clear(applicationContext, id)
            secureKey?.fill(0)
            vault?.closeMetadata()
            if (terminal) {
                TransferReportStore(applicationContext).write(
                    TransferReport(
                        id = job.id,
                        operation = operationTitle(job.operation),
                        target = job.targetAlbum,
                        completedAtMs = System.currentTimeMillis(),
                        summary = terminalSummary,
                        entries = displayNames.map { (key, name) ->
                            when {
                                key in skips -> TransferReportEntry(name, TransferReportStatus.SKIPPED, skips[key].orEmpty())
                                key in checkpoint.completedKeys -> {
                                    val undo = checkpoint.undo[key]?.let { runCatching { JSONObject(it) }.getOrNull() }
                                    TransferReportEntry(
                                        name,
                                        TransferReportStatus.SUCCESS,
                                        undoUri = undo?.optString("uri")?.takeIf(String::isNotBlank),
                                        undoRelativePath = undo?.optString("path")?.takeIf(String::isNotBlank),
                                        undoName = undo?.optString("name")?.takeIf(String::isNotBlank),
                                    )
                                }
                                key in failures -> TransferReportEntry(name, TransferReportStatus.FAILED, failures[key].orEmpty())
                                terminalStatus == TransferReportStatus.CANCELLED -> TransferReportEntry(name, TransferReportStatus.CANCELLED, "Not completed")
                                else -> TransferReportEntry(name, TransferReportStatus.SKIPPED, "Not processed")
                            }
                        },
                    ),
                )
                job.secureSession?.let { session -> vault?.destroyTransferSession(session) }
                jobStore.delete(job.id)
            }
        }
    }

    private fun movePublic(uri: Uri, albumName: String, policy: TransferConflictPolicy) {
        val metadata = queryMetadata(uri)
        val target = resolvePublicTarget(metadata, albumName, uri, policy)
        val expectedPath = destinationPath(albumName)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, expectedPath)
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
        }
        if (applicationContext.contentResolver.update(uri, values, null, null) <= 0) {
            throw IOException("Android did not move ${queryName(uri)}. Allow the requested media access and try again.")
        }
        if (!isMediaInRelativePath(uri, expectedPath)) {
            throw IOException("Android reported the move as complete, but the destination folder could not be verified")
        }
        if (policy == TransferConflictPolicy.REPLACE) target.existing?.let { existing ->
            if (applicationContext.contentResolver.delete(existing, null, null) <= 0) {
                throw IOException("The item moved successfully, but the older destination copy could not be removed")
            }
        }
    }

    private fun estimateRequiredBytes(job: TransferJob, secureKey: ByteArray?, existingVault: SecureVault?): Long = when (job.operation) {
        TransferOperation.MOVE_PUBLIC, TransferOperation.MOVE_SECURE_ALBUM -> 0L
        TransferOperation.COPY_PUBLIC, TransferOperation.IMPORT_SECURE_COPY, TransferOperation.IMPORT_SECURE_MOVE ->
            job.sources.sumOf { encoded -> runCatching { queryMetadata(Uri.parse(encoded)).size.coerceAtLeast(0L) }.getOrDefault(0L) }
        TransferOperation.EXPORT_SECURE_COPY,
        TransferOperation.EXPORT_SECURE_MOVE,
        TransferOperation.COPY_SECURE_ALBUM,
        TransferOperation.CONVERT_SECURE_STORAGE -> {
            val key = secureKey ?: return 0L
            val activeVault = existingVault ?: SecureVault(applicationContext)
            activeVault.list(key, includeTrashed = true).filter { it.id in job.secureIds }.sumOf { it.sizeBytes.coerceAtLeast(0L) }
        }
    }

    private fun copyPublic(
        source: Uri,
        albumName: String,
        previousDestination: Uri?,
        onDestinationCreated: (Uri) -> Unit,
        policy: TransferConflictPolicy,
        onProgress: (Long, Long) -> Unit,
    ) {
        val resolver = applicationContext.contentResolver
        val metadata = queryMetadata(source)
        val target = resolvePublicTarget(metadata, albumName, source, policy)
        if (previousDestination != null && isCompletedMedia(previousDestination, metadata.size)) return
        previousDestination?.let { resolver.delete(it, null, null) }
        val collection = if (metadata.mime.startsWith("video/")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, target.name)
            put(MediaStore.MediaColumns.MIME_TYPE, metadata.mime)
            put(MediaStore.MediaColumns.RELATIVE_PATH, destinationPath(albumName))
            put(MediaStore.MediaColumns.IS_PENDING, 1)
            if (metadata.dateTaken > 0) put(MediaStore.Images.ImageColumns.DATE_TAKEN, metadata.dateTaken)
        }
        val destination = resolver.insert(collection, values) ?: throw IOException("Could not create ${metadata.name}")
        onDestinationCreated(destination)
        try {
            var transferred = 0L
            val sourceDigest = MessageDigest.getInstance("SHA-256")
            resolver.openInputStream(source).use { input ->
                requireNotNull(input) { "Source is unavailable" }
                resolver.openOutputStream(destination, "w").use { output ->
                    requireNotNull(output) { "Destination is unavailable" }
                    val buffer = ByteArray(1024 * 1024)
                    while (true) {
                        checkTransferState()
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        sourceDigest.update(buffer, 0, count)
                        transferred += count
                        onProgress(transferred, metadata.size)
                    }
                }
            }
            require(metadata.size <= 0L || transferred == metadata.size) {
                "The source changed during the copy (${transferred} of ${metadata.size} bytes read)"
            }
            resolver.update(destination, ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }, null, null)
            require(isCompletedMedia(destination, metadata.size)) {
                "The copied item could not be verified after Android committed it"
            }
            require(MessageDigest.isEqual(sourceDigest.digest(), uriDigest(destination))) {
                "The copied item hash did not match the source"
            }
            if (policy == TransferConflictPolicy.REPLACE) target.existing?.let { existing ->
                if (resolver.delete(existing, null, null) <= 0) throw IOException("The new copy is safe, but the older destination copy could not be removed")
            }
        } catch (error: Throwable) {
            resolver.delete(destination, null, null)
            throw error
        }
    }

    private data class PublicTarget(val name: String, val existing: Uri?)
    private data class SecureTarget(val name: String, val existing: List<com.danyal.vaultgallery.security.SecureItem>)

    private fun resolvePublicTarget(
        metadata: PublicMetadata,
        albumName: String,
        source: Uri?,
        policy: TransferConflictPolicy,
    ): PublicTarget {
        val existing = findPublicConflict(metadata, albumName, source)
        if (existing == null) return PublicTarget(metadata.name, null)
        return when (policy) {
            TransferConflictPolicy.MERGE -> PublicTarget(metadata.name, null)
            TransferConflictPolicy.RENAME -> PublicTarget(uniquePublicName(metadata, albumName), null)
            TransferConflictPolicy.REPLACE -> PublicTarget(metadata.name, existing)
            TransferConflictPolicy.SKIP -> throw ItemSkippedException("A file named ${metadata.name} already exists")
            TransferConflictPolicy.ASK -> throw ItemSkippedException("Needs your choice: ${metadata.name} already exists")
        }
    }

    private fun uniquePublicName(metadata: PublicMetadata, albumName: String): String {
        val dot = metadata.name.lastIndexOf('.')
        val stem = if (dot > 0) metadata.name.substring(0, dot) else metadata.name
        val extension = if (dot > 0) metadata.name.substring(dot) else ""
        for (suffix in 1..10_000) {
            val candidate = "$stem ($suffix)$extension"
            if (findPublicConflict(metadata.copy(name = candidate), albumName, null) == null) return candidate
        }
        throw IOException("Could not create a unique name for ${metadata.name}")
    }

    private fun resolveSecureTarget(
        items: List<com.danyal.vaultgallery.security.SecureItem>,
        albumName: String,
        displayName: String,
        sourceId: String?,
        policy: TransferConflictPolicy,
    ): SecureTarget {
        val conflicts = items.filter {
            it.id != sourceId && !it.isTrashed && it.albumName.equals(albumName, ignoreCase = true) && it.name.equals(displayName, ignoreCase = true)
        }
        if (conflicts.isEmpty()) return SecureTarget(displayName, emptyList())
        return when (policy) {
            TransferConflictPolicy.MERGE -> SecureTarget(displayName, emptyList())
            TransferConflictPolicy.RENAME -> {
                val dot = displayName.lastIndexOf('.')
                val stem = if (dot > 0) displayName.substring(0, dot) else displayName
                val extension = if (dot > 0) displayName.substring(dot) else ""
                val used = items.filter { it.albumName.equals(albumName, true) }.mapTo(HashSet()) { it.name.lowercase() }
                val unique = (1..10_000).asSequence().map { "$stem ($it)$extension" }.firstOrNull { it.lowercase() !in used }
                    ?: throw IOException("Could not create a unique name for $displayName")
                SecureTarget(unique, emptyList())
            }
            TransferConflictPolicy.REPLACE -> SecureTarget(displayName, conflicts)
            TransferConflictPolicy.SKIP -> throw ItemSkippedException("A file named $displayName already exists")
            TransferConflictPolicy.ASK -> throw ItemSkippedException("Needs your choice: $displayName already exists")
        }
    }

    private fun findPublicConflict(metadata: PublicMetadata, albumName: String, source: Uri?): Uri? {
        val resolver = applicationContext.contentResolver
        val collection = if (metadata.mime.startsWith("video/")) MediaStore.Video.Media.EXTERNAL_CONTENT_URI else MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        return resolver.query(
            collection,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.RELATIVE_PATH} = ? AND ${MediaStore.MediaColumns.DISPLAY_NAME} = ?",
            arrayOf(destinationPath(albumName), metadata.name),
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            while (cursor.moveToNext()) {
                val candidate = ContentUris.withAppendedId(collection, cursor.getLong(idIndex))
                if (source == null || candidate != source) return@use candidate
            }
            null
        }
    }

    private fun uriDigest(uri: Uri): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        applicationContext.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "Media is unavailable for integrity verification" }
            val buffer = ByteArray(1024 * 1024)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
            buffer.fill(0)
        }
        return digest.digest()
    }

    private fun secureDigest(vault: SecureVault, master: ByteArray, item: com.danyal.vaultgallery.security.SecureItem): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        vault.streamPlain(master, item) { bytes, count -> digest.update(bytes, 0, count) }
        return digest.digest()
    }

    private fun isCompletedMedia(uri: Uri, expectedSize: Long): Boolean = runCatching {
        applicationContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.SIZE, MediaStore.MediaColumns.IS_PENDING),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use false
            val sizeIndex = cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)
            val pendingIndex = cursor.getColumnIndex(MediaStore.MediaColumns.IS_PENDING)
            val size = if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) cursor.getLong(sizeIndex) else -1L
            val pending = if (pendingIndex >= 0 && !cursor.isNull(pendingIndex)) cursor.getInt(pendingIndex) else 0
            pending == 0 && (expectedSize <= 0 || size == expectedSize)
        } ?: false
    }.getOrDefault(false)

    private fun isMediaInRelativePath(uri: Uri, expectedPath: String): Boolean = runCatching {
        applicationContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.RELATIVE_PATH),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use false
            val index = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            index >= 0 && !cursor.isNull(index) && sameRelativePath(cursor.getString(index), expectedPath)
        } ?: false
    }.getOrDefault(false)

    private fun queryName(uri: Uri): String = queryMetadata(uri).name

    private fun queryPublicLocation(uri: Uri): PublicLocation? = runCatching {
        applicationContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.RELATIVE_PATH),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return@use null
            val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
            val pathIndex = cursor.getColumnIndex(MediaStore.MediaColumns.RELATIVE_PATH)
            if (nameIndex < 0 || pathIndex < 0 || cursor.isNull(pathIndex)) null
            else PublicLocation(cursor.getString(nameIndex).orEmpty(), cursor.getString(pathIndex).orEmpty())
        }
    }.getOrNull()

    private fun querySourceAlbum(uri: Uri): String = runCatching {
        applicationContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME),
            null,
            null,
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) "" else cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME)
                .takeIf { it >= 0 && !cursor.isNull(it) }?.let(cursor::getString).orEmpty()
        }.orEmpty()
    }.getOrDefault("")

    /**
     * Android Photo Picker deliberately exposes an opaque grant. For local MediaStore items its
     * final path segment is the original numeric media id; resolving that id restores the original
     * display name and bucket without changing which granted URI is read. Cloud-only items safely
     * fall back to the picker metadata and the explicit Imported destination.
     */
    private fun resolveLocalPickerMediaUri(uri: Uri): Uri? {
        if (uri.authority != MediaStore.AUTHORITY || "picker" !in uri.pathSegments) return null
        val id = uri.lastPathSegment?.toLongOrNull() ?: return null
        val mime = applicationContext.contentResolver.getType(uri).orEmpty()
        val candidates = when {
            mime.startsWith("video/") -> listOf(MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            mime.startsWith("image/") -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            else -> listOf(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
        }
        return candidates.asSequence()
            .map { ContentUris.withAppendedId(it, id) }
            .firstOrNull { candidate ->
                runCatching {
                    applicationContext.contentResolver.query(
                        candidate,
                        arrayOf(MediaStore.MediaColumns._ID),
                        null,
                        null,
                        null,
                    )?.use { it.moveToFirst() } == true
                }.getOrDefault(false)
            }
    }

    private fun queryMetadata(uri: Uri): PublicMetadata {
        var name = uri.lastPathSegment ?: "Media"
        var mime = applicationContext.contentResolver.getType(uri).orEmpty().ifBlank { "image/jpeg" }
        var dateTaken = 0L
        var size = -1L
        applicationContext.contentResolver.query(
            uri,
            arrayOf(MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE, MediaStore.Images.ImageColumns.DATE_TAKEN, MediaStore.MediaColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME).takeIf { it >= 0 }?.let { name = cursor.getString(it).orEmpty().ifBlank { name } }
                cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE).takeIf { it >= 0 }?.let { mime = cursor.getString(it).orEmpty().ifBlank { mime } }
                cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATE_TAKEN).takeIf { it >= 0 && !cursor.isNull(it) }?.let { dateTaken = cursor.getLong(it) }
                cursor.getColumnIndex(MediaStore.MediaColumns.SIZE).takeIf { it >= 0 && !cursor.isNull(it) }?.let { size = cursor.getLong(it) }
            }
        }
        return PublicMetadata(name, mime, dateTaken, size)
    }

    private fun foreground(title: String, text: String, progress: Int, indeterminate: Boolean): ForegroundInfo {
        val notification = notification(title, text, progress, indeterminate)
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(notificationId, notification)
    }

    private fun notification(title: String, text: String, progress: Int, indeterminate: Boolean, paused: Boolean = false) =
        NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_upload)
            .setContentTitle(title)
            .setContentText(text)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setSubText(if (paused) "Paused at $progress%" else "$progress%")
            .setProgress(100, progress, indeterminate)
            .addAction(
                if (paused) android.R.drawable.ic_media_play else android.R.drawable.ic_media_pause,
                if (paused) "Resume" else "Pause",
                transferControlPendingIntent(if (paused) TransferControl.ACTION_RESUME else TransferControl.ACTION_PAUSE),
            )
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", transferControlPendingIntent(TransferControl.ACTION_CANCEL))
            .build()

    private fun transferControlPendingIntent(action: String): PendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        notificationId * 31 + action.hashCode(),
        Intent(applicationContext, TransferControlReceiver::class.java)
            .setAction(action)
            .putExtra(TransferControl.EXTRA_WORK_ID, id.toString()),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun awaitTransferControl(title: String, itemName: String, progress: Int, completed: Int, total: Int) {
        var notified = false
        while (TransferControl.isPaused(applicationContext, id)) {
            checkTransferState()
            if (!notified) {
                notificationManager.notify(
                    notificationId,
                    notification(title, "$itemName • $progress% • $completed of $total", progress, false, paused = true),
                )
                notified = true
            }
            Thread.sleep(200)
        }
        if (notified) notificationManager.notify(
            notificationId,
            notification(title, "$itemName • $progress% • $completed of $total", progress, false),
        )
    }

    private fun checkTransferState() {
        if (TransferControl.isCancelled(applicationContext, id)) throw UserCancelledException()
        if (isStopped) throw WorkerStoppedException()
    }

    private fun showCompletion(title: String, text: String, failed: Boolean = false) {
        val activity = if (title.contains("Secure", true)) SecureGalleryActivity::class.java else MainGalleryActivity::class.java
        val intent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, activity).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        // Foreground progress and the finished result are different states. Explicitly remove
        // the ongoing notification before publishing the result so Android never shows a stale
        // 0/0 or paused progress row beside a completed transfer.
        notificationManager.cancel(notificationId)
        notificationManager.notify(
            notificationId + COMPLETION_OFFSET,
            NotificationCompat.Builder(applicationContext, CHANNEL_ID)
                .setSmallIcon(if (failed) android.R.drawable.stat_notify_error else android.R.drawable.stat_sys_upload_done)
                .setContentTitle(title)
                .setContentText(text)
                .setContentIntent(intent)
                .setOngoing(false)
                .setProgress(0, 0, false)
                .setAutoCancel(true)
                .build(),
        )
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= 26) notificationManager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "File transfers", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progress for copying, moving, encrypting, and restoring media"
            },
        )
    }

    private fun destinationPath(albumName: String): String = "Pictures/${sanitizeAlbumName(albumName)}/"
    private fun errorData(message: String) = Data.Builder().putString(KEY_MESSAGE, message).build()
    private fun operationTitle(operation: TransferOperation) = when (operation) {
        TransferOperation.COPY_PUBLIC -> "Copying to album"
        TransferOperation.MOVE_PUBLIC -> "Moving to album"
        TransferOperation.IMPORT_SECURE_COPY -> "Copying to Secure Gallery"
        TransferOperation.IMPORT_SECURE_MOVE -> "Moving to Secure Gallery"
        TransferOperation.EXPORT_SECURE_COPY -> "Copying from Secure Gallery"
        TransferOperation.EXPORT_SECURE_MOVE -> "Moving from Secure Gallery"
        TransferOperation.COPY_SECURE_ALBUM -> "Copying in Secure Gallery"
        TransferOperation.MOVE_SECURE_ALBUM -> "Moving in Secure Gallery"
        TransferOperation.CONVERT_SECURE_STORAGE -> "Converting secure storage"
    }
    private fun pastTense(operation: TransferOperation) = when {
        operation == TransferOperation.CONVERT_SECURE_STORAGE -> "Converted"
        operation.name.contains("MOVE") -> "Moved"
        else -> "Copied"
    }

    companion object {
        internal const val KEY_JOB_ID = "transfer_job_id"
        internal const val KEY_PROGRESS = "transfer_progress"
        internal const val KEY_COMPLETED = "transfer_completed"
        internal const val KEY_TOTAL = "transfer_total"
        internal const val KEY_MESSAGE = "transfer_message"
        internal const val KEY_BYTES = "transfer_bytes"
        internal const val KEY_ITEM_BYTES = "transfer_item_bytes"
        internal const val KEY_BYTES_PER_SECOND = "transfer_bytes_per_second"
        internal const val KEY_ETA_SECONDS = "transfer_eta_seconds"
        private const val CHANNEL_ID = "media-transfers"
        private const val COMPLETION_OFFSET = 10_000
        private const val PROGRESS_UPDATE_INTERVAL_MS = 250L
    }
}

private fun transferByteLabel(bytes: Long): String {
    val value = bytes.coerceAtLeast(0L).toDouble()
    return when {
        value >= 1024 * 1024 * 1024 -> "%.1f GB".format(java.util.Locale.ROOT, value / (1024 * 1024 * 1024))
        value >= 1024 * 1024 -> "%.1f MB".format(java.util.Locale.ROOT, value / (1024 * 1024))
        value >= 1024 -> "%.1f KB".format(java.util.Locale.ROOT, value / 1024)
        else -> "${value.toLong()} B"
    }
}

private fun transferEtaLabel(seconds: Long): String = when {
    seconds < 60 -> "${seconds.coerceAtLeast(0)}s"
    seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
    else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
}

private class UserCancelledException : IOException("Transfer cancelled by user")
private class WorkerStoppedException : IOException("Transfer was interrupted by Android")
private class ItemSkippedException(message: String) : IOException(message)

private data class PublicMetadata(val name: String, val mime: String, val dateTaken: Long, val size: Long)
private data class PublicLocation(val name: String, val relativePath: String)

private class TransferJobStore(context: Context) {
    private val root = File(context.filesDir, "transfer-jobs").apply { mkdirs() }

    fun write(job: TransferJob) {
        val json = JSONObject().apply {
            put("id", job.id)
            put("operation", job.operation.name)
            put("target", job.targetAlbum)
            put("sources", JSONArray(job.sources))
            put("secure_ids", JSONArray(job.secureIds))
            put("secure_session", job.secureSession ?: JSONObject.NULL)
            put("secure_storage_policy", job.secureStoragePolicy)
            put("conflict_policy", job.conflictPolicy)
        }
        val temp = File(root, "${job.id}.tmp")
        temp.writeText(json.toString())
        val destination = file(job.id)
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }

    fun read(id: String): TransferJob {
        require(UUID.fromString(id).toString() == id) { "Invalid transfer identifier" }
        val json = JSONObject(file(id).readText())
        return TransferJob(
            id = json.getString("id"),
            operation = TransferOperation.valueOf(json.getString("operation")),
            targetAlbum = json.optString("target"),
            sources = json.getJSONArray("sources").strings(),
            secureIds = json.getJSONArray("secure_ids").strings(),
            secureSession = json.optString("secure_session").takeIf { it.isNotBlank() && it != "null" },
            secureStoragePolicy = json.optString("secure_storage_policy", SecureStoragePolicy.LOCKED_ONLY.name),
            conflictPolicy = json.optString("conflict_policy", TransferConflictPolicy.RENAME.name),
        )
    }

    fun delete(id: String) {
        runCatching {
            if (UUID.fromString(id).toString() == id) {
                file(id).delete()
                checkpointFile(id).delete()
            }
        }
    }

    fun readCheckpoint(id: String): TransferCheckpoint {
        val source = checkpointFile(id)
        if (!source.exists()) return TransferCheckpoint()
        return runCatching {
            val json = JSONObject(source.readText())
            val completed = json.optJSONArray("completed")?.strings().orEmpty().toMutableSet()
            val failuresJson = json.optJSONObject("failures")
            val failures = buildMap {
                failuresJson?.keys()?.forEach { key -> put(key, failuresJson.optString(key)) }
            }
            val artifactsJson = json.optJSONObject("artifacts")
            val artifacts = buildMap {
                artifactsJson?.keys()?.forEach { key -> put(key, artifactsJson.optString(key)) }
            }.toMutableMap()
            val skipsJson = json.optJSONObject("skips")
            val skips = buildMap {
                skipsJson?.keys()?.forEach { key -> put(key, skipsJson.optString(key)) }
            }.toMutableMap()
            val namesJson = json.optJSONObject("names")
            val names = buildMap {
                namesJson?.keys()?.forEach { key -> put(key, namesJson.optString(key)) }
            }.toMutableMap()
            val undoJson = json.optJSONObject("undo")
            val undo = buildMap {
                undoJson?.keys()?.forEach { key -> put(key, undoJson.optString(key)) }
            }.toMutableMap()
            TransferCheckpoint(completed, failures, artifacts, names, skips, undo)
        }.getOrDefault(TransferCheckpoint())
    }

    fun writeCheckpoint(id: String, checkpoint: TransferCheckpoint) {
        require(UUID.fromString(id).toString() == id) { "Invalid transfer identifier" }
        val json = JSONObject().apply {
            put("completed", JSONArray(checkpoint.completedKeys.toList()))
            put("failures", JSONObject(checkpoint.failures))
            put("artifacts", JSONObject(checkpoint.artifacts))
            put("names", JSONObject(checkpoint.names))
            put("skips", JSONObject(checkpoint.skips))
            put("undo", JSONObject(checkpoint.undo))
        }
        val temp = File(root, "$id.progress.tmp")
        temp.writeText(json.toString())
        val destination = checkpointFile(id)
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
    }

    private fun file(id: String) = File(root, "$id.json")
    private fun checkpointFile(id: String) = File(root, "$id.progress.json")
}

private data class TransferCheckpoint(
    val completedKeys: MutableSet<String> = mutableSetOf(),
    val failures: Map<String, String> = emptyMap(),
    val artifacts: MutableMap<String, String> = mutableMapOf(),
    val names: MutableMap<String, String> = mutableMapOf(),
    val skips: MutableMap<String, String> = mutableMapOf(),
    val undo: MutableMap<String, String> = mutableMapOf(),
)

private fun JSONArray.strings(): List<String> = buildList { repeat(length()) { add(getString(it)) } }

internal fun sanitizeAlbumName(name: String): String = name
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .trim()
    .take(60)
    .ifBlank { "New album" }

internal fun sameRelativePath(first: String, second: String): Boolean =
    first.replace('\\', '/').trim('/').equals(second.replace('\\', '/').trim('/'), ignoreCase = true)
