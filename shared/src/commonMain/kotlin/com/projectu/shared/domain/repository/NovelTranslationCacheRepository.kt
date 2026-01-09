package com.projectu.shared.domain.repository

import com.projectu.shared.data.local.dao.NovelTranslationCacheDao
import com.projectu.shared.data.local.entity.NovelTranslationCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext

/**
 * 小说翻译缓存仓库
 * 
 * 管理小说页面的翻译缓存，包括读取、保存、清理等操作
 */
class NovelTranslationCacheRepository(
    private val dao: NovelTranslationCacheDao
) {
    
    /**
     * 获取页面翻译缓存
     * 
     * @param novelId 小说ID
     * @param pageIndex 页面索引（从0开始）
     * @param targetLanguage 目标语言
     * @return 翻译内容，不存在则返回null
     */
    suspend fun getTranslation(
        novelId: String,
        pageIndex: Int,
        targetLanguage: String
    ): String? = withContext(Dispatchers.IO) {
        val cache = dao.getTranslation(novelId, pageIndex, targetLanguage)
        if (cache != null) {
            // 更新最后访问时间
            dao.updateLastAccessedAt(cache.id, System.currentTimeMillis())
            cache.translatedContent
        } else {
            null
        }
    }
    
    /**
     * 获取指定小说的所有翻译缓存（按页码排序）
     * 
     * @param novelId 小说ID
     * @param targetLanguage 目标语言
     * @return 页码 -> 翻译内容的映射
     */
    suspend fun getNovelTranslations(
        novelId: String,
        targetLanguage: String
    ): Map<Int, String> = withContext(Dispatchers.IO) {
        dao.getNovelTranslations(novelId, targetLanguage)
            .associate { it.pageIndex to it.translatedContent }
    }
    
    /**
     * 保存翻译缓存
     * 
     * @param novelId 小说ID
     * @param pageIndex 页面索引（从0开始）
     * @param originalContent 原文内容
     * @param translatedContent 翻译内容
     * @param targetLanguage 目标语言
     * @param engine 翻译引擎
     */
    suspend fun saveTranslation(
        novelId: String,
        pageIndex: Int,
        originalContent: String,
        translatedContent: String,
        targetLanguage: String,
        engine: String
    ) = withContext(Dispatchers.IO) {
        val id = "${novelId}_${pageIndex}_${targetLanguage}"
        val timestamp = System.currentTimeMillis()
        
        val cache = NovelTranslationCacheEntity(
            id = id,
            novelId = novelId,
            pageIndex = pageIndex,
            originalContent = originalContent,
            translatedContent = translatedContent,
            targetLanguage = targetLanguage,
            engine = engine,
            createdAt = timestamp,
            lastAccessedAt = timestamp
        )
        
        dao.insertOrUpdate(cache)
    }
    
    /**
     * 批量保存翻译缓存
     * 
     * @param novelId 小说ID
     * @param translations 页码 -> (原文, 译文) 的映射
     * @param targetLanguage 目标语言
     * @param engine 翻译引擎
     */
    suspend fun saveTranslations(
        novelId: String,
        translations: Map<Int, Pair<String, String>>,
        targetLanguage: String,
        engine: String
    ) = withContext(Dispatchers.IO) {
        val timestamp = System.currentTimeMillis()
        val caches = translations.map { (pageIndex, pair) ->
            val (originalContent, translatedContent) = pair
            val id = "${novelId}_${pageIndex}_${targetLanguage}"
            
            NovelTranslationCacheEntity(
                id = id,
                novelId = novelId,
                pageIndex = pageIndex,
                originalContent = originalContent,
                translatedContent = translatedContent,
                targetLanguage = targetLanguage,
                engine = engine,
                createdAt = timestamp,
                lastAccessedAt = timestamp
            )
        }
        
        dao.insertOrUpdateAll(caches)
    }
    
    /**
     * 清除指定小说的翻译缓存
     * 
     * @param novelId 小说ID
     */
    suspend fun clearNovelCache(novelId: String) = withContext(Dispatchers.IO) {
        dao.deleteNovelCache(novelId)
    }
    
    /**
     * 清除指定页面的翻译缓存
     * 
     * @param novelId 小说ID
     * @param pageIndex 页面索引（从0开始）
     * @param targetLanguage 目标语言
     */
    suspend fun clearPageCache(
        novelId: String,
        pageIndex: Int,
        targetLanguage: String
    ) = withContext(Dispatchers.IO) {
        dao.deletePageCache(novelId, pageIndex, targetLanguage)
    }
    
    /**
     * 清除指定小说指定语言的翻译缓存
     * 
     * @param novelId 小说ID
     * @param targetLanguage 目标语言
     */
    suspend fun clearNovelCacheByLanguage(
        novelId: String,
        targetLanguage: String
    ) = withContext(Dispatchers.IO) {
        dao.deleteNovelCacheByLanguage(novelId, targetLanguage)
    }
    
    /**
     * 清除过期缓存
     * 
     * @param daysThreshold 天数阈值，超过该天数未访问的缓存将被清除
     */
    suspend fun clearExpiredCache(daysThreshold: Int = 30) = withContext(Dispatchers.IO) {
        val thresholdTimestamp = System.currentTimeMillis() - (daysThreshold * 24 * 60 * 60 * 1000L)
        dao.deleteExpiredCache(thresholdTimestamp)
    }
    
    /**
     * 获取缓存大小（字节数）
     * 
     * @return 缓存大小，单位：字节
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        dao.getCacheSizeBytes() ?: 0L
    }
    
    /**
     * 获取缓存条目数量
     * 
     * @return 缓存条目数
     */
    suspend fun getCacheCount(): Int = withContext(Dispatchers.IO) {
        dao.getCacheCount()
    }
    
    /**
     * 清空所有翻译缓存
     */
    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }
}
