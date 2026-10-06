package com.danyal.vaultgallery

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import com.danyal.vaultgallery.ai.LamaInpaintingEngine
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.SegmentationMask
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.pow
import kotlin.math.sqrt
import kotlinx.coroutines.suspendCancellableCoroutine
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.MatOfPoint2f
import org.opencv.core.Point
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.opencv.photo.Photo

internal suspend fun straightenPhoto(source: Bitmap, degrees: Float): Bitmap = openCvBitmap(source) { rgba ->
    val rotation = Imgproc.getRotationMatrix2D(Point(rgba.cols() / 2.0, rgba.rows() / 2.0), degrees.toDouble(), 1.0)
    val output = Mat()
    Imgproc.warpAffine(rgba, output, rotation, rgba.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE)
    rotation.release()
    output
}

/** Detects the dominant near-horizontal structural angle and levels the photo conservatively. */
internal suspend fun autoStraightenPhoto(source: Bitmap): Bitmap {
    val angle = detectHorizonAngle(source)
    return if (kotlin.math.abs(angle) < .15f) source.copy(Bitmap.Config.ARGB_8888, true)
    else straightenPhoto(source, -angle)
}

internal suspend fun detectHorizonAngle(source: Bitmap): Float = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Default) {
    check(OpenCVLoader.initLocal()) { "OpenCV is unavailable on this device" }
    val scaled = if (maxOf(source.width, source.height) > 1600) {
        val factor = 1600f / maxOf(source.width, source.height)
        Bitmap.createScaledBitmap(source, (source.width * factor).toInt().coerceAtLeast(1), (source.height * factor).toInt().coerceAtLeast(1), true)
    } else source
    val rgba = Mat(); val gray = Mat(); val edges = Mat(); val lines = Mat()
    try {
        Utils.bitmapToMat(scaled, rgba)
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(gray, edges, 60.0, 180.0)
        val minimum = minOf(scaled.width, scaled.height) * .18
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180.0, 70, minimum, 24.0)
        val candidates = ArrayList<Pair<Float, Float>>()
        for (row in 0 until lines.rows()) {
            val line = lines.get(row, 0) ?: continue
            val dx = line[2] - line[0]; val dy = line[3] - line[1]
            val angle = Math.toDegrees(kotlin.math.atan2(dy, dx)).toFloat().let {
                when {
                    it > 90f -> it - 180f
                    it < -90f -> it + 180f
                    else -> it
                }
            }
            if (kotlin.math.abs(angle) <= 25f) {
                val length = kotlin.math.sqrt((dx * dx + dy * dy)).toFloat()
                candidates += angle to length
            }
        }
        if (candidates.isEmpty()) 0f else {
            val ordered = candidates.sortedBy { it.first }
            val half = ordered.sumOf { it.second.toDouble() } / 2.0
            var weight = 0.0
            ordered.firstOrNull { weight += it.second; weight >= half }?.first ?: 0f
        }
    } finally {
        rgba.release(); gray.release(); edges.release(); lines.release()
        if (scaled !== source && !scaled.isRecycled) scaled.recycle()
    }
}

internal suspend fun perspectivePhoto(source: Bitmap, horizontal: Float = 0f, vertical: Float = 0f): Bitmap = openCvBitmap(source) { rgba ->
    val width = (rgba.cols() - 1).toDouble().coerceAtLeast(1.0)
    val height = (rgba.rows() - 1).toDouble().coerceAtLeast(1.0)
    val h = horizontal.coerceIn(-.42f, .42f).toDouble()
    val v = vertical.coerceIn(-.42f, .42f).toDouble()
    // Horizontal keystone correction moves the left/right edges vertically, while
    // vertical keystone correction moves the top/bottom edges horizontally.  These
    // used to be derived from the opposite slider, making either single-axis edit a
    // mathematical no-op.
    val horizontalEdgeInset = kotlin.math.abs(h) * height * .34
    val verticalEdgeInset = kotlin.math.abs(v) * width * .34
    val destination = when {
        kotlin.math.abs(h) >= kotlin.math.abs(v) && h > 0 -> arrayOf(Point(0.0, horizontalEdgeInset), Point(width, 0.0), Point(width, height), Point(0.0, height - horizontalEdgeInset))
        kotlin.math.abs(h) >= kotlin.math.abs(v) && h < 0 -> arrayOf(Point(0.0, 0.0), Point(width, horizontalEdgeInset), Point(width, height - horizontalEdgeInset), Point(0.0, height))
        v > 0 -> arrayOf(Point(verticalEdgeInset, 0.0), Point(width - verticalEdgeInset, 0.0), Point(width, height), Point(0.0, height))
        v < 0 -> arrayOf(Point(0.0, 0.0), Point(width, 0.0), Point(width - verticalEdgeInset, height), Point(verticalEdgeInset, height))
        else -> arrayOf(Point(0.0, 0.0), Point(width, 0.0), Point(width, height), Point(0.0, height))
    }
    val sourcePoints = MatOfPoint2f(Point(0.0, 0.0), Point(width, 0.0), Point(width, height), Point(0.0, height))
    val destinationPoints = MatOfPoint2f(*destination)
    val transform = Imgproc.getPerspectiveTransform(sourcePoints, destinationPoints)
    val output = Mat()
    Imgproc.warpPerspective(rgba, output, transform, rgba.size(), Imgproc.INTER_CUBIC, Core.BORDER_REPLICATE)
    sourcePoints.release(); destinationPoints.release(); transform.release()
    output
}

/** Local, full-resolution editing tools shared by the public and Secure Gallery editors. */
internal suspend fun autoEnhancePhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val lab = Mat()
    Imgproc.cvtColor(rgba, lab, Imgproc.COLOR_RGBA2RGB)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_RGB2Lab)
    val channels = ArrayList<Mat>(3)
    Core.split(lab, channels)
    Imgproc.createCLAHE(2.2, Size(8.0, 8.0)).apply(channels[0], channels[0])
    Core.merge(channels, lab)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_Lab2RGB)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_RGB2RGBA)
    channels.forEach(Mat::release)
    lab
}

