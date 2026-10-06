package com.danyal.vaultgallery

import android.content.Context
import android.net.Uri
import androidx.exifinterface.media.ExifInterface

internal data class ImageTechnicalInfo(
    val camera: String?,
    val lens: String?,
    val exposure: String?,
    val aperture: String?,
    val iso: String?,
    val focalLength: String?,
    val colorSpace: String?,
    val latitude: Double?,
    val longitude: Double?,
)

internal fun readImageTechnicalInfo(context: Context, uri: Uri): ImageTechnicalInfo =
    context.contentResolver.openInputStream(uri).use { input ->
        requireNotNull(input) { "Image metadata is unavailable" }
        val exif = ExifInterface(input)
        val make = exif.getAttribute(ExifInterface.TAG_MAKE)?.trim()?.takeIf(String::isNotBlank)
        val model = exif.getAttribute(ExifInterface.TAG_MODEL)?.trim()?.takeIf(String::isNotBlank)
        val color = when (exif.getAttributeInt(ExifInterface.TAG_COLOR_SPACE, -1)) {
            1 -> "sRGB"
            2 -> "Adobe RGB"
            0xFFFF -> "Uncalibrated"
            else -> null
        }
        val location = exif.latLong
        ImageTechnicalInfo(
            camera = listOfNotNull(make, model).distinct().joinToString(" ").ifBlank { null },
            lens = exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.trim()?.takeIf(String::isNotBlank),
            exposure = exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let { "$it s" },
            aperture = exif.getAttributeDouble(ExifInterface.TAG_F_NUMBER, Double.NaN)
                .takeUnless(Double::isNaN)?.let { "f/%.1f".format(java.util.Locale.ROOT, it) },
            iso = exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { "ISO $it" },
            focalLength = exif.getAttributeDouble(ExifInterface.TAG_FOCAL_LENGTH, Double.NaN)
                .takeUnless(Double::isNaN)?.let { "%.1f mm".format(java.util.Locale.ROOT, it) },
            colorSpace = color,
            latitude = location?.getOrNull(0),
            longitude = location?.getOrNull(1),
        )
    }
