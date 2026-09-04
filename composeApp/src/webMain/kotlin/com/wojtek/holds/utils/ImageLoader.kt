package com.wojtek.holds.utils

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.memory.MemoryCache
import coil3.network.ktor3.KtorNetworkFetcherFactory
import coil3.request.crossfade

/**
 * Creates and configures the default [ImageLoader] for the application.
 * Configures memory cache and Ktor 3 network fetcher.
 */
fun newImageLoader(
    context: PlatformContext = PlatformContext.INSTANCE,
): ImageLoader {
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory())
        }
        .memoryCache {
            MemoryCache.Builder()
                .maxSizePercent(context, percent = 0.25)
                .build()
        }
        .crossfade(true)
        .build()
}
