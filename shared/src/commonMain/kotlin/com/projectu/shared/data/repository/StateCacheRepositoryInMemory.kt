package com.projectu.shared.data.repository

import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.StateCacheEntry
import com.projectu.shared.domain.model.StateCacheType
import com.projectu.shared.domain.repository.StateCacheRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 全局状态缓存仓储实现（纯内存版本）
 * 
 * 使用内存Map进行缓存，不持久化到数据库
 * 通过Flow提供响应式数据更新
 */
class StateCacheRepositoryInMemory : StateCacheRepository {
    
    // 使用复合Key (id_type) 存储状态
    private val cache = MutableStateFlow<Map<String, StateCacheEntry>>(emptyMap())
    private val mutex = Mutex()
    
    /**
     * 生成缓存Key
     */
    private fun cacheKey(id: String, type: StateCacheType): String {
        return "${id}_${type.name}"
    }
    
    // ==================== 查询操作 ====================
    
    override fun getStateCacheEntry(id: String, type: StateCacheType): Flow<StateCacheEntry?> {
        val key = cacheKey(id, type)
        return cache.map { it[key] }
    }
    
    override fun getBookmarkedArtworkIds(): Flow<List<String>> {
        return cache.map { cacheMap ->
            cacheMap.values
                .filter { it.type == StateCacheType.ARTWORK && it.isBookmarked }
                .map { it.id }
                .sortedByDescending { cacheMap[cacheKey(it, StateCacheType.ARTWORK)]?.lastUpdatedAt ?: 0 }
        }
    }
    
    override fun getBookmarkedNovelIds(): Flow<List<String>> {
        return cache.map { cacheMap ->
            cacheMap.values
                .filter { it.type == StateCacheType.NOVEL && it.isBookmarked }
                .map { it.id }
                .sortedByDescending { cacheMap[cacheKey(it, StateCacheType.NOVEL)]?.lastUpdatedAt ?: 0 }
        }
    }
    
    override fun getFollowedUserIds(): Flow<List<String>> {
        return cache.map { cacheMap ->
            cacheMap.values
                .filter { it.type == StateCacheType.USER && it.isFollowing }
                .map { it.id }
                .sortedByDescending { cacheMap[cacheKey(it, StateCacheType.USER)]?.lastUpdatedAt ?: 0 }
        }
    }
    
    override suspend fun getArtworkStates(artworkIds: List<String>): Map<String, StateCacheEntry> {
        if (artworkIds.isEmpty()) return emptyMap()
        
        val currentCache = cache.value
        return artworkIds.mapNotNull { id ->
            val entry = currentCache[cacheKey(id, StateCacheType.ARTWORK)]
            entry?.let { id to it }
        }.toMap()
    }
    
    override suspend fun getNovelStates(novelIds: List<String>): Map<String, StateCacheEntry> {
        if (novelIds.isEmpty()) return emptyMap()
        
        val currentCache = cache.value
        return novelIds.mapNotNull { id ->
            val entry = currentCache[cacheKey(id, StateCacheType.NOVEL)]
            entry?.let { id to it }
        }.toMap()
    }
    
    override suspend fun getUserStates(userIds: List<String>): Map<String, StateCacheEntry> {
        if (userIds.isEmpty()) return emptyMap()
        
        val currentCache = cache.value
        return userIds.mapNotNull { id ->
            val entry = currentCache[cacheKey(id, StateCacheType.USER)]
            entry?.let { id to it }
        }.toMap()
    }
    
    // ==================== 更新操作 ====================
    
    override suspend fun updateArtworkBookmarkStatus(
        artworkId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) = mutex.withLock {
        val key = cacheKey(artworkId, StateCacheType.ARTWORK)
        val existing = cache.value[key]
        val now = System.currentTimeMillis()
        
        val entry = existing?.copy(
            bookmarkStatus = status,
            bookmarkId = bookmarkId,
            lastUpdatedAt = now
        ) ?: StateCacheEntry(
            id = artworkId,
            type = StateCacheType.ARTWORK,
            bookmarkStatus = status,
            bookmarkId = bookmarkId,
            lastUpdatedAt = now
        )
        
        cache.value = cache.value + (key to entry)
    }
    
    override suspend fun updateNovelBookmarkStatus(
        novelId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) = mutex.withLock {
        val key = cacheKey(novelId, StateCacheType.NOVEL)
        val existing = cache.value[key]
        val now = System.currentTimeMillis()
        
        val entry = existing?.copy(
            bookmarkStatus = status,
            bookmarkId = bookmarkId,
            lastUpdatedAt = now
        ) ?: StateCacheEntry(
            id = novelId,
            type = StateCacheType.NOVEL,
            bookmarkStatus = status,
            bookmarkId = bookmarkId,
            lastUpdatedAt = now
        )
        
        cache.value = cache.value + (key to entry)
    }
    
    override suspend fun updateUserFollowStatus(
        userId: String,
        status: FollowStatus
    ) = mutex.withLock {
        val key = cacheKey(userId, StateCacheType.USER)
        val existing = cache.value[key]
        val now = System.currentTimeMillis()
        
        val entry = existing?.copy(
            followStatus = status,
            lastUpdatedAt = now
        ) ?: StateCacheEntry(
            id = userId,
            type = StateCacheType.USER,
            followStatus = status,
            lastUpdatedAt = now
        )
        
        cache.value = cache.value + (key to entry)
    }
    
    override suspend fun updateStateCacheEntries(entries: List<StateCacheEntry>) = mutex.withLock {
        if (entries.isEmpty()) return@withLock
        
        val updates = entries.associate { entry ->
            cacheKey(entry.id, entry.type) to entry
        }
        
        cache.value = cache.value + updates
    }
    
    // ==================== 删除操作 ====================
    
    override suspend fun deleteStateCacheEntry(id: String, type: StateCacheType) = mutex.withLock {
        val key = cacheKey(id, type)
        cache.value = cache.value - key
    }
    
    override suspend fun clearAllCache() = mutex.withLock {
        cache.value = emptyMap()
    }
    
    override suspend fun clearCacheByType(type: StateCacheType) = mutex.withLock {
        cache.value = cache.value.filterValues { it.type != type }
    }
    
    override suspend fun clearExpiredCache(daysOld: Int) = mutex.withLock {
        val thresholdTimestamp = System.currentTimeMillis() - (daysOld * 24 * 60 * 60 * 1000L)
        cache.value = cache.value.filterValues { it.lastUpdatedAt >= thresholdTimestamp }
    }
}