internal suspend fun recoverLowLightPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val corrected = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    val lookup = Mat(1, 256, CvType.CV_8U)
    lookup.put(0, 0, ByteArray(256) { index ->
        ((index / 255.0).pow(.64) * 255.0).toInt().coerceIn(0, 255).toByte()
    })
    Core.LUT(rgb, lookup, corrected)
    lookup.release()
    rgb.release()
    Imgproc.cvtColor(corrected, corrected, Imgproc.COLOR_RGB2RGBA)
    corrected
}

internal suspend fun denoisePhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val cleaned = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Photo.fastNlMeansDenoisingColored(rgb, cleaned, 7f, 7f, 7, 21)
    rgb.release()
    Imgproc.cvtColor(cleaned, cleaned, Imgproc.COLOR_RGB2RGBA)
    cleaned
}

internal suspend fun improveClarityPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val detailed = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Photo.detailEnhance(rgb, detailed, 10f, .15f)
    rgb.release()
    Imgproc.cvtColor(detailed, detailed, Imgproc.COLOR_RGB2RGBA)
    detailed
}

internal suspend fun whiteBalancePhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    val channels = ArrayList<Mat>(3)
    Core.split(rgb, channels)
    val averages = channels.map { Core.mean(it).`val`[0].coerceAtLeast(1.0) }
    val neutral = averages.average()
    channels.forEachIndexed { index, channel ->
        Core.multiply(channel, Scalar(neutral / averages[index]), channel)
    }
    Core.merge(channels, rgb)
    channels.forEach(Mat::release)
    Imgproc.cvtColor(rgb, rgb, Imgproc.COLOR_RGB2RGBA)
    rgb
}

internal suspend fun softGlowPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val glow = Mat()
    val output = Mat()
    Imgproc.GaussianBlur(rgba, glow, Size(0.0, 0.0), 6.0)
    Core.addWeighted(rgba, .76, glow, .34, 4.0, output)
    glow.release()
    output
}

internal suspend fun cleanDocumentPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val gray = Mat()
    Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.GaussianBlur(gray, gray, Size(3.0, 3.0), 0.0)
    Imgproc.adaptiveThreshold(
        gray,
        gray,
        255.0,
        Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
        Imgproc.THRESH_BINARY,
        31,
        12.0,
    )
    Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA)
    gray
}

internal suspend fun cleanColorDocumentPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val smooth = Mat()
    val lab = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Imgproc.bilateralFilter(rgb, smooth, 7, 38.0, 38.0)
    rgb.release()
    Imgproc.cvtColor(smooth, lab, Imgproc.COLOR_RGB2Lab)
    smooth.release()
    val channels = ArrayList<Mat>(3)
    Core.split(lab, channels)
    Imgproc.createCLAHE(2.8, Size(8.0, 8.0)).apply(channels[0], channels[0])
    Core.merge(channels, lab)
    channels.forEach(Mat::release)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_Lab2RGB)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_RGB2RGBA)
    lab
}

internal suspend fun smartSharpenPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val blur = Mat()
    val sharpened = Mat()
    Imgproc.GaussianBlur(rgba, blur, Size(0.0, 0.0), 2.2)
    Core.addWeighted(rgba, 1.55, blur, -0.55, 0.0, sharpened)
    blur.release()
    sharpened
}

internal suspend fun liftShadowsPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val lifted = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    val lookup = Mat(1, 256, CvType.CV_8U)
    lookup.put(0, 0, ByteArray(256) { index ->
        val normalized = index / 255.0
        val value = normalized + (1.0 - normalized) * normalized.pow(.62) * .34
        (value * 255.0).toInt().coerceIn(0, 255).toByte()
    })
    Core.LUT(rgb, lookup, lifted)
    lookup.release(); rgb.release()
    Imgproc.cvtColor(lifted, lifted, Imgproc.COLOR_RGB2RGBA)
    lifted
}

internal suspend fun recoverHighlightsPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val recovered = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    val lookup = Mat(1, 256, CvType.CV_8U)
    lookup.put(0, 0, ByteArray(256) { index ->
        val normalized = index / 255.0
        val value = normalized.pow(1.18) * .88 + normalized * .12
        (value * 255.0).toInt().coerceIn(0, 255).toByte()
    })
    Core.LUT(rgb, lookup, recovered)
    lookup.release(); rgb.release()
    Imgproc.cvtColor(recovered, recovered, Imgproc.COLOR_RGB2RGBA)
    recovered
}

internal suspend fun dehazePhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val lab = Mat()
    Imgproc.cvtColor(rgba, lab, Imgproc.COLOR_RGBA2RGB)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_RGB2Lab)
    val channels = ArrayList<Mat>(3)
    Core.split(lab, channels)
    Imgproc.createCLAHE(3.6, Size(10.0, 10.0)).apply(channels[0], channels[0])
    Core.merge(channels, lab)
    channels.forEach(Mat::release)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_Lab2RGB)
    Imgproc.cvtColor(lab, lab, Imgproc.COLOR_RGB2RGBA)
    lab
}

/** Selective saturation: muted colours are lifted more than already vivid colours. */
internal suspend fun vibrancePhoto(source: Bitmap, amount: Float = .32f): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(output.width * output.height)
    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    pixels.indices.forEach { index ->
        val color = pixels[index]
        val a = color ushr 24
        val r = color shr 16 and 0xFF; val g = color shr 8 and 0xFF; val b = color and 0xFF
        val maximum = maxOf(r, g, b).toFloat(); val minimum = minOf(r, g, b).toFloat()
        val saturation = if (maximum <= 0f) 0f else (maximum - minimum) / maximum
        val lift = 1f + amount.coerceIn(-1f, 1f) * (1f - saturation)
        val luminance = .2126f * r + .7152f * g + .0722f * b
        val nr = (luminance + (r - luminance) * lift).toInt().coerceIn(0, 255)
        val ng = (luminance + (g - luminance) * lift).toInt().coerceIn(0, 255)
        val nb = (luminance + (b - luminance) * lift).toInt().coerceIn(0, 255)
        pixels[index] = a shl 24 or (nr shl 16) or (ng shl 8) or nb
    }
    output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    pixels.fill(0)
    return output
}

