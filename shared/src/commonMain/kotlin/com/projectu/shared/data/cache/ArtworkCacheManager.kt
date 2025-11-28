package com.projectu.shared.data.cache

import com.projectu.shared.domain.model.Artwork
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 全局作品缓存管理器
 * 
 * 用于缓存已加载过详情的作品数据，避免重复调用API。
 * 
 * 核心特性：
 * - 纯内存缓存，高性能访问
 * - 区分"基础数据"和"详情数据"
 * - 支持批量操作和Flow响应式更新
 * - 线程安全
 * 
 * 缓存策略：
 * - 作品详情（包括多页数据）一旦加载成功就缓存
 * - 缓存会随应用生命周期存在，应用重启后清空
 * - 可通过 hasDetailLoaded 判断是否需要调用详情API
 */
class ArtworkCacheManager {
    
    // 作品缓存：artworkId -> Artwork
    private val cache = MutableStateFlow<Map<String, ArtworkCacheEntry>>(emptyMap())
    private val mutex = Mutex()
    
    /**
     * 获取缓存的作品
     * @param artworkId 作品ID
     * @return 缓存的作品，如果不存在返回null
     */
    suspend fun getArtwork(artworkId: String): Artwork? {
        return cache.value[artworkId]?.artwork
    }
    
    /**
     * 获取缓存的作品（Flow）
     * @param artworkId 作品ID
     * @return 缓存的作品Flow
     */
    fun getArtworkFlow(artworkId: String): Flow<Artwork?> {
        return cache.map { it[artworkId]?.artwork }
    }
    
    /**
     * 检查作品详情是否已加载
     * 
     * 详情数据包括：
     * - 完整的作品信息（description、tags等）
     * - 多页作品的所有页面URL
     * - 作者头像URL
     * 
     * @param artworkId 作品ID
     * @return 是否已加载详情
     */
    suspend fun hasDetailLoaded(artworkId: String): Boolean {
        return cache.value[artworkId]?.hasDetailLoaded == true
    }
    
    /**
     * 缓存作品（基础数据，来自列表等接口）
     * 
     * 如果已存在详情缓存，不会覆盖
     * 
     * @param artwork 作品数据
     */
    suspend fun cacheArtwork(artwork: Artwork) = mutex.withLock {
        val existing = cache.value[artwork.id]
        // 如果已有详情缓存，不覆盖
        if (existing?.hasDetailLoaded == true) {
            return@withLock
        }
        
        val entry = ArtworkCacheEntry(
            artwork = artwork,
            hasDetailLoaded = false,
            cachedAt = System.currentTimeMillis()
        )
        cache.value = cache.value + (artwork.id to entry)
    }
    
    /**
     * 缓存作品详情
     * 
     * 标记为已加载详情，后续访问不需要再调用详情API
     * 
     * @param artwork 完整的作品详情数据
     */
    suspend fun cacheArtworkDetail(artwork: Artwork) = mutex.withLock {
        val entry = ArtworkCacheEntry(
            artwork = artwork,
            hasDetailLoaded = true,
            cachedAt = System.currentTimeMillis()
        )
        cache.value = cache.value + (artwork.id to entry)
    }
    
    /**
     * 批量缓存作品（基础数据）
     * 
     * @param artworks 作品列表
     */
    suspend fun cacheArtworks(artworks: List<Artwork>) = mutex.withLock {
        val updates = artworks.mapNotNull { artwork ->
            val existing = cache.value[artwork.id]
            // 如果已有详情缓存，不覆盖
            if (existing?.hasDetailLoaded == true) {
                null
            } else {
                artwork.id to ArtworkCacheEntry(
                    artwork = artwork,
                    hasDetailLoaded = false,
                    cachedAt = System.currentTimeMillis()
                )
            }
        }.toMap()
        
        if (updates.isNotEmpty()) {
            cache.value = cache.value + updates
        }
    }
    
    /**
     * 更新缓存中作品的部分字段
     * 
     * 用于状态同步（如收藏状态变更）时更新缓存
     * 
     * @param artworkId 作品ID
     * @param update 更新函数
     */
    suspend fun updateArtwork(artworkId: String, update: (Artwork) -> Artwork) = mutex.withLock {
        val existing = cache.value[artworkId]
        if (existing != null) {
            val updated = existing.copy(
                artwork = update(existing.artwork),
                cachedAt = System.currentTimeMillis()
            )
            cache.value = cache.value + (artworkId to updated)
        }
    }
    
    /**
     * 批量获取缓存的作品
     * 
     * @param artworkIds 作品ID列表
     * @return 缓存的作品Map
     */
    suspend fun getArtworks(artworkIds: List<String>): Map<String, Artwork> {
        return artworkIds.mapNotNull { id ->
            cache.value[id]?.artwork?.let { id to it }
        }.toMap()
    }
    
    /**
     * 获取已加载详情的作品
     * 
     * @param artworkId 作品ID
     * @return 已加载详情的作品，如果未加载详情返回null
     */
    suspend fun getDetailedArtwork(artworkId: String): Artwork? {
        val entry = cache.value[artworkId]
        return if (entry?.hasDetailLoaded == true) entry.artwork else null
    }
    
    /**
     * 移除缓存的作品
     * 
     * @param artworkId 作品ID
     */
    suspend fun removeArtwork(artworkId: String) = mutex.withLock {
        cache.value = cache.value - artworkId
    }
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAll() = mutex.withLock {
        cache.value = emptyMap()
    }
    
    /**
     * 获取缓存统计信息
     */
    fun getCacheStats(): CacheStats {
        val entries = cache.value.values
        return CacheStats(
            totalCount = entries.size,
            detailLoadedCount = entries.count { it.hasDetailLoaded }
        )
    }
    
    /**
     * 缓存统计信息
     */
    data class CacheStats(
        val totalCount: Int,
        val detailLoadedCount: Int
    )
}

/**
 * 作品缓存条目
 * 
 * @param artwork 作品数据
 * @param hasDetailLoaded 是否已加载详情（包括多页数据）
 * @param cachedAt 缓存时间戳
 */
data class ArtworkCacheEntry(
    val artwork: Artwork,
    val hasDetailLoaded: Boolean,
    val cachedAt: Long
)
