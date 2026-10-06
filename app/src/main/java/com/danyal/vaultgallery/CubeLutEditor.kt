package com.danyal.vaultgallery

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.Reader
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

internal data class CubeLut(
    val title: String,
    val size: Int,
    val values: FloatArray,
    val domainMin: FloatArray = floatArrayOf(0f, 0f, 0f),
    val domainMax: FloatArray = floatArrayOf(1f, 1f, 1f),
)

internal fun parseCubeLut(reader: Reader): CubeLut {
    var title = "Imported LUT"
    var size = 0
    var domainMin = floatArrayOf(0f, 0f, 0f)
    var domainMax = floatArrayOf(1f, 1f, 1f)
    val samples = ArrayList<Float>()
    reader.buffered().useLines { lines ->
        lines.forEach { raw ->
            val line = raw.substringBefore('#').trim()
            if (line.isBlank()) return@forEach
            val parts = line.split(Regex("\\s+"))
            when (parts.first().uppercase()) {
                "TITLE" -> title = line.substringAfter(' ', title).trim().trim('"').ifBlank { title }
                "LUT_3D_SIZE" -> size = parts.getOrNull(1)?.toIntOrNull() ?: error("Invalid LUT_3D_SIZE")
                "DOMAIN_MIN" -> domainMin = parts.drop(1).take(3).map(String::toFloat).toFloatArray()
                "DOMAIN_MAX" -> domainMax = parts.drop(1).take(3).map(String::toFloat).toFloatArray()
                "LUT_1D_SIZE" -> error("1D LUTs are not supported; choose a 3D .cube LUT")
                else -> if (parts.size >= 3 && parts.take(3).all { it.toFloatOrNull() != null }) {
                    parts.take(3).forEach { samples += it.toFloat().coerceIn(0f, 1f) }
                }
            }
        }
    }
    require(size in 2..64) { "3D LUT size must be between 2 and 64" }
    require(domainMin.size == 3 && domainMax.size == 3) { "Invalid LUT domain" }
    require(samples.size == size * size * size * 3) { "Expected ${size * size * size} LUT samples, found ${samples.size / 3}" }
    return CubeLut(title, size, samples.toFloatArray(), domainMin, domainMax)
}

internal fun applyCubeLut(source: Bitmap, lut: CubeLut, strength: Float): Bitmap {
    val amount = strength.coerceIn(0f, 1f)
    if (amount <= 0f) return source.copy(Bitmap.Config.ARGB_8888, false)
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val row = IntArray(source.width)
    repeat(source.height) { y ->
        source.getPixels(row, 0, source.width, 0, y, source.width, 1)
        row.indices.forEach { x ->
            val color = row[x]
            val original = floatArrayOf(AndroidColor.red(color) / 255f, AndroidColor.green(color) / 255f, AndroidColor.blue(color) / 255f)
            val mapped = sampleCubeLut(lut, original[0], original[1], original[2])
            val r = (original[0] + (mapped[0] - original[0]) * amount).coerceIn(0f, 1f)
            val g = (original[1] + (mapped[1] - original[1]) * amount).coerceIn(0f, 1f)
            val b = (original[2] + (mapped[2] - original[2]) * amount).coerceIn(0f, 1f)
            row[x] = AndroidColor.argb(AndroidColor.alpha(color), (r * 255).roundToInt(), (g * 255).roundToInt(), (b * 255).roundToInt())
        }
        output.setPixels(row, 0, source.width, 0, y, source.width, 1)
    }
    return output
}