internal suspend fun vignettePhoto(source: Bitmap, amount: Float = .38f): Bitmap {
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(output.width * output.height)
    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    val cx = (output.width - 1) / 2f; val cy = (output.height - 1) / 2f
    val maximum = sqrt(cx * cx + cy * cy).coerceAtLeast(1f)
    pixels.indices.forEach { index ->
        val x = index % output.width; val y = index / output.width
        val distance = sqrt((x - cx) * (x - cx) + (y - cy) * (y - cy)) / maximum
        val shade = (1f - amount.coerceIn(0f, .85f) * distance.pow(1.7f)).coerceIn(.15f, 1f)
        val color = pixels[index]
        pixels[index] = (color ushr 24 shl 24) or
            (((color shr 16 and 0xFF) * shade).toInt() shl 16) or
            (((color shr 8 and 0xFF) * shade).toInt() shl 8) or
            ((color and 0xFF) * shade).toInt()
    }
    output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    pixels.fill(0)
    return output
}

internal suspend fun filmGrainPhoto(source: Bitmap, amount: Int = 12): Bitmap {
    val strength = amount.coerceIn(1, 32)
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
    val pixels = IntArray(output.width * output.height)
    output.getPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    pixels.indices.forEach { index ->
        val x = index % output.width; val y = index / output.width
        val noise = (((x * 73856093) xor (y * 19349663) xor (index * 83492791)) and 0xFF) - 128
        val delta = noise * strength / 128
        val color = pixels[index]
        val r = ((color shr 16 and 0xFF) + delta).coerceIn(0, 255)
        val g = ((color shr 8 and 0xFF) + delta).coerceIn(0, 255)
        val b = ((color and 0xFF) + delta).coerceIn(0, 255)
        pixels[index] = (color ushr 24 shl 24) or (r shl 16) or (g shl 8) or b
    }
    output.setPixels(pixels, 0, output.width, 0, 0, output.width, output.height)
    pixels.fill(0)
    return output
}

internal suspend fun hdrDetailPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val detailed = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Photo.detailEnhance(rgb, detailed, 18f, .24f)
    rgb.release()
    Imgproc.cvtColor(detailed, detailed, Imgproc.COLOR_RGB2RGBA)
    detailed
}

internal suspend fun upscalePhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val longest = maxOf(rgba.cols(), rgba.rows()).coerceAtLeast(1)
    val factor = minOf(2.0, 6_144.0 / longest)
    val resized = Mat()
    Imgproc.resize(rgba, resized, Size(rgba.cols() * factor, rgba.rows() * factor), 0.0, 0.0, Imgproc.INTER_LANCZOS4)
    val blur = Mat()
    val output = Mat()
    Imgproc.GaussianBlur(resized, blur, Size(0.0, 0.0), 1.1)
    Core.addWeighted(resized, 1.32, blur, -.32, 0.0, output)
    resized.release(); blur.release()
    output
}

internal suspend fun restoreFadedPhoto(source: Bitmap): Bitmap {
    val balanced = whiteBalancePhoto(source)
    return try { autoEnhancePhoto(balanced) } finally { if (balanced !== source) balanced.recycle() }
}

internal suspend fun aiPortraitSmoothPhoto(source: Bitmap): Bitmap {
    val smooth = openCvBitmap(source) { rgba ->
        val rgb = Mat()
        val output = Mat()
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        Imgproc.bilateralFilter(rgb, output, 9, 58.0, 58.0)
        rgb.release()
        Imgproc.cvtColor(output, output, Imgproc.COLOR_RGB2RGBA)
        output
    }
    val pixels = IntArray(source.width * source.height)
    smooth.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    return try {
        compositePortrait(source, personSelectionMask(source), foreground = { _, index -> pixels[index] }, background = { color, _ -> color })
    } finally { smooth.recycle() }
}

internal suspend fun aiPortraitWarmPhoto(source: Bitmap): Bitmap = compositePortrait(
    source,
    personSelectionMask(source),
    background = { color, _ -> color },
    foreground = { color, _ -> gradeArgb(color, red = 1.07f, green = 1.025f, blue = .94f, saturation = 1.035f) },
)

internal suspend fun aiPortraitNightPhoto(source: Bitmap): Bitmap = compositePortrait(
    source,
    personSelectionMask(source),
    background = { color, _ -> gradeArgb(color, red = .55f, green = .62f, blue = .76f, saturation = .72f) },
    foreground = { color, _ -> adjustArgb(color, brightness = 1.08f, saturation = 1.03f) },
)

internal suspend fun removeDocumentShadowsPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    val channels = ArrayList<Mat>(3)
    Core.split(rgb, channels)
    channels.forEach { channel ->
        val background = Mat()
        Imgproc.medianBlur(channel, background, 31)
        Core.divide(channel, background, channel, 235.0)
        background.release()
    }
    Core.merge(channels, rgb)
    channels.forEach(Mat::release)
    Imgproc.cvtColor(rgb, rgb, Imgproc.COLOR_RGB2RGBA)
    rgb
}

internal suspend fun inkBoostDocumentPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val gray = Mat()
    Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.adaptiveThreshold(gray, gray, 255.0, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, 21, 9.0)
    Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA)
    gray
}

internal suspend fun grayscaleDocumentPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val gray = Mat()
    Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA)
    gray
}

internal suspend fun cinematicPhoto(source: Bitmap): Bitmap = transformPixels(source) { color ->
    val luminance = (.2126f * Color.red(color) + .7152f * Color.green(color) + .0722f * Color.blue(color)) / 255f
    if (luminance < .52f) gradeArgb(color, .88f, 1.01f, 1.12f, 1.08f)
    else gradeArgb(color, 1.10f, 1.02f, .88f, 1.08f)
}

internal suspend fun mattePhoto(source: Bitmap): Bitmap = transformPixels(source) { color ->
    fun matte(channel: Int) = (22f + channel * .84f).toInt().coerceIn(0, 255)
    Color.argb(Color.alpha(color), matte(Color.red(color)), matte(Color.green(color)), matte(Color.blue(color)))
}

