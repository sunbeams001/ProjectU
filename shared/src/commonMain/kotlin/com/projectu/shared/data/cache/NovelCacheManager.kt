package com.projectu.shared.data.cache

import com.projectu.shared.domain.model.Novel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 全局小说缓存管理器
 * 
 * 用于缓存已加载过详情的小说数据，避免重复调用API。
 * 
 * 核心特性：
 * - 纯内存缓存，高性能访问
 * - 区分"基础数据"和"详情数据"
 * - 支持批量操作和Flow响应式更新
 * - 线程安全
 * 
 * 缓存策略：
 * - 小说详情（包括正文内容）一旦加载成功就缓存
 * - 缓存会随应用生命周期存在，应用重启后清空
 * - 可通过 hasDetailLoaded 判断是否需要调用详情API
 */
class NovelCacheManager {
    
    // 小说缓存：novelId -> Novel
    private val cache = MutableStateFlow<Map<String, NovelCacheEntry>>(emptyMap())
    private val mutex = Mutex()
    
    /**
     * 获取缓存的小说
     * @param novelId 小说ID
     * @return 缓存的小说，如果不存在返回null
     */
    suspend fun getNovel(novelId: String): Novel? {
        return cache.value[novelId]?.novel
    }
    
    /**
     * 获取缓存的小说（Flow）
     * @param novelId 小说ID
     * @return 缓存的小说Flow
     */
    fun getNovelFlow(novelId: String): Flow<Novel?> {
        return cache.map { it[novelId]?.novel }
    }
    
    /**
     * 检查小说详情是否已加载
     * 
     * 详情数据包括：
     * - 完整的小说信息（description、tags等）
     * - 小说正文内容（content）
     * - 内嵌图片信息
     * 
     * @param novelId 小说ID
     * @return 是否已加载详情
     */
    suspend fun hasDetailLoaded(novelId: String): Boolean {
        return cache.value[novelId]?.hasDetailLoaded == true
    }
    
    /**
     * 缓存小说（基础数据，来自列表等接口）
     * 
     * 如果已存在详情缓存，不会覆盖
     * 
     * @param novel 小说数据
     */
    suspend fun cacheNovel(novel: Novel) = mutex.withLock {
        val existing = cache.value[novel.id]
        // 如果已有详情缓存，不覆盖
        if (existing?.hasDetailLoaded == true) {
            return@withLock
        }
        
        val entry = NovelCacheEntry(
            novel = novel,
            hasDetailLoaded = false,
            cachedAt = System.currentTimeMillis()
        )
        cache.value = cache.value + (novel.id to entry)
    }
    
    /**
     * 缓存小说详情
     * 
     * 标记为已加载详情，后续访问不需要再调用详情API
     * 
     * @param novel 完整的小说详情数据
     */
    suspend fun cacheNovelDetail(novel: Novel) = mutex.withLock {
        val entry = NovelCacheEntry(
            novel = novel,
            hasDetailLoaded = true,
            cachedAt = System.currentTimeMillis()
        )
        cache.value = cache.value + (novel.id to entry)
    }
    
    /**
     * 批量缓存小说（基础数据）
     * 
     * @param novels 小说列表
     */
    suspend fun cacheNovels(novels: List<Novel>) = mutex.withLock {
        val updates = novels.mapNotNull { novel ->
            val existing = cache.value[novel.id]
            // 如果已有详情缓存，不覆盖
            if (existing?.hasDetailLoaded == true) {
                null
            } else {
                novel.id to NovelCacheEntry(
                    novel = novel,
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
     * 更新缓存中小说的部分字段
     * 
     * 用于状态同步（如收藏状态变更）时更新缓存
     * 
     * @param novelId 小说ID
     * @param update 更新函数
     */
    suspend fun updateNovel(novelId: String, update: (Novel) -> Novel) = mutex.withLock {
        val existing = cache.value[novelId]
        if (existing != null) {
            val updated = existing.copy(
                novel = update(existing.novel),
                cachedAt = System.currentTimeMillis()
            )
            cache.value = cache.value + (novelId to updated)
        }
    }
    
    /**
     * 批量获取缓存的小说
     * 
     * @param novelIds 小说ID列表
     * @return 缓存的小说Map
     */
    suspend fun getNovels(novelIds: List<String>): Map<String, Novel> {
        return novelIds.mapNotNull { id ->
            cache.value[id]?.novel?.let { id to it }
        }.toMap()
    }
    
    /**
     * 获取已加载详情的小说
     * 
     * @param novelId 小说ID
     * @return 已加载详情的小说，如果未加载详情返回null
     */
    suspend fun getDetailedNovel(novelId: String): Novel? {
        val entry = cache.value[novelId]
        return if (entry?.hasDetailLoaded == true) entry.novel else null
    }
    
    /**
     * 移除缓存的小说
     * 
     * @param novelId 小说ID
     */
    suspend fun removeNovel(novelId: String) = mutex.withLock {
        cache.value = cache.value - novelId
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
 * 小说缓存条目
 * 
 * @param novel 小说数据
 * @param hasDetailLoaded 是否已加载详情（包括正文内容）
 * @param cachedAt 缓存时间戳
 */
data class NovelCacheEntry(
    val novel: Novel,
    val hasDetailLoaded: Boolean,
    val cachedAt: Long
)
