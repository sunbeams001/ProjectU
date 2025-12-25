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
 * 缓存详情数据类
 * 用于展示不同类型缓存的大小统计
 */
data class CacheDetails(
    /** 图片缓存大小（Coil 磁盘缓存） */
    val imageCacheSize: Long = 0L,
    /** Ugoira (动图) 缓存大小 */
    val ugoiraCacheSize: Long = 0L
) {
    /** 总缓存大小 */
    val totalSize: Long
        get() = imageCacheSize + ugoiraCacheSize
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
     * 当前缓存详情的状态流（区分不同缓存类型）
     */
    val cacheDetails: StateFlow<CacheDetails>
    
    /**
     * 最大缓存大小（字节）
     */
    val maxCacheSize: Long
    
    /**
     * 获取当前缓存大小（字节）
     */
    suspend fun getCacheSize(): Long
    
    /**
     * 获取缓存详情（区分不同缓存类型）
     */
    suspend fun getCacheDetails(): CacheDetails
    
    /**
     * 清空所有磁盘缓存（图片+动图）
     */
    suspend fun clearCache()
    
    /**
     * 仅清空图片缓存
     */
    suspend fun clearImageCache()
    
    /**
     * 仅清空动图缓存
     */
    suspend fun clearUgoiraCache()
    
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