internal suspend fun noirPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val gray = Mat()
    Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
    Imgproc.createCLAHE(3.1, Size(8.0, 8.0)).apply(gray, gray)
    Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA)
    gray
}

internal suspend fun comicPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val styled = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Photo.stylization(rgb, styled, 72f, .42f)
    rgb.release()
    Imgproc.cvtColor(styled, styled, Imgproc.COLOR_RGB2RGBA)
    styled
}

internal suspend fun pencilSketchPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val mono = Mat()
    val color = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Photo.pencilSketch(rgb, mono, color, 60f, .075f, .02f)
    rgb.release(); color.release()
    Imgproc.cvtColor(mono, mono, Imgproc.COLOR_GRAY2RGBA)
    mono
}

internal suspend fun watercolorPhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val rgb = Mat()
    val styled = Mat()
    Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
    Photo.stylization(rgb, styled, 48f, .34f)
    rgb.release()
    Imgproc.cvtColor(styled, styled, Imgproc.COLOR_RGB2RGBA)
    styled
}

internal suspend fun pixelatePhoto(source: Bitmap): Bitmap = openCvBitmap(source) { rgba ->
    val small = Mat()
    val output = Mat()
    Imgproc.resize(rgba, small, Size(maxOf(24.0, rgba.cols() / 34.0), maxOf(24.0, rgba.rows() / 34.0)), 0.0, 0.0, Imgproc.INTER_AREA)
    Imgproc.resize(small, output, rgba.size(), 0.0, 0.0, Imgproc.INTER_NEAREST)
    small.release()
    output
}

/** ML Kit finds people; OpenCV creates a soft, full-resolution background blur on device. */
internal suspend fun aiPortraitBackgroundBlur(source: Bitmap): Bitmap {
    val blurred = openCvBitmap(source) { rgba ->
        Mat().also { Imgproc.GaussianBlur(rgba, it, Size(0.0, 0.0), 18.0) }
    }
    val blurredPixels = IntArray(source.width * source.height)
    blurred.getPixels(blurredPixels, 0, source.width, 0, 0, source.width, source.height)
    return try {
        compositePortrait(source, personSelectionMask(source), background = { _, index -> blurredPixels[index] })
    } finally {
        blurred.recycle()
    }
}

internal suspend fun aiPortraitSpotlight(source: Bitmap): Bitmap = compositePortrait(
    source,
    personSelectionMask(source),
    background = { color, _ -> adjustArgb(color, brightness = .60f, saturation = .62f) },
    foreground = { color, _ -> adjustArgb(color, brightness = 1.10f, saturation = 1.04f) },
)

internal suspend fun aiPortraitColorPop(source: Bitmap): Bitmap = compositePortrait(
    source,
    personSelectionMask(source),
    background = { color, _ -> adjustArgb(color, brightness = .88f, saturation = 0f) },
)

internal suspend fun aiPortraitStudioBackground(source: Bitmap): Bitmap = compositePortrait(
    source,
    personSelectionMask(source),
    background = { _, _ -> Color.rgb(239, 241, 246) },
    foreground = { color, _ -> adjustArgb(color, brightness = 1.04f, saturation = 1.02f) },
)

private suspend fun segmentPerson(source: Bitmap): SegmentationMask {
    val options = SelfieSegmenterOptions.Builder()
        .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
        .enableRawSizeMask()
        .build()
    val segmenter = Segmentation.getClient(options)
    return try {
        suspendCancellableCoroutine { continuation ->
            segmenter.process(InputImage.fromBitmap(source, 0))
                .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
        }
    } finally {
        segmenter.close()
    }
}

/** Converts ML Kit's probability buffer into the same alpha-mask contract used by AI Select. */
internal suspend fun personSelectionMask(source: Bitmap): Bitmap {
    val result = segmentPerson(source)
    val probabilities = FloatArray(result.width * result.height)
    result.buffer.rewind()
    result.buffer.asFloatBuffer().get(probabilities)
    val pixels = IntArray(probabilities.size) { index ->
        val probability = ((probabilities[index] - .08f) / .84f).coerceIn(0f, 1f)
        Color.argb((probability * 255f).toInt(), 255, 255, 255)
    }
    val coarse = Bitmap.createBitmap(pixels, result.width, result.height, Bitmap.Config.ARGB_8888)
    return try {
        edgeAwareSelectionMask(source, coarse)
    } finally {
        coarse.recycle()
    }
}

/**
 * Snaps a semantic probability mask to the actual image edges with a guided filter. This retains
 * hair, fingers and soft clothing edges that were lost by the previous binary threshold while
 * suppressing colour spill from the low-resolution segmentation model.
 */
