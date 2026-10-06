package com.danyal.vaultgallery

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
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
import com.danyal.vaultgallery.data.MediaKind
import com.danyal.vaultgallery.editor.PhotoOperation
import com.danyal.vaultgallery.editor.PhotoPreset
import java.io.File
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import kotlin.coroutines.coroutineContext

internal data class BatchPresetJob(val id: String, val sources: List<GalleryMedia>, val preset: PhotoPreset)

internal object BatchPresetCoordinator {
    const val WORK_TAG = "vault-gallery-batch-preset"

    fun enqueue(context: Context, sources: List<GalleryMedia>, preset: PhotoPreset): UUID {
        require(sources.isNotEmpty())
        val job = BatchPresetJob(UUID.randomUUID().toString(), sources.filter { it.kind == MediaKind.IMAGE }, preset)
        BatchPresetJobStore(context).write(job)
        val request = OneTimeWorkRequestBuilder<BatchPresetWorker>()
            .setInputData(Data.Builder().putString(BatchPresetWorker.KEY_JOB_ID, job.id).build())
            .setConstraints(Constraints.Builder().setRequiresStorageNotLow(true).build())
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .addTag(WORK_TAG)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork("batch-preset-${job.id}", ExistingWorkPolicy.KEEP, request)
        return request.id
    }
}

internal class BatchPresetWorker(context: Context, parameters: WorkerParameters) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString(KEY_JOB_ID) ?: return@withContext Result.failure()
        val store = BatchPresetJobStore(applicationContext)
        val job = store.read(id) ?: return@withContext Result.failure()
        ensureChannel()
        setForeground(foreground("Preparing ${job.preset.name}", 0, job.sources.size))
        var completed = 0
        val failures = ArrayList<String>()
        try {
            job.sources.forEachIndexed { index, source ->
                coroutineContext.ensureActive()
                setProgress(Data.Builder().putInt("completed", completed).putInt("total", job.sources.size).build())
                setForeground(foreground("${job.preset.name} • ${source.name}", index, job.sources.size))
                runCatching {
                    val decoded = decodeBitmapForEditing(applicationContext, source.uri, maxDimension = 8192)
                        ?: error("Could not decode ${source.name}")
                    val rendered = try { renderPhotoAdjustment(decoded, job.preset.adjustment) } finally { decoded.recycle() }
                    try { saveEditedBitmap(applicationContext, rendered, source) } finally { rendered.recycle() }
                }.onFailure { failures += "${source.name}: ${it.message ?: "failed"}" }
                completed++
            }
            val output = Data.Builder().putInt("completed", completed).putInt("total", job.sources.size)
                .putInt("failed", failures.size).putString("errors", failures.take(8).joinToString("\n")).build()
            if (failures.size == job.sources.size) Result.failure(output) else Result.success(output)
        } finally {
            store.delete(id)
        }
    }

    private fun foreground(text: String, completed: Int, total: Int): ForegroundInfo {
        val notificationId = id.hashCode()
        val launch = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            Intent(applicationContext, MainGalleryActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancel = WorkManager.getInstance(applicationContext).createCancelPendingIntent(id)
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Applying photo preset")
            .setContentText(text)
            .setContentIntent(launch)
            .setOnlyAlertOnce(true)
            .setOngoing(completed < total)
            .setProgress(total.coerceAtLeast(1), completed.coerceIn(0, total.coerceAtLeast(1)), total <= 0)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Cancel", cancel)
            .build()
        return ForegroundInfo(notificationId, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
    }

    private fun ensureChannel() {
        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL_ID, "Photo batch edits", NotificationManager.IMPORTANCE_LOW))
    }

    companion object {
        const val KEY_JOB_ID = "job_id"
        private const val CHANNEL_ID = "photo-batch-edits"
    }
}

private class BatchPresetJobStore(private val context: Context) {
    private val root = File(context.noBackupFilesDir, "batch-preset-jobs").apply { mkdirs() }

    fun write(job: BatchPresetJob) {
        val destination = file(job.id)
        val temporary = File(root, ".${job.id}.tmp")
        temporary.writeText(JSONObject().apply {
            put("id", job.id)
            put("preset", job.preset.toJson())
            put("sources", JSONArray().apply { job.sources.forEach { put(it.toJson()) } })
        }.toString())
        if (destination.exists()) destination.delete()
        require(temporary.renameTo(destination)) { "Could not queue batch preset" }
    }

    fun read(id: String): BatchPresetJob? = runCatching {
        val root = JSONObject(file(id).readText())
        val sources = root.getJSONArray("sources").let { array ->
            List(array.length()) { index -> array.getJSONObject(index).toGalleryMedia() }
        }
        BatchPresetJob(root.getString("id"), sources, root.getJSONObject("preset").toPhotoPreset())
    }.getOrNull()

    fun delete(id: String) { file(id).delete() }
    private fun file(id: String) = File(root, "$id.json")
}

private fun GalleryMedia.toJson() = JSONObject().apply {
    put("id", id); put("uri", uri.toString()); put("name", name); put("mime", mimeType)
    put("width", width); put("height", height); put("size", sizeBytes); put("date", dateTakenMs)
    put("bucketId", bucketId); put("bucket", bucketName); put("relative", relativePath)
}

private fun JSONObject.toGalleryMedia() = GalleryMedia(
    id = getLong("id"), uri = Uri.parse(getString("uri")), name = getString("name"), mimeType = getString("mime"),
    kind = MediaKind.IMAGE, width = getInt("width"), height = getInt("height"), durationMs = 0,
    sizeBytes = getLong("size"), dateTakenMs = getLong("date"), bucketId = getLong("bucketId"),
    bucketName = getString("bucket"), isFavourite = false, relativePath = optString("relative"),
)

private fun PhotoPreset.toJson() = JSONObject().apply {
    put("id", id); put("name", name); put("created", createdAtMs); put("updated", updatedAtMs)
    put("adjustment", adjustment.toJson())
}

private fun JSONObject.toPhotoPreset() = PhotoPreset(
    id = getString("id"), name = getString("name"), adjustment = getJSONObject("adjustment").toAdjustment(),
    createdAtMs = getLong("created"), updatedAtMs = getLong("updated"),
)

private fun PhotoOperation.Adjustment.toJson() = JSONObject().apply {
    put("filter", filter); put("light", lightBalance); put("brightness", brightness); put("exposure", exposure)
    put("contrast", contrast); put("highlights", highlights); put("shadows", shadows); put("black", blackPoint)
    put("white", whitePoint); put("saturation", saturation); put("tint", tint); put("temperature", temperature)
    put("red", redGain); put("green", greenGain); put("blue", blueGain)
}

private fun JSONObject.toAdjustment() = PhotoOperation.Adjustment(
    filter = getString("filter"), lightBalance = getDouble("light").toFloat(), brightness = getDouble("brightness").toFloat(),
    exposure = getDouble("exposure").toFloat(), contrast = getDouble("contrast").toFloat(),
    highlights = getDouble("highlights").toFloat(), shadows = getDouble("shadows").toFloat(),
    blackPoint = getDouble("black").toFloat(), whitePoint = getDouble("white").toFloat(),
    saturation = getDouble("saturation").toFloat(), tint = getDouble("tint").toFloat(),
    temperature = getDouble("temperature").toFloat(), redGain = getDouble("red").toFloat(),
    greenGain = getDouble("green").toFloat(), blueGain = getDouble("blue").toFloat(),
)
