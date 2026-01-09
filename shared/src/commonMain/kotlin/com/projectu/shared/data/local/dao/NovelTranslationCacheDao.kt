package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.NovelTranslationCacheEntity
import kotlinx.coroutines.flow.Flow

/**
 * 小说翻译缓存数据访问对象
 */
@Dao
interface NovelTranslationCacheDao {
    
    /**
     * 获取指定页面的翻译缓存
     */
    @Query("""
        SELECT * FROM novel_translation_cache 
        WHERE novelId = :novelId 
        AND pageIndex = :pageIndex 
        AND targetLanguage = :targetLanguage
        LIMIT 1
    """)
    suspend fun getTranslation(
        novelId: String,
        pageIndex: Int,
        targetLanguage: String
    ): NovelTranslationCacheEntity?
    
    /**
     * 获取指定小说的所有翻译缓存
     */
    @Query("""
        SELECT * FROM novel_translation_cache 
        WHERE novelId = :novelId 
        AND targetLanguage = :targetLanguage
        ORDER BY pageIndex ASC
    """)
    suspend fun getNovelTranslations(
        novelId: String,
        targetLanguage: String
    ): List<NovelTranslationCacheEntity>
    
    /**
     * 保存或更新翻译缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(cache: NovelTranslationCacheEntity)
    
    /**
     * 批量保存翻译缓存
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(caches: List<NovelTranslationCacheEntity>)
    
    /**
     * 更新最后访问时间
     */
    @Query("""
        UPDATE novel_translation_cache 
        SET lastAccessedAt = :timestamp 
        WHERE id = :id
    """)
    suspend fun updateLastAccessedAt(id: String, timestamp: Long)
    
    /**
     * 删除指定小说的所有翻译缓存
     */
    @Query("DELETE FROM novel_translation_cache WHERE novelId = :novelId")
    suspend fun deleteNovelCache(novelId: String)
    
    /**
     * 删除指定页面的翻译缓存
     */
    @Query("""
        DELETE FROM novel_translation_cache 
        WHERE novelId = :novelId 
        AND pageIndex = :pageIndex
        AND targetLanguage = :targetLanguage
    """)
    suspend fun deletePageCache(novelId: String, pageIndex: Int, targetLanguage: String)
    
    /**
     * 删除指定小说指定语言的所有翻译缓存
     */
    @Query("""
        DELETE FROM novel_translation_cache 
        WHERE novelId = :novelId 
        AND targetLanguage = :targetLanguage
    """)
    suspend fun deleteNovelCacheByLanguage(novelId: String, targetLanguage: String)
    
    /**
     * 清除过期缓存（超过指定天数未访问）
     */
    @Query("""
        DELETE FROM novel_translation_cache 
        WHERE lastAccessedAt < :thresholdTimestamp
    """)
    suspend fun deleteExpiredCache(thresholdTimestamp: Long)
    
    /**
     * 获取缓存总大小（字节数估算）
     */
    @Query("""
        SELECT SUM(LENGTH(originalContent) + LENGTH(translatedContent)) 
        FROM novel_translation_cache
    """)
    suspend fun getCacheSizeBytes(): Long?
    
    /**
     * 获取缓存条目数量
     */
    @Query("SELECT COUNT(*) FROM novel_translation_cache")
    suspend fun getCacheCount(): Int
    
    /**
     * 清空所有翻译缓存
     */
    @Query("DELETE FROM novel_translation_cache")
    suspend fun clearAll()
}
