package com.danyal.vaultgallery

import android.content.ContentValues
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.editor.SubtitleCue
import com.danyal.vaultgallery.editor.parseSrt
import com.danyal.vaultgallery.editor.serializeSrt
import com.danyal.vaultgallery.ui.VaultSecondary

@Composable
internal fun SubtitleEditorPanel(
    cues: List<SubtitleCue>,
    positionMs: Long,
    durationMs: Long,
    onChange: (List<SubtitleCue>) -> Unit,
) {
    val context = LocalContext.current
    var editing by remember { mutableStateOf<SubtitleCue?>(null) }
    var creating by remember { mutableStateOf(false) }
    val importer = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { parseSrt(it.readText()) }
                ?: error("The subtitle file could not be read")
        }.onSuccess { imported ->
            onChange(imported)
            Toast.makeText(context, "Imported ${imported.size} subtitle cues", Toast.LENGTH_SHORT).show()
        }.onFailure { Toast.makeText(context, it.message ?: "Subtitle import failed", Toast.LENGTH_LONG).show() }
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { importer.launch(arrayOf("application/x-subrip", "text/plain", "text/*")) }) { Text("Import SRT") }
            OutlinedButton(onClick = { creating = true }) { Icon(Icons.Outlined.Add, null); Text("Add at playhead") }
            OutlinedButton(enabled = cues.isNotEmpty(), onClick = {
                runCatching { exportSubtitles(context, cues) }
                    .onSuccess { Toast.makeText(context, "SRT saved to Downloads/Vault Gallery Captions", Toast.LENGTH_SHORT).show() }
                    .onFailure { Toast.makeText(context, it.message ?: "Subtitle export failed", Toast.LENGTH_LONG).show() }
            }) { Text("Export SRT") }
        }
        Column(Modifier.fillMaxWidth().heightIn(max = 130.dp).verticalScroll(rememberScrollState())) {
            if (cues.isEmpty()) Text("No subtitles. Import an SRT file or add a timed caption.", color = VaultSecondary)
            cues.sortedBy(SubtitleCue::startMs).forEach { cue ->
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("${samsungVideoTimeLabel(cue.startMs)}–${samsungVideoTimeLabel(cue.endMs)}  ${cue.text.replace('\n', ' ')}", modifier = Modifier.weight(1f), maxLines = 1)
                    IconButton(onClick = { editing = cue }) { Icon(Icons.Outlined.Edit, "Edit subtitle") }
                    IconButton(onClick = { onChange(cues.filterNot { it.id == cue.id }) }) { Icon(Icons.Outlined.Delete, "Delete subtitle") }
                }
            }
        }
    }

    if (creating || editing != null) {
        val initialStart = positionMs.coerceIn(0L, (durationMs - 250L).coerceAtLeast(0L))
        val initial = editing ?: SubtitleCue(
            startMs = initialStart,
            endMs = (initialStart + 2_500L).coerceAtMost(durationMs).coerceAtLeast(initialStart + 1L),
            text = "Caption",
        )
        SubtitleCueDialog(initial, onDismiss = { creating = false; editing = null }) { changed ->
            onChange((cues.filterNot { it.id == changed.id } + changed).sortedBy(SubtitleCue::startMs))
            creating = false
            editing = null
        }
    }
}

@Composable
private fun SubtitleCueDialog(cue: SubtitleCue, onDismiss: () -> Unit, onSave: (SubtitleCue) -> Unit) {
    var text by remember(cue.id) { mutableStateOf(cue.text) }
    var start by remember(cue.id) { mutableStateOf("%.3f".format(cue.startMs / 1_000.0)) }
    var end by remember(cue.id) { mutableStateOf("%.3f".format(cue.endMs / 1_000.0)) }
    val startMs = (start.toDoubleOrNull()?.times(1_000))?.toLong()
    val endMs = (end.toDoubleOrNull()?.times(1_000))?.toLong()
    val valid = text.isNotBlank() && startMs != null && endMs != null && startMs >= 0 && endMs > startMs
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timed subtitle") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(text, { text = it.take(500) }, label = { Text("Text") }, modifier = Modifier.fillMaxWidth())
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(start, { start = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("Start seconds") }, modifier = Modifier.weight(1f))
                    OutlinedTextField(end, { end = it.filter { ch -> ch.isDigit() || ch == '.' } }, label = { Text("End seconds") }, modifier = Modifier.weight(1f))
                }
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { TextButton(enabled = valid, onClick = { onSave(cue.copy(startMs = startMs!!, endMs = endMs!!, text = text.trim())) }) { Text("Save") } },
    )
}

private fun exportSubtitles(context: android.content.Context, cues: List<SubtitleCue>) {
    val values = ContentValues().apply {
        put(MediaStore.Downloads.DISPLAY_NAME, "VaultGallery_${System.currentTimeMillis()}.srt")
        put(MediaStore.Downloads.MIME_TYPE, "application/x-subrip")
        put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/Vault Gallery Captions")
        put(MediaStore.Downloads.IS_PENDING, 1)
    }
    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        ?: error("Could not create the subtitle file")
    try {
        context.contentResolver.openOutputStream(uri, "w")?.bufferedWriter()?.use { it.write(serializeSrt(cues)) }
            ?: error("Could not write the subtitle file")
        context.contentResolver.update(uri, ContentValues().apply { put(MediaStore.Downloads.IS_PENDING, 0) }, null, null)
    } catch (failure: Throwable) {
        context.contentResolver.delete(uri, null, null)
        throw failure
    }
}
