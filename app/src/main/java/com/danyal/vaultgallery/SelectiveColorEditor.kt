package com.danyal.vaultgallery

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import com.danyal.vaultgallery.ui.VaultSecondary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

internal enum class SelectiveHue(val label: String, val center: Float) {
    RED("Red", 0f), ORANGE("Orange", 30f), YELLOW("Yellow", 60f), GREEN("Green", 120f),
    CYAN("Cyan", 180f), BLUE("Blue", 240f), PURPLE("Purple", 275f), MAGENTA("Magenta", 320f),
}

internal fun selectiveHueWeight(hue: Float, center: Float): Float {
    val rawDistance = abs(hue - center)
    val distance = min(rawDistance, 360f - rawDistance)
    return ((55f - distance) / 25f).coerceIn(0f, 1f)
}

internal fun applySelectiveHsl(source: Bitmap, target: SelectiveHue, hueShift: Float, saturation: Float, luminance: Float): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val row = IntArray(source.width)
    val hsv = FloatArray(3)
    repeat(source.height) { y ->
        source.getPixels(row, 0, source.width, 0, y, source.width, 1)
        row.indices.forEach { x ->
            val color = row[x]
            AndroidColor.colorToHSV(color, hsv)
            val weight = selectiveHueWeight(hsv[0], target.center)
            if (weight > 0f) {
                hsv[0] = (hsv[0] + hueShift * weight + 360f) % 360f
                hsv[1] = (hsv[1] * (1f + saturation / 100f * weight)).coerceIn(0f, 1f)
                hsv[2] = (hsv[2] + luminance / 100f * weight).coerceIn(0f, 1f)
                row[x] = AndroidColor.HSVToColor(AndroidColor.alpha(color), hsv)
            }
        }
        output.setPixels(row, 0, source.width, 0, y, source.width, 1)
    }
    return output
}

@Composable
internal fun SelectiveColorEditor(source: Bitmap, onCancel: () -> Unit, onDone: (Bitmap) -> Unit) {
    var target by remember { mutableStateOf(SelectiveHue.RED) }
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(0f) }
    var luminance by remember { mutableFloatStateOf(0f) }
    var applying by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val previewSource = remember(source) {
        val scale = min(1f, 640f / max(source.width, source.height).coerceAtLeast(1))
        if (scale < 1f) Bitmap.createScaledBitmap(source, (source.width * scale).toInt(), (source.height * scale).toInt(), true) else source
    }
    val preview by produceState<Bitmap?>(previewSource, previewSource, target, hue, saturation, luminance) {
        value = withContext(Dispatchers.Default) { applySelectiveHsl(previewSource, target, hue, saturation, luminance) }
    }
    BackHandler(enabled = !applying, onBack = onCancel)
    Scaffold(
        containerColor = Color.Black,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !applying, onClick = onCancel) { Text("Cancel") }
                Text("Selective colour", color = Color.White, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Button(enabled = !applying, onClick = {
                    applying = true
                    scope.launch {
                        val result = withContext(Dispatchers.Default) { applySelectiveHsl(source, target, hue, saturation, luminance) }
                        onDone(result)
                    }
                }) { Text("Apply") }
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().background(Color(0xFF17171A)).navigationBarsPadding().padding(vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                LazyRow(contentPadding = PaddingValues(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    items(SelectiveHue.entries) { channel -> FilterChip(target == channel, { target = channel }, label = { Text(channel.label) }) }
                }
                SelectiveSlider("Hue", hue, -90f..90f) { hue = it }
                SelectiveSlider("Saturation", saturation, -100f..100f) { saturation = it }
                SelectiveSlider("Luminance", luminance, -60f..60f) { luminance = it }
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            preview?.let { Image(it.asImageBitmap(), "Selective colour preview", Modifier.fillMaxSize(), contentScale = ContentScale.Fit) }
            if (applying) Column(Modifier.background(Color(0xCC202024), MaterialTheme.shapes.large).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                CircularProgressIndicator()
                Text("Rendering full resolution…", color = Color.White)
            }
        }
    }
}

@Composable
private fun SelectiveSlider(label: String, value: Float, range: ClosedFloatingPointRange<Float>, onValue: (Float) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = VaultSecondary, modifier = Modifier.weight(.7f))
        Slider(value, onValue, valueRange = range, modifier = Modifier.weight(1.8f))
        Text(value.toInt().toString(), color = Color.White, modifier = Modifier.weight(.35f))
    }
}
