package com.projectu.ui.util

import coil3.ImageLoader
import coil3.disk.DiskCache
import com.projectu.shared.domain.model.CacheSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * 创建 Android 平台的图片缓存管理器实例
 */
actual fun createImageCacheManager(
    imageLoader: ImageLoader,
    maxCacheSizeBytes: Long
): ImageCacheManager = ImageCacheManagerImpl(imageLoader, maxCacheSizeBytes)

/**
 * Android 平台的图片缓存管理器实现
 */
class ImageCacheManagerImpl(
    private val imageLoader: ImageLoader,
    initialMaxSize: Long
) : ImageCacheManager {
    
    private val _currentCacheSize = MutableStateFlow(0L)
    override val currentCacheSize: StateFlow<Long> = _currentCacheSize.asStateFlow()
    
    private var _maxCacheSize: Long = initialMaxSize
    override val maxCacheSize: Long
        get() = _maxCacheSize
    
    private val diskCache: DiskCache?
        get() = imageLoader.diskCache
    
    override suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        diskCache?.size ?: 0L
    }
    
    override suspend fun clearCache() = withContext(Dispatchers.IO) {
        diskCache?.clear()
        refreshCacheSize()
    }
    
    override fun updateCacheSize(size: CacheSize) {
        _maxCacheSize = size.sizeInBytes
        // 注意：Coil 的 ImageLoader 创建后无法修改缓存大小
        // 需要重启应用才能完全生效
    }
    
    override suspend fun refreshCacheSize() {
        _currentCacheSize.value = getCacheSize()
    }
}
