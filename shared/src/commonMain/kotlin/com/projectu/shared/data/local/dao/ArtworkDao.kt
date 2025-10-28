package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.ArtworkEntity
import com.projectu.shared.data.local.entity.UgoiraCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * 作品数据访问对象
 * 定义作品相关的数据库操作
 */
@Dao
interface ArtworkDao {
    
    /**
     * 获取所有作品
     */
    @Query("SELECT * FROM artworks ORDER BY cachedAt DESC")
    fun getAllArtworks(): Flow<List<ArtworkEntity>>
    
    /**
     * 根据ID获取作品
     */
    @Query("SELECT * FROM artworks WHERE id = :id")
    suspend fun getArtworkById(id: String): ArtworkEntity?
    
    /**
     * 插入或更新作品
     */
    @Upsert
    suspend fun upsertArtwork(artwork: ArtworkEntity)
    
    /**
     * 批量插入或更新作品
     */
    @Upsert
    suspend fun upsertArtworks(artworks: List<ArtworkEntity>)
    
    /**
     * 删除作品
     */
    @Query("DELETE FROM artworks WHERE id = :id")
    suspend fun deleteArtwork(id: String)
    
    /**
     * 清空所有作品
     */
    @Query("DELETE FROM artworks")
    suspend fun deleteAllArtworks()
    
    /**
     * 更新作品收藏状态
     */
    @Query("UPDATE artworks SET isBookmarked = :isBookmarked WHERE id = :id")
    suspend fun updateBookmarkStatus(id: String, isBookmarked: Boolean)
    
    /**
     * 获取收藏的作品
     */
    @Query("SELECT * FROM artworks WHERE isBookmarked = 1 ORDER BY cachedAt DESC")
    fun getBookmarkedArtworks(): Flow<List<ArtworkEntity>>
}

/**
 * Ugoira缓存数据访问对象
 */
@Dao
interface UgoiraCacheDao {
    
    /**
     * 获取Ugoira缓存
     */
    @Query("SELECT * FROM ugoira_cache WHERE artworkId = :artworkId")
    suspend fun getUgoiraCache(artworkId: String): UgoiraCacheEntity?
    
    /**
     * 插入或更新Ugoira缓存
     */
    @Upsert
    suspend fun upsertUgoiraCache(cache: UgoiraCacheEntity)
    
    /**
     * 删除Ugoira缓存
     */
    @Query("DELETE FROM ugoira_cache WHERE artworkId = :artworkId")
    suspend fun deleteUgoiraCache(artworkId: String)
    
    /**
     * 更新最后访问时间
     */
    @Query("UPDATE ugoira_cache SET lastAccessedAt = :timestamp WHERE artworkId = :artworkId")
    suspend fun updateLastAccessedAt(artworkId: String, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 清理过期缓存
     */
    @Query("DELETE FROM ugoira_cache WHERE lastAccessedAt < :expiredBefore")
    suspend fun cleanExpiredCache(expiredBefore: Long)
}
