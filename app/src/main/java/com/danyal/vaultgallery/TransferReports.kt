package com.danyal.vaultgallery

import android.content.Context
import android.content.ContentValues
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSurface
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal enum class TransferReportStatus { SUCCESS, FAILED, SKIPPED, CANCELLED }

internal data class TransferReportEntry(
    val name: String,
    val status: TransferReportStatus,
    val detail: String = "",
    val undoUri: String? = null,
    val undoRelativePath: String? = null,
    val undoName: String? = null,
)

internal data class TransferReport(
    val id: String,
    val operation: String,
    val target: String,
    val completedAtMs: Long,
    val summary: String,
    val entries: List<TransferReportEntry>,
)

internal class TransferReportStore(context: Context) {
    private val root = File(context.filesDir, "transfer-reports").apply { mkdirs() }

    fun write(report: TransferReport) {
        val json = JSONObject().apply {
            put("version", 1)
            put("id", report.id)
            put("operation", report.operation)
            put("target", report.target)
            put("completed_at", report.completedAtMs)
            put("summary", report.summary)
            put("entries", JSONArray().apply {
                report.entries.forEach { entry -> put(JSONObject().apply {
                    put("name", entry.name)
                    put("status", entry.status.name)
                    put("detail", entry.detail)
                    entry.undoUri?.let { put("undo_uri", it) }
                    entry.undoRelativePath?.let { put("undo_path", it) }
                    entry.undoName?.let { put("undo_name", it) }
                }) }
            })
        }
        val destination = file(report.id)
        val temp = File(root, "${report.id}.tmp")
        FileOutputStream(temp).use { output ->
            output.write(json.toString().toByteArray(Charsets.UTF_8))
            output.fd.sync()
        }
        if (destination.exists()) destination.delete()
        if (!temp.renameTo(destination)) {
            temp.copyTo(destination, overwrite = true)
            temp.delete()
        }
        root.listFiles().orEmpty().filter { it.extension == "json" }
            .sortedByDescending(File::lastModified).drop(50).forEach(File::delete)
    }

    fun list(): List<TransferReport> = root.listFiles().orEmpty()
        .filter { it.extension == "json" }
        .mapNotNull { source -> runCatching { decode(JSONObject(source.readText())) }.getOrNull() }
        .sortedByDescending(TransferReport::completedAtMs)

    fun clear() {
        root.listFiles().orEmpty().forEach(File::delete)
    }

    private fun file(id: String) = File(root, "$id.json")

    private fun decode(json: JSONObject): TransferReport {
        val entries = buildList {
            val array = json.optJSONArray("entries") ?: JSONArray()
            repeat(array.length()) { index ->
                val entry = array.getJSONObject(index)
                add(TransferReportEntry(
                    name = entry.optString("name", "Media"),
                    status = runCatching { TransferReportStatus.valueOf(entry.optString("status")) }.getOrDefault(TransferReportStatus.FAILED),
                    detail = entry.optString("detail"),
                    undoUri = entry.optString("undo_uri").takeIf(String::isNotBlank),
                    undoRelativePath = entry.optString("undo_path").takeIf(String::isNotBlank),
                    undoName = entry.optString("undo_name").takeIf(String::isNotBlank),
                ))
            }
        }
        return TransferReport(
            id = json.getString("id"),
            operation = json.optString("operation", "Transfer"),
            target = json.optString("target"),
            completedAtMs = json.optLong("completed_at"),
            summary = json.optString("summary"),
            entries = entries,
        )
    }
}

@Composable
internal fun TransferHistoryScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { TransferReportStore(context) }
    var reports by remember { mutableStateOf(store.list()) }
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf<String?>(null) }
    val formatter = remember { SimpleDateFormat("d MMM yyyy, h:mm a", Locale.getDefault()) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(title = "Transfer history", back = onBack)
        if (reports.isEmpty()) {
            Text("No completed transfers yet", color = VaultSecondary, modifier = Modifier.padding(24.dp))
        } else LazyColumn(
            Modifier.fillMaxSize().padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(reports, key = { it.id }) { report ->
                Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(report.operation, style = MaterialTheme.typography.titleMedium)
                            Text(formatter.format(Date(report.completedAtMs)), color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
                        }
                        if (report.target.isNotBlank()) Text("Destination: ${report.target}", color = VaultSecondary)
                        Text(report.summary)
                        report.entries.forEach { entry ->
                            val marker = when (entry.status) {
                                TransferReportStatus.SUCCESS -> "✓"
                                TransferReportStatus.FAILED -> "!"
                                TransferReportStatus.SKIPPED -> "–"
                                TransferReportStatus.CANCELLED -> "×"
                            }
                            Text("$marker  ${entry.name}${entry.detail.takeIf(String::isNotBlank)?.let { " — $it" }.orEmpty()}", color = if (entry.status == TransferReportStatus.SUCCESS) VaultSecondary else MaterialTheme.colorScheme.error)
                        }
                        val recoverable = report.entries.filter { it.status == TransferReportStatus.SUCCESS && it.undoUri != null && it.undoRelativePath != null }
                        if (recoverable.isNotEmpty()) Button(onClick = {
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { undoPublicMoves(context, recoverable) }
                                if (result.restoredUris.isNotEmpty()) {
                                    store.write(report.copy(entries = report.entries.map { entry ->
                                        if (entry.undoUri in result.restoredUris) entry.copy(
                                            detail = "Move undone",
                                            undoUri = null,
                                            undoRelativePath = null,
                                            undoName = null,
                                        ) else entry
                                    }))
                                }
                                status = result.message
                                reports = store.list()
                            }
                        }) { Text("Undo move") }
                    }
                }
            }
        }
        status?.let { message ->
            Text(message, modifier = Modifier.fillMaxWidth().padding(12.dp), color = VaultSecondary)
        }
    }
}

private data class UndoMoveResult(val message: String, val restoredUris: Set<String>)

private fun undoPublicMoves(context: Context, entries: List<TransferReportEntry>): UndoMoveResult {
    var restored = 0
    var failed = 0
    val restoredUris = mutableSetOf<String>()
    entries.forEach { entry ->
        val uri = entry.undoUri?.let(Uri::parse) ?: return@forEach
        val path = entry.undoRelativePath ?: return@forEach
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.RELATIVE_PATH, path)
            entry.undoName?.let { put(MediaStore.MediaColumns.DISPLAY_NAME, it) }
            put(MediaStore.MediaColumns.DATE_MODIFIED, System.currentTimeMillis() / 1000L)
        }
        runCatching { context.contentResolver.update(uri, values, null, null) }
            .onSuccess {
                if (it > 0) {
                    restored++
                    entry.undoUri?.let(restoredUris::add)
                } else failed++
            }
            .onFailure { failed++ }
    }
    val message = when {
        restored > 0 && failed == 0 -> "Restored $restored item${if (restored == 1) "" else "s"} to the original folder."
        restored > 0 -> "Restored $restored item${if (restored == 1) "" else "s"}; $failed could not be restored."
        else -> "The move could not be undone. The item may have changed or Android may require media permission."
    }
    return UndoMoveResult(message, restoredUris)
}
