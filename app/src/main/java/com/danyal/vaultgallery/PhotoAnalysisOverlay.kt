package com.danyal.vaultgallery

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

internal data class PhotoHistogram(
    val red: IntArray,
    val green: IntArray,
    val blue: IntArray,
    val luminance: IntArray,
)

internal fun calculatePhotoHistogram(bitmap: Bitmap, maximumDimension: Int = 640): PhotoHistogram {
    val scale = minOf(1f, maximumDimension.toFloat() / max(bitmap.width, bitmap.height).coerceAtLeast(1))
    val sampled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), false) else bitmap
    val pixels = IntArray(sampled.width * sampled.height)
    sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)
    val red = IntArray(256); val green = IntArray(256); val blue = IntArray(256); val luminance = IntArray(256)
    pixels.forEach { pixel ->
        val r = pixel shr 16 and 0xFF
        val g = pixel shr 8 and 0xFF
        val b = pixel and 0xFF
        red[r]++; green[g]++; blue[b]++
        luminance[(.2126f * r + .7152f * g + .0722f * b).toInt().coerceIn(0, 255)]++
    }
    if (sampled !== bitmap) sampled.recycle()
    return PhotoHistogram(red, green, blue, luminance)
}

internal fun createClippingWarning(bitmap: Bitmap, maximumDimension: Int = 960): Bitmap {
    val scale = minOf(1f, maximumDimension.toFloat() / max(bitmap.width, bitmap.height).coerceAtLeast(1))
    val sampled = if (scale < 1f) Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt().coerceAtLeast(1), (bitmap.height * scale).toInt().coerceAtLeast(1), false) else bitmap
    val pixels = IntArray(sampled.width * sampled.height)
    sampled.getPixels(pixels, 0, sampled.width, 0, 0, sampled.width, sampled.height)
    pixels.indices.forEach { index ->
        val pixel = pixels[index]
        val r = pixel shr 16 and 0xFF; val g = pixel shr 8 and 0xFF; val b = pixel and 0xFF
        pixels[index] = when {
            maxOf(r, g, b) >= 250 -> 0xD9FF2D55.toInt()
            minOf(r, g, b) <= 5 -> 0xD9307CFF.toInt()
            else -> 0x00000000
        }
    }
    val output = Bitmap.createBitmap(pixels, sampled.width, sampled.height, Bitmap.Config.ARGB_8888)
    if (sampled !== bitmap) sampled.recycle()
    return output
}

@Composable
internal fun PhotoHistogramOverlay(bitmap: Bitmap, modifier: Modifier = Modifier) {
    val histogram by produceState<PhotoHistogram?>(null, bitmap) {
        value = withContext(Dispatchers.Default) { calculatePhotoHistogram(bitmap) }
    }
    val data = histogram ?: return
    Box(modifier.width(210.dp).height(112.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xCC17171A)).padding(10.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val maximum = data.luminance.maxOrNull()?.coerceAtLeast(1)?.toFloat() ?: 1f
            fun draw(values: IntArray, color: Color, alpha: Float) {
                val step = size.width / 255f
                for (index in 1 until values.size) {
                    drawLine(
                        color.copy(alpha = alpha),
                        start = androidx.compose.ui.geometry.Offset((index - 1) * step, size.height - values[index - 1] / maximum * size.height),
                        end = androidx.compose.ui.geometry.Offset(index * step, size.height - values[index] / maximum * size.height),
                        strokeWidth = 1.5f,
                    )
                }
            }
            draw(data.luminance, Color.White, .65f)
            draw(data.red, Color.Red, .78f)
            draw(data.green, Color.Green, .72f)
            draw(data.blue, Color(0xFF4A86F7), .82f)
        }
    }
}

@Composable
internal fun PhotoClippingOverlay(bitmap: Bitmap, modifier: Modifier = Modifier) {
    val warning by produceState<Bitmap?>(null, bitmap) {
        value = withContext(Dispatchers.Default) { createClippingWarning(bitmap) }
    }
    warning?.let { overlay ->
        Image(overlay.asImageBitmap(), "Red marks clipped highlights; blue marks clipped shadows", modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
