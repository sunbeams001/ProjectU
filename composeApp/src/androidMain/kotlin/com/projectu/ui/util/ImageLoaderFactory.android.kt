package com.projectu.ui.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.projectu.shared.domain.model.CacheSize
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okio.Path.Companion.toOkioPath

/**
 * 默认缓存大小（512MB）
 */
private val DEFAULT_CACHE_SIZE = CacheSize.DEFAULT.sizeInBytes

/**
 * Android 平台的 ImageLoader 配置
 * 使用 Ktor 拦截器添加 Referer 头
 */
actual fun createImageLoader(context: PlatformContext, maxCacheSizeBytes: Long): ImageLoader {
    // 创建带 Referer 拦截器的 Ktor HttpClient
    // 使用 OkHttp 引擎以获得更好的 Android 平台性能优化
    // 配置优先使用 HTTP/2 协议
    val okHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .build()
    
    val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
        
        install(HttpTimeout) {
            requestTimeoutMillis = 15000
            connectTimeoutMillis = 10000
        }
        
        // 添加默认请求拦截器
        defaultRequest {
            header("Referer", "https://www.pixiv.net/")
        }
    }
    
    // 配置磁盘缓存
    val diskCache = DiskCache.Builder()
        .directory(context.cacheDir.resolve("image_cache").toOkioPath())
        .maxSizeBytes(maxCacheSizeBytes)
        .build()
    
    return ImageLoader.Builder(context)
        .components {
            add(KtorNetworkFetcherFactory(httpClient))
            add(PixivImageKeyer())
        }
        .diskCache(diskCache)
        .applyPixivConfiguration()
        .build()
}

/**
 * 使用默认缓存大小创建 ImageLoader
 */
actual fun createImageLoader(context: PlatformContext): ImageLoader {
    return createImageLoader(context, DEFAULT_CACHE_SIZE)
}

/**
 * 获取 ImageLoader 的磁盘缓存
 */
actual fun getImageLoaderDiskCache(imageLoader: ImageLoader): Any? {
    return imageLoader.diskCache
}

