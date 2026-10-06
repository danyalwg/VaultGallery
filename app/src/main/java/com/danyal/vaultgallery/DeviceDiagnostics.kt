package com.danyal.vaultgallery

import android.app.ActivityManager
import android.content.Context
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.security.SecureVault
import com.danyal.vaultgallery.ui.VaultSecondary
import com.danyal.vaultgallery.ui.VaultSurface
import java.io.File
import java.util.Locale

internal data class DeviceMediaCapabilities(
    val decoders: Set<String>,
    val encoders: Set<String>,
    val hardwareDecoders: Set<String>,
    val hardwareEncoders: Set<String>,
    val hdrDecoderTypes: Set<String>,
    val totalStorageBytes: Long,
    val freeStorageBytes: Long,
    val memoryClassMb: Int,
    val largeMemoryClassMb: Int,
    val glEsVersion: String,
    val aiBackends: List<String>,
    val modelPacks: List<Pair<String, Long>>,
    val persistentVaultAvailable: Boolean,
)

internal fun detectDeviceMediaCapabilities(context: Context): DeviceMediaCapabilities {
    val regularDecoders = linkedSetOf<String>()
    val regularEncoders = linkedSetOf<String>()
    val hardwareDecoders = linkedSetOf<String>()
    val hardwareEncoders = linkedSetOf<String>()
    val hdrTypes = linkedSetOf<String>()
    runCatching {
        MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.forEach { codec ->
            codec.supportedTypes.forEach { mime ->
                if (codec.isEncoder) regularEncoders += mime.lowercase(Locale.ROOT) else regularDecoders += mime.lowercase(Locale.ROOT)
                if (Build.VERSION.SDK_INT >= 29 && codec.isHardwareAccelerated) {
                    if (codec.isEncoder) hardwareEncoders += mime.lowercase(Locale.ROOT) else hardwareDecoders += mime.lowercase(Locale.ROOT)
                }
                if (!codec.isEncoder && Build.VERSION.SDK_INT >= 24) runCatching {
                    codec.getCapabilitiesForType(mime).profileLevels.forEach { profile ->
                        val label = when (profile.profile) {
                            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10 -> "HDR10"
                            MediaCodecInfo.CodecProfileLevel.HEVCProfileMain10HDR10Plus -> "HDR10+"
                            else -> null
                        }
                        label?.let(hdrTypes::add)
                    }
                }
            }
        }
    }
    val storage = StatFs(Environment.getExternalStorageDirectory().absolutePath)
    val activityManager = context.getSystemService(ActivityManager::class.java)
    val modelRoot = File(context.filesDir, "ai-models")
    val modelPacks = modelRoot.listFiles().orEmpty().filter(File::isFile).map { it.name to it.length() }.sortedBy { it.first }
    return DeviceMediaCapabilities(
        decoders = regularDecoders,
        encoders = regularEncoders,
        hardwareDecoders = hardwareDecoders,
        hardwareEncoders = hardwareEncoders,
        hdrDecoderTypes = hdrTypes,
        totalStorageBytes = storage.totalBytes,
        freeStorageBytes = storage.availableBytes,
        memoryClassMb = activityManager.memoryClass,
        largeMemoryClassMb = activityManager.largeMemoryClass,
        glEsVersion = activityManager.deviceConfigurationInfo.glEsVersion,
        aiBackends = listOf("ML Kit OCR", "ML Kit person segmentation", "MediaPipe Interactive Segmenter", "OpenCV CPU"),
        modelPacks = modelPacks,
        persistentVaultAvailable = SecureVault.hasPersistentStorageAccess(context),
    )
}

internal fun DeviceMediaCapabilities.canDecode(mime: String): Boolean = mime.lowercase(Locale.ROOT) in decoders
internal fun DeviceMediaCapabilities.canEncode(mime: String): Boolean = mime.lowercase(Locale.ROOT) in encoders

