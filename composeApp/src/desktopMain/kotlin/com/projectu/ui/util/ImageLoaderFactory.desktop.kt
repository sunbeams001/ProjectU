package com.projectu.ui.util

import coil3.ImageLoader
import coil3.PlatformContext
import coil3.disk.DiskCache
import coil3.disk.directory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.projectu.shared.domain.model.CacheSize
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import okio.Path.Companion.toPath
import java.io.File

/**
 * 默认缓存大小（512MB）
 */
private val DEFAULT_CACHE_SIZE = CacheSize.DEFAULT.sizeInBytes

/**
 * 获取桌面平台的缓存目录
 */
private fun getDesktopCacheDir(): File {
    val userHome = System.getProperty("user.home")
    val cacheDir = when {
        System.getProperty("os.name").lowercase().contains("win") -> {
            File(System.getenv("LOCALAPPDATA") ?: "$userHome/AppData/Local", "ProjectU/cache")
        }
        System.getProperty("os.name").lowercase().contains("mac") -> {
            File("$userHome/Library/Caches/ProjectU")
        }
        else -> {
            File(System.getenv("XDG_CACHE_HOME") ?: "$userHome/.cache", "ProjectU")
        }
    }
    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }
    return cacheDir
}

/**
 * Desktop 平台的 ImageLoader 配置
 * 使用 Ktor 拦截器添加 Referer 头
 */
actual fun createImageLoader(context: PlatformContext, maxCacheSizeBytes: Long): ImageLoader {
    // 创建带 Referer 拦截器的 Ktor HttpClient
    val httpClient = HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 30000
            connectTimeoutMillis = 30000
        }
        
        // 添加默认请求拦截器
        defaultRequest {
            header("Referer", "https://www.pixiv.net/")
        }
    }
    
    // 配置磁盘缓存
    val diskCache = DiskCache.Builder()
        .directory(getDesktopCacheDir().resolve("image_cache").absolutePath.toPath())
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