internal fun edgeAwareSelectionMask(source: Bitmap, semanticMask: Bitmap): Bitmap {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    val maximum = 1_280
    val scale = minOf(1f, maximum.toFloat() / maxOf(source.width, source.height))
    val width = (source.width * scale).toInt().coerceAtLeast(2)
    val height = (source.height * scale).toInt().coerceAtLeast(2)
    val guideBitmap = if (source.width == width && source.height == height) source
    else Bitmap.createScaledBitmap(source, width, height, true)
    val maskBitmap = Bitmap.createScaledBitmap(semanticMask, width, height, true)
    val rgba = Mat()
    val guide = Mat()
    val maskRgba = Mat()
    val probability = Mat()
    val meanGuide = Mat()
    val meanProbability = Mat()
    val guideSquared = Mat()
    val meanGuideSquared = Mat()
    val guideProbability = Mat()
    val meanGuideProbability = Mat()
    val variance = Mat()
    val covariance = Mat()
    val coefficientA = Mat()
    val coefficientB = Mat()
    val meanA = Mat()
    val meanB = Mat()
    val refined = Mat()
    return try {
        Utils.bitmapToMat(guideBitmap, rgba)
        Imgproc.cvtColor(rgba, guide, Imgproc.COLOR_RGBA2GRAY)
        guide.convertTo(guide, CvType.CV_32F, 1.0 / 255.0)
        Utils.bitmapToMat(maskBitmap, maskRgba)
        Core.extractChannel(maskRgba, probability, 3)
        probability.convertTo(probability, CvType.CV_32F, 1.0 / 255.0)

        val radius = (minOf(width, height) / 85).coerceIn(5, 18)
        val window = Size((radius * 2 + 1).toDouble(), (radius * 2 + 1).toDouble())
        Imgproc.boxFilter(guide, meanGuide, CvType.CV_32F, window)
        Imgproc.boxFilter(probability, meanProbability, CvType.CV_32F, window)
        Core.multiply(guide, guide, guideSquared)
        Imgproc.boxFilter(guideSquared, meanGuideSquared, CvType.CV_32F, window)
        Core.multiply(meanGuide, meanGuide, variance)
        Core.subtract(meanGuideSquared, variance, variance)
        Core.multiply(guide, probability, guideProbability)
        Imgproc.boxFilter(guideProbability, meanGuideProbability, CvType.CV_32F, window)
        Core.multiply(meanGuide, meanProbability, covariance)
        Core.subtract(meanGuideProbability, covariance, covariance)
        Core.add(variance, Scalar(0.0025), variance)
        Core.divide(covariance, variance, coefficientA)
        Core.multiply(coefficientA, meanGuide, coefficientB)
        Core.subtract(meanProbability, coefficientB, coefficientB)
        Imgproc.boxFilter(coefficientA, meanA, CvType.CV_32F, window)
        Imgproc.boxFilter(coefficientB, meanB, CvType.CV_32F, window)
        Core.multiply(meanA, guide, refined)
        Core.add(refined, meanB, refined)

        // Tighten only the uncertain halo. High-confidence semantic pixels remain soft enough for
        // natural hair and motion edges instead of becoming the former cardboard cut-out.
        Core.subtract(refined, Scalar(0.07), refined)
        Core.multiply(refined, Scalar(1.0 / 0.86), refined)
        Imgproc.threshold(refined, refined, 1.0, 1.0, Imgproc.THRESH_TRUNC)
        Imgproc.threshold(refined, refined, 0.0, 0.0, Imgproc.THRESH_TOZERO)
        refined.convertTo(refined, CvType.CV_8U, 255.0)
        alphaMaskBitmapLike(source, refined)
    } finally {
        rgba.release(); guide.release(); maskRgba.release(); probability.release()
        meanGuide.release(); meanProbability.release(); guideSquared.release(); meanGuideSquared.release()
        guideProbability.release(); meanGuideProbability.release(); variance.release(); covariance.release()
        coefficientA.release(); coefficientB.release(); meanA.release(); meanB.release(); refined.release()
        if (guideBitmap !== source) guideBitmap.recycle()
        maskBitmap.recycle()
    }
}

private fun compositePortrait(
    source: Bitmap,
    mask: Bitmap,
    background: (color: Int, pixelIndex: Int) -> Int,
    foreground: (color: Int, pixelIndex: Int) -> Int = { color, _ -> color },
): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    val fittedMask = if (mask.width == source.width && mask.height == source.height) mask
    else Bitmap.createScaledBitmap(mask, source.width, source.height, true)
    val probabilities = IntArray(source.width * source.height)
    fittedMask.getPixels(probabilities, 0, source.width, 0, 0, source.width, source.height)
    for (y in 0 until source.height) {
        for (x in 0 until source.width) {
            val index = y * source.width + x
            val person = Color.alpha(probabilities[index]) / 255f
            pixels[index] = blendArgb(background(pixels[index], index), foreground(pixels[index], index), person)
        }
    }
    if (fittedMask !== mask) fittedMask.recycle()
    mask.recycle()
    return createEditBitmapLike(source).also {
        it.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    }
}

private fun adjustArgb(color: Int, brightness: Float, saturation: Float): Int {
    val red = Color.red(color).toFloat()
    val green = Color.green(color).toFloat()
    val blue = Color.blue(color).toFloat()
    val luma = .2126f * red + .7152f * green + .0722f * blue
    fun channel(value: Float): Int = ((luma + (value - luma) * saturation) * brightness).toInt().coerceIn(0, 255)
    return Color.argb(Color.alpha(color), channel(red), channel(green), channel(blue))
}

private fun gradeArgb(color: Int, red: Float, green: Float, blue: Float, saturation: Float): Int {
    val adjusted = adjustArgb(color, brightness = 1f, saturation = saturation)
    return Color.argb(
        Color.alpha(color),
        (Color.red(adjusted) * red).toInt().coerceIn(0, 255),
        (Color.green(adjusted) * green).toInt().coerceIn(0, 255),
        (Color.blue(adjusted) * blue).toInt().coerceIn(0, 255),
    )
}

private fun transformPixels(source: Bitmap, transform: (Int) -> Int): Bitmap {
    val pixels = IntArray(source.width * source.height)
    source.getPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    pixels.indices.forEach { pixels[it] = transform(pixels[it]) }
    return createEditBitmapLike(source).also {
        it.setPixels(pixels, 0, source.width, 0, 0, source.width, source.height)
    }
}

internal fun openCvBitmap(source: Bitmap, operation: (Mat) -> Mat): Bitmap {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    require(source.width > 0 && source.height > 0 && !source.isRecycled) { "The source image is unavailable" }
    // Camera/Coil commonly provide immutable hardware bitmaps. Native OpenCV cannot safely
    // lock those pixels and used to fail or abort for otherwise valid AI actions.
    val safeSource = if (source.config == Bitmap.Config.HARDWARE || source.config != Bitmap.Config.ARGB_8888) {
        source.copy(Bitmap.Config.ARGB_8888, false)
            ?: createEditBitmapLike(source).also { Canvas(it).drawBitmap(source, 0f, 0f, null) }
    } else source
    val input = Mat()
    try { Utils.bitmapToMat(safeSource, input) } finally { if (safeSource !== source) safeSource.recycle() }
    val result = try { operation(input) } finally { input.release() }
    require(!result.empty()) { "The edit engine returned an empty image" }
    return try {
        createEditBitmapLike(source, result.cols(), result.rows()).also { Utils.matToBitmap(result, it) }
    } finally {
        result.release()
    }
}

