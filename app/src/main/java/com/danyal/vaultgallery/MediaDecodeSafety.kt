package com.danyal.vaultgallery

/** Hard limits for hostile/corrupt raster headers. Normal high-resolution phone photos remain valid. */
internal fun imageDecodeSafetyIssue(
    width: Int,
    height: Int,
    encodedBytes: Long = -1L,
    mimeType: String? = null,
): String? {
    if (width <= 0 || height <= 0) return "The image dimensions are invalid."
    if (width > 32_768 || height > 32_768) return "The image dimensions exceed the safe decoder limit."
    val pixels = width.toLong() * height.toLong()
    if (pixels > 300_000_000L) return "The image exceeds the 300-megapixel safe decoder limit."
    if (encodedBytes > 2L * 1024 * 1024 * 1024) return "The encoded image exceeds the 2 GB safe decoder limit."
    if (!mimeType.isNullOrBlank() && !mimeType.startsWith("image/", ignoreCase = true)) {
        return "The selected file is not reported as an image."
    }
    return null
}