internal fun sampleCubeLut(lut: CubeLut, red: Float, green: Float, blue: Float): FloatArray {
    fun normalized(value: Float, channel: Int): Float {
        val minValue = lut.domainMin[channel]
        val range = (lut.domainMax[channel] - minValue).takeIf { it > 0f } ?: 1f
        return ((value - minValue) / range).coerceIn(0f, 1f) * (lut.size - 1)
    }
    val coordinates = floatArrayOf(normalized(red, 0), normalized(green, 1), normalized(blue, 2))
    val low = IntArray(3) { floor(coordinates[it]).toInt().coerceIn(0, lut.size - 1) }
    val high = IntArray(3) { (low[it] + 1).coerceAtMost(lut.size - 1) }
    val fraction = FloatArray(3) { coordinates[it] - low[it] }
    fun sample(r: Int, g: Int, b: Int, c: Int): Float = lut.values[((b * lut.size * lut.size + g * lut.size + r) * 3) + c]
    return FloatArray(3) { channel ->
        val c00 = sample(low[0], low[1], low[2], channel) * (1 - fraction[0]) + sample(high[0], low[1], low[2], channel) * fraction[0]
        val c10 = sample(low[0], high[1], low[2], channel) * (1 - fraction[0]) + sample(high[0], high[1], low[2], channel) * fraction[0]
        val c01 = sample(low[0], low[1], high[2], channel) * (1 - fraction[0]) + sample(high[0], low[1], high[2], channel) * fraction[0]
        val c11 = sample(low[0], high[1], high[2], channel) * (1 - fraction[0]) + sample(high[0], high[1], high[2], channel) * fraction[0]
        val c0 = c00 * (1 - fraction[1]) + c10 * fraction[1]
        val c1 = c01 * (1 - fraction[1]) + c11 * fraction[1]
        c0 * (1 - fraction[2]) + c1 * fraction[2]
    }
}

@Composable
internal fun CubeLutEditor(source: Bitmap, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var lut by remember { mutableStateOf<CubeLut?>(null) }
    var strength by remember { mutableFloatStateOf(1f) }
    var error by remember { mutableStateOf<String?>(null) }
    var applying by remember { mutableStateOf(false) }
    val previewSource = remember(source) {
        val scale = min(1f, 640f / max(source.width, source.height).coerceAtLeast(1))
        if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true) else source
    }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                withContext(Dispatchers.IO) { context.contentResolver.openInputStream(uri)!!.reader().use(::parseCubeLut) }
            }.onSuccess { lut = it; error = null }.onFailure { error = it.message ?: "Could not read LUT" }
        }
    }
    val previewEffect by produceState<Bitmap?>(null, previewSource, lut) {
        // Compute the selected LUT once. Strength is a live GPU alpha composite below rather
        // than a fresh per-pixel CPU pass for every slider movement.
        value = lut?.let { withContext(Dispatchers.Default) { applyCubeLut(previewSource, it, 1f) } }
    }
    BackHandler(enabled = !applying, onBack = onCancel)
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !applying, onClick = onCancel) { Text("Cancel") }
                Column(Modifier.weight(1f)) {
                    Text("3D LUT", color = Color.White, style = MaterialTheme.typography.titleLarge)
                    Text(lut?.title ?: "Import a standards-based .cube look", color = VaultSecondary, style = MaterialTheme.typography.labelSmall)
                }
                Button(enabled = lut != null && !applying, onClick = {
                    val active = lut ?: return@Button
                    applying = true
                    scope.launch { onDone(withContext(Dispatchers.Default) { applyCubeLut(source, active, strength) }) }
                }) { Text("Apply") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Color(0xFF17171A)).navigationBarsPadding().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { picker.launch(arrayOf("text/plain", "application/octet-stream", "application/x-cube")) }, modifier = Modifier.fillMaxWidth()) { Text(if (lut == null) "Choose .cube LUT" else "Choose another LUT") }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Strength", color = VaultSecondary)
                    Slider(strength, { strength = it }, modifier = Modifier.weight(1f).padding(horizontal = 10.dp))
                    Text("${(strength * 100).roundToInt()}%", color = Color.White)
                }
                error?.let { Text(it, color = Color(0xFFFF6B6B), style = MaterialTheme.typography.bodySmall) }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Image(previewSource.asImageBitmap(), "LUT source preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            previewEffect?.let {
                Image(it.asImageBitmap(), "LUT preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit, alpha = strength)
            }
            if (applying) Column(Modifier.background(Color(0xCC202024), MaterialTheme.shapes.large).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Rendering full resolution…", color = Color.White)
            }
        }
    }
}