/** Effects driven by MagicTouch's arbitrary-object probability mask. Unlike the portrait tools,
 * these work on products, pets, vehicles, food, documents, or any object the user points at. */
internal suspend fun aiSelectionBackgroundBlur(source: Bitmap, mask: Bitmap): Bitmap {
    val blurred = openCvBitmap(source) { rgba ->
        Mat().also { Imgproc.GaussianBlur(rgba, it, Size(0.0, 0.0), 24.0) }
    }
    return try { compositeSelection(source, blurred, mask) } finally { blurred.recycle() }
}

internal suspend fun aiSelectionColorPop(source: Bitmap, mask: Bitmap): Bitmap {
    val mono = openCvBitmap(source) { rgba ->
        val gray = Mat()
        Imgproc.cvtColor(rgba, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.cvtColor(gray, gray, Imgproc.COLOR_GRAY2RGBA)
        gray
    }
    return try { compositeSelection(source, mono, mask) } finally { mono.recycle() }
}

internal suspend fun aiSelectionRelight(source: Bitmap, mask: Bitmap): Bitmap {
    val darkBackground = transformPixels(source) { adjustArgb(it, brightness = .58f, saturation = .70f) }
    val liftedSubject = transformPixels(source) { adjustArgb(it, brightness = 1.13f, saturation = 1.05f) }
    return try { compositeSelection(liftedSubject, darkBackground, mask) } finally {
        darkBackground.recycle(); liftedSubject.recycle()
    }
}

internal suspend fun aiSelectionStudioBackdrop(source: Bitmap, mask: Bitmap): Bitmap {
    val width = source.width.coerceAtLeast(1)
    val height = source.height.coerceAtLeast(1)
    val backdrop = createEditBitmapLike(source, width, height)
    val pixels = IntArray(width * height)
    for (y in 0 until height) {
        val normalizedY = y.toFloat() / (height - 1).coerceAtLeast(1)
        val shade = (247f - normalizedY * 38f).toInt().coerceIn(0, 255)
        for (x in 0 until width) {
            val normalizedX = x.toFloat() / (width - 1).coerceAtLeast(1)
            val blue = (shade + 8f + 12f * (1f - normalizedX)).toInt().coerceIn(0, 255)
            pixels[y * width + x] = Color.rgb(shade, (shade + 3).coerceAtMost(255), blue)
        }
    }
    backdrop.setPixels(pixels, 0, width, 0, 0, width, height)
    return try { compositeSelection(source, backdrop, mask) } finally { backdrop.recycle() }
}

internal suspend fun aiSelectionSticker(source: Bitmap, mask: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val fittedMask = if (mask.width == width && mask.height == height) mask
    else Bitmap.createScaledBitmap(mask, width, height, true)
    val sourcePixels = IntArray(width * height)
    val maskPixels = IntArray(width * height)
    source.getPixels(sourcePixels, 0, width, 0, 0, width, height)
    fittedMask.getPixels(maskPixels, 0, width, 0, 0, width, height)
    val output = IntArray(width * height) { Color.rgb(244, 245, 250) }
    val outlineRadius = maxOf(2, minOf(width, height) / 140)
    for (y in 0 until height) for (x in 0 until width) {
        val index = y * width + x
        val alpha = Color.alpha(maskPixels[index])
        if (alpha > 110) output[index] = sourcePixels[index]
        else if (alpha > 18 || hasSelectedNeighbour(maskPixels, width, height, x, y, outlineRadius)) {
            output[index] = Color.WHITE
        }
    }
    if (fittedMask !== mask) fittedMask.recycle()
    return createEditBitmapFromPixels(source, output, width, height)
}

internal suspend fun aiSelectionErase(source: Bitmap, mask: Bitmap): Bitmap {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    require(source.width > 1 && source.height > 1 && mask.width > 1 && mask.height > 1) { "This image cannot be erased" }

    // Locate the object on a bounded inspection mask first. The previous implementation expanded
    // every Pixel photo into several full-resolution Bitmaps, IntArrays and Mats at once, which
    // could exhaust the heap or abort inside native OpenCV before Kotlin could catch the error.
    val inspectScale = minOf(1f, 640f / maxOf(mask.width, mask.height))
    val inspection = if (inspectScale < 1f) Bitmap.createScaledBitmap(
        mask,
        (mask.width * inspectScale).toInt().coerceAtLeast(2),
        (mask.height * inspectScale).toInt().coerceAtLeast(2),
        true,
    ) else mask
    val inspectionWidth = inspection.width
    val inspectionHeight = inspection.height
    val inspectionPixels = IntArray(inspectionWidth * inspectionHeight)
    inspection.getPixels(inspectionPixels, 0, inspectionWidth, 0, 0, inspectionWidth, inspectionHeight)
    var minX = inspectionWidth
    var minY = inspectionHeight
    var maxX = -1
    var maxY = -1
    var selected = 0
    inspectionPixels.forEachIndexed { index, pixel ->
        if (Color.alpha(pixel) > 72) {
            val x = index % inspectionWidth
            val y = index / inspectionWidth
            minX = minOf(minX, x); minY = minOf(minY, y)
            maxX = maxOf(maxX, x); maxY = maxOf(maxY, y)
            selected++
        }
    }
    if (inspection !== mask) inspection.recycle()
    require(selected > 8 && maxX >= minX && maxY >= minY) { "Select an object before using Object eraser" }
    require(selected.toFloat() / inspectionPixels.size < .72f) { "Make a smaller selection for Object eraser" }

    val scaleX = source.width.toFloat() / inspectionWidth
    val scaleY = source.height.toFloat() / inspectionHeight
    val padding = (minOf(source.width, source.height) * .045f).toInt().coerceIn(28, 240)
    val left = (minX * scaleX).toInt().minus(padding).coerceAtLeast(0)
    val top = (minY * scaleY).toInt().minus(padding).coerceAtLeast(0)
    val right = (((maxX + 1) * scaleX).toInt() + padding).coerceAtMost(source.width)
    val bottom = (((maxY + 1) * scaleY).toInt() + padding).coerceAtMost(source.height)
    val cropWidth = (right - left).coerceAtLeast(2)
    val cropHeight = (bottom - top).coerceAtLeast(2)
    val sourceCrop = Bitmap.createBitmap(source, left, top, cropWidth, cropHeight)

    val maskLeft = (left.toFloat() / source.width * mask.width).toInt().coerceIn(0, mask.width - 1)
    val maskTop = (top.toFloat() / source.height * mask.height).toInt().coerceIn(0, mask.height - 1)
    val maskRight = (right.toFloat() / source.width * mask.width).toInt().coerceIn(maskLeft + 1, mask.width)
    val maskBottom = (bottom.toFloat() / source.height * mask.height).toInt().coerceIn(maskTop + 1, mask.height)
    val maskCrop = Bitmap.createBitmap(mask, maskLeft, maskTop, maskRight - maskLeft, maskBottom - maskTop)

    val processingScale = minOf(1f, 2048f / maxOf(cropWidth, cropHeight))
    val processWidth = (cropWidth * processingScale).toInt().coerceAtLeast(2)
    val processHeight = (cropHeight * processingScale).toInt().coerceAtLeast(2)
    val processSource = if (processingScale < 1f) Bitmap.createScaledBitmap(sourceCrop, processWidth, processHeight, true) else sourceCrop
    val processMask = Bitmap.createScaledBitmap(maskCrop, processWidth, processHeight, true)
    val maskPixels = IntArray(processWidth * processHeight)
    processMask.getPixels(maskPixels, 0, processWidth, 0, 0, processWidth, processHeight)
    val maskBytes = ByteArray(maskPixels.size) { index -> if (Color.alpha(maskPixels[index]) > 72) 0xFF.toByte() else 0 }

    val rgba = Mat()
    val rgb = Mat()
    val result = Mat()
    val maskMat = Mat(processHeight, processWidth, CvType.CV_8UC1)
    val kernelSize = (minOf(processWidth, processHeight) / 90).coerceIn(3, 17).let { if (it % 2 == 0) it + 1 else it }
    val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, Size(kernelSize.toDouble(), kernelSize.toDouble()))
    val processed = try {
        Utils.bitmapToMat(processSource, rgba)
        Imgproc.cvtColor(rgba, rgb, Imgproc.COLOR_RGBA2RGB)
        maskMat.put(0, 0, maskBytes)
        Imgproc.dilate(maskMat, maskMat, kernel)
        Photo.inpaint(rgb, maskMat, result, (kernelSize * .55).coerceAtLeast(3.0), Photo.INPAINT_TELEA)
        Imgproc.cvtColor(result, result, Imgproc.COLOR_RGB2RGBA)
        createEditBitmapLike(source, processWidth, processHeight).also { Utils.matToBitmap(result, it) }
    } finally {
        rgba.release(); rgb.release(); result.release(); maskMat.release(); kernel.release()
        processMask.recycle()
        maskCrop.recycle()
        if (processSource !== sourceCrop) processSource.recycle()
        sourceCrop.recycle()
    }
    val fittedResult = if (processed.width == cropWidth && processed.height == cropHeight) processed
    else Bitmap.createScaledBitmap(processed, cropWidth, cropHeight, true)
    val output = source.copy(Bitmap.Config.ARGB_8888, true)
        ?: createEditBitmapLike(source).also { android.graphics.Canvas(it).drawBitmap(source, 0f, 0f, null) }
    android.graphics.Canvas(output).drawBitmap(fittedResult, left.toFloat(), top.toFloat(), null)
    if (fittedResult !== processed) fittedResult.recycle()
    processed.recycle()
    return output
}

