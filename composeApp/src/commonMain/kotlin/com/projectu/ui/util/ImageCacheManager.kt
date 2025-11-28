package com.projectu.ui.util

import androidx.compose.runtime.staticCompositionLocalOf
import coil3.ImageLoader
import com.projectu.shared.domain.model.CacheSize
import kotlinx.coroutines.flow.StateFlow

/**
 * 图片缓存管理器 CompositionLocal
 * 用于在 Compose UI 树中访问缓存管理器
 */
val LocalImageCacheManager = staticCompositionLocalOf<ImageCacheManager> {
    error("ImageCacheManager not provided")
}

/**
 * 创建平台特定的 ImageCacheManager 实例
 * 
 * @param imageLoader Coil ImageLoader 实例
 * @param maxCacheSizeBytes 最大缓存大小（字节）
 */
expect fun createImageCacheManager(
    imageLoader: ImageLoader,
    maxCacheSizeBytes: Long
): ImageCacheManager

/**
 * 图片缓存管理器接口
 * 提供缓存查询、清空和配置功能
 */
interface ImageCacheManager {
    /**
     * 当前缓存大小（字节）的状态流
     */
    val currentCacheSize: StateFlow<Long>
    
    /**
     * 最大缓存大小（字节）
     */
    val maxCacheSize: Long
    
    /**
     * 获取当前缓存大小（字节）
     */
    suspend fun getCacheSize(): Long
    
    /**
     * 清空磁盘缓存
     */
    suspend fun clearCache()
    
    /**
     * 更新缓存配置
     * 注意：由于 Coil 的 ImageLoader 在创建后无法修改缓存配置，
     * 缓存大小变更需要重启应用才能完全生效
     * 
     * @param size 新的缓存大小设置
     */
    fun updateCacheSize(size: CacheSize)
    
    /**
     * 刷新缓存大小统计
     */
    suspend fun refreshCacheSize()
}
