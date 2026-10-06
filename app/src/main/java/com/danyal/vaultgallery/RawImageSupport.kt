package com.danyal.vaultgallery

internal fun isRawImage(mimeType: String, name: String): Boolean {
    val mime = mimeType.lowercase()
    val extension = name.substringAfterLast('.', "").lowercase()
    return mime in setOf(
        "image/x-adobe-dng", "image/dng", "image/x-canon-cr2", "image/x-canon-cr3",
        "image/x-nikon-nef", "image/x-sony-arw", "image/x-panasonic-rw2", "image/x-olympus-orf",
        "image/x-fuji-raf", "image/x-pentax-pef", "image/x-samsung-srw",
    ) || extension in setOf("dng", "cr2", "cr3", "nef", "arw", "rw2", "orf", "raf", "pef", "srw")
}