/** High-quality, fully on-device content-aware fill backed by Big-LaMa instead of Telea. */
internal suspend fun aiSelectionErase(context: Context, source: Bitmap, mask: Bitmap): Bitmap {
    check(OpenCVLoader.initLocal()) { "OpenCV could not be initialized" }
    val repair = selectionRepairRegion(source, mask)
    val sourceCrop = Bitmap.createBitmap(source, repair.left, repair.top, repair.width(), repair.height())
    val fittedMask = if (mask.width == source.width && mask.height == source.height) mask
    else Bitmap.createScaledBitmap(mask, source.width, source.height, true)
    val maskCrop = Bitmap.createBitmap(fittedMask, repair.left, repair.top, repair.width(), repair.height())
    if (fittedMask !== mask) fittedMask.recycle()
    val preparedMask = prepareNeuralInpaintMask(maskCrop)
    maskCrop.recycle()
    val repaired = try {
        lamaInpaintingEngine(context).inpaint(sourceCrop, preparedMask)
    } finally {
        sourceCrop.recycle()
        preparedMask.recycle()
    }
    return try {
        val output = source.copy(Bitmap.Config.ARGB_8888, true)
            ?: createEditBitmapLike(source).also { Canvas(it).drawBitmap(source, 0f, 0f, null) }
        Canvas(output).drawBitmap(repaired, repair.left.toFloat(), repair.top.toFloat(), null)
        output
    } finally {
        repaired.recycle()
    }
}

