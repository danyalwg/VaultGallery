package com.danyal.vaultgallery

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import coil3.gif.AnimatedImageDecoder
import coil3.svg.SvgDecoder
import coil3.video.VideoFrameDecoder

class VaultGalleryApplication : Application(), SingletonImageLoader.Factory {
    override fun onCreate() {
        super.onCreate()
        org.maplibre.android.MapLibre.getInstance(this)
    }

    override fun newImageLoader(context: Context): ImageLoader = ImageLoader.Builder(context)
        .components {
            add(SvgDecoder.Factory())
            add(AnimatedImageDecoder.Factory())
            add(VideoFrameDecoder.Factory())
        }
        .crossfade(true)
        .build()
}