/** Returns a user-facing reason before an edit allocates an output file or starts Transformer. */
internal fun DeviceMediaCapabilities.videoExportIssue(sourceMime: String?): String? {
    val normalizedSource = sourceMime?.substringBefore(';')?.trim()?.lowercase(Locale.ROOT)
    // video/mp4 and video/webm are container MIME types. Android decoders advertise elementary
    // stream MIME types (video/avc, video/hevc, video/av01, video/x-vnd.on2.vp9), so rejecting the
    // container itself produces a false "no decoder" result on fully capable devices.
    val isContainer = normalizedSource in setOf("video/mp4", "video/webm", "video/quicktime", "video/x-matroska")
    if (!normalizedSource.isNullOrBlank() && normalizedSource.startsWith("video/") && !isContainer && !canDecode(normalizedSource)) {
        return "This device has no decoder for $normalizedSource. The original video is unchanged."
    }
    if (!canEncode("video/avc")) {
        return "This device has no H.264 encoder, so it cannot safely export this edit. The original video is unchanged."
    }
    if (freeStorageBytes < 256L * 1024 * 1024) {
        return "At least 256 MB of free storage is required to start a video export."
    }
    return null
}

@Composable
internal fun DeviceDiagnosticsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var refreshVersion by remember { mutableIntStateOf(0) }
    val capabilities = remember(refreshVersion) { detectDeviceMediaCapabilities(context) }
    Column(Modifier.fillMaxSize()) {
        GalleryToolbar(title = "Diagnostics", back = onBack)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            DiagnosticCard("Device") {
                DiagnosticRow("Android", "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                DiagnosticRow("Device", "${Build.MANUFACTURER} ${Build.MODEL}")
                DiagnosticRow("OpenGL ES", capabilities.glEsVersion)
                DiagnosticRow("Memory class", "${capabilities.memoryClassMb} MB • large ${capabilities.largeMemoryClassMb} MB")
            }
            DiagnosticCard("Storage") {
                DiagnosticRow("Available", "${diagnosticBytes(capabilities.freeStorageBytes)} of ${diagnosticBytes(capabilities.totalStorageBytes)}")
                DiagnosticRow("Persistent Secure Gallery", if (capabilities.persistentVaultAvailable) "Available" else "Permission required")
            }
            DiagnosticCard("Playback and export") {
                listOf("video/avc" to "H.264", "video/hevc" to "HEVC", "video/av01" to "AV1", "video/x-vnd.on2.vp9" to "VP9").forEach { (mime, label) ->
                    DiagnosticRow(
                        label,
                        "Decode ${if (capabilities.canDecode(mime)) "yes" else "no"} • Encode ${if (capabilities.canEncode(mime)) "yes" else "no"}" +
                            if (mime in capabilities.hardwareDecoders) " • hardware" else "",
                    )
                }
                DiagnosticRow("HDR decoder profiles", capabilities.hdrDecoderTypes.ifEmpty { setOf("Not reported") }.joinToString())
                Text("Unsupported formats are hidden or rejected before an export starts; the source is never modified.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
            DiagnosticCard("On-device intelligence") {
                capabilities.aiBackends.forEach { DiagnosticRow(it, "Available") }
                DiagnosticRow("Downloaded model packs", if (capabilities.modelPacks.isEmpty()) "None" else capabilities.modelPacks.joinToString { "${it.first} (${diagnosticBytes(it.second)})" })
                Text("This build does not claim an AI model when it only uses a deterministic image filter.", color = VaultSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { refreshVersion++ }, modifier = Modifier.fillMaxWidth()) { Text("Run checks again") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DiagnosticCard(title: String, content: @Composable () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = VaultSurface), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun DiagnosticRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f))
        Text(value, color = VaultSecondary, modifier = Modifier.weight(1.25f))
    }
}

private fun diagnosticBytes(bytes: Long): String = when {
    bytes >= 1024L * 1024 * 1024 -> "%.1f GB".format(Locale.ROOT, bytes.toDouble() / (1024L * 1024 * 1024))
    bytes >= 1024L * 1024 -> "%.1f MB".format(Locale.ROOT, bytes.toDouble() / (1024L * 1024))
    else -> "%.1f KB".format(Locale.ROOT, bytes.toDouble() / 1024L)
}