private fun selectionRepairRegion(source: Bitmap, mask: Bitmap): android.graphics.Rect {
    val inspection = if (mask.width <= 640 && mask.height <= 640) mask else {
        val scale = 640f / maxOf(mask.width, mask.height)
        Bitmap.createScaledBitmap(mask, (mask.width * scale).toInt(), (mask.height * scale).toInt(), true)
    }
    val pixels = IntArray(inspection.width * inspection.height)
    inspection.getPixels(pixels, 0, inspection.width, 0, 0, inspection.width, inspection.height)
    var minX = inspection.width
    var minY = inspection.height
    var maxX = -1
    var maxY = -1
    var count = 0
    pixels.forEachIndexed { index, color ->
        if (Color.alpha(color) >= 54) {
            val x = index % inspection.width
            val y = index / inspection.width
            minX = minOf(minX, x); minY = minOf(minY, y)
            maxX = maxOf(maxX, x); maxY = maxOf(maxY, y)
            count++
        }
    }
    val inspectionWidth = inspection.width
    val inspectionHeight = inspection.height
    if (inspection !== mask) inspection.recycle()
    require(count >= 12 && maxX >= minX && maxY >= minY) { "Select the complete object before using Content-aware fill" }
    require(count.toFloat() / pixels.size < .75f) { "Select a smaller area so Content-aware fill has enough surroundings" }
    val scaleX = source.width.toFloat() / inspectionWidth
    val scaleY = source.height.toFloat() / inspectionHeight
    val objectLeft = (minX * scaleX).toInt()
    val objectTop = (minY * scaleY).toInt()
    val objectRight = ((maxX + 1) * scaleX).toInt()
    val objectBottom = ((maxY + 1) * scaleY).toInt()
    val objectWidth = (objectRight - objectLeft).coerceAtLeast(1)
    val objectHeight = (objectBottom - objectTop).coerceAtLeast(1)
    val horizontalContext = maxOf((objectWidth * .72f).toInt(), source.width / 16, 48)
    val verticalContext = maxOf((objectHeight * .72f).toInt(), source.height / 16, 48)
    return android.graphics.Rect(
        (objectLeft - horizontalContext).coerceAtLeast(0),
        (objectTop - verticalContext).coerceAtLeast(0),
        (objectRight + horizontalContext).coerceAtMost(source.width),
        (objectBottom + verticalContext).coerceAtMost(source.height),
    )
}

private fun prepareNeuralInpaintMask(source: Bitmap): Bitmap {
    val rgba = Mat()
    val gray = Mat()
    val minimum = minOf(source.width, source.height)
    val radius = (minimum / 160).coerceIn(2, 18)
    val kernel = Imgproc.getStructuringElement(
        Imgproc.MORPH_ELLIPSE,
        Size((radius * 2 + 1).toDouble(), (radius * 2 + 1).toDouble()),
    )
    return try {
        Utils.bitmapToMat(source, rgba)
        Core.extractChannel(rgba, gray, 3)
        Imgproc.threshold(gray, gray, 54.0, 255.0, Imgproc.THRESH_BINARY)
        Imgproc.morphologyEx(gray, gray, Imgproc.MORPH_CLOSE, kernel)
        Imgproc.dilate(gray, gray, kernel)
        Imgproc.GaussianBlur(gray, gray, Size(0.0, 0.0), maxOf(1.0, radius * .55))
        alphaMaskBitmapLike(source, gray)
    } finally {
        rgba.release(); gray.release(); kernel.release()
    }
}

/** Converts an OpenCV 8-bit probability plane to the app's white-RGB/alpha-mask contract. */
internal fun alphaMaskBitmapLike(reference: Bitmap, alpha: Mat): Bitmap {
    require(alpha.type() == CvType.CV_8UC1) { "Expected an 8-bit one-channel alpha mask" }
    val bytes = ByteArray(alpha.rows() * alpha.cols())
    alpha.get(0, 0, bytes)
    val pixels = IntArray(bytes.size) { index -> Color.argb(bytes[index].toInt() and 0xff, 255, 255, 255) }
    bytes.fill(0)
    return createEditBitmapFromPixels(reference, pixels, alpha.cols(), alpha.rows())
}

@Volatile private var sharedLamaInpaintingEngine: LamaInpaintingEngine? = null

private fun lamaInpaintingEngine(context: Context): LamaInpaintingEngine =
    sharedLamaInpaintingEngine ?: synchronized(AiPhotoToolsLamaLock) {
        sharedLamaInpaintingEngine ?: LamaInpaintingEngine(context.applicationContext).also {
            sharedLamaInpaintingEngine = it
        }
    }

private object AiPhotoToolsLamaLock

/** [foreground] is kept where the AI mask is opaque; [background] is visible elsewhere. */
private fun compositeSelection(foreground: Bitmap, background: Bitmap, mask: Bitmap): Bitmap {
    val width = foreground.width
    val height = foreground.height
    val fittedBackground = if (background.width == width && background.height == height) background
    else Bitmap.createScaledBitmap(background, width, height, true)
    val fittedMask = if (mask.width == width && mask.height == height) mask
    else Bitmap.createScaledBitmap(mask, width, height, true)
    val foregroundPixels = IntArray(width * height)
    val backgroundPixels = IntArray(width * height)
    val maskPixels = IntArray(width * height)
    foreground.getPixels(foregroundPixels, 0, width, 0, 0, width, height)
    fittedBackground.getPixels(backgroundPixels, 0, width, 0, 0, width, height)
    fittedMask.getPixels(maskPixels, 0, width, 0, 0, width, height)
    for (index in foregroundPixels.indices) {
        val probability = (Color.alpha(maskPixels[index]) / 255f).coerceIn(0f, 1f)
        foregroundPixels[index] = blendArgb(backgroundPixels[index], foregroundPixels[index], probability)
    }
    if (fittedBackground !== background) fittedBackground.recycle()
    if (fittedMask !== mask) fittedMask.recycle()
    return createEditBitmapFromPixels(foreground, foregroundPixels, width, height)
}

private fun hasSelectedNeighbour(mask: IntArray, width: Int, height: Int, x: Int, y: Int, radius: Int): Boolean {
    val x0 = (x - radius).coerceAtLeast(0)
    val x1 = (x + radius).coerceAtMost(width - 1)
    val y0 = (y - radius).coerceAtLeast(0)
    val y1 = (y + radius).coerceAtMost(height - 1)
    return Color.alpha(mask[y0 * width + x]) > 110 || Color.alpha(mask[y1 * width + x]) > 110 ||
        Color.alpha(mask[y * width + x0]) > 110 || Color.alpha(mask[y * width + x1]) > 110
}

private fun blendArgb(background: Int, foreground: Int, amount: Float): Int {
    fun channel(shift: Int): Int {
        val back = background ushr shift and 0xff
        val front = foreground ushr shift and 0xff
        return (back + (front - back) * amount).toInt().coerceIn(0, 255)
    }
    return (channel(24) shl 24) or (channel(16) shl 16) or (channel(8) shl 8) or channel(0)
}
