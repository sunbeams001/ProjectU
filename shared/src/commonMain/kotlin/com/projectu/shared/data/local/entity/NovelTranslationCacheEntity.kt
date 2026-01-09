package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 小说翻译缓存实体
 * 
 * 用于存储小说页面的翻译结果，避免重复翻译
 * 
 * @param id 缓存ID，格式：${novelId}_${pageIndex}_${targetLanguage}
 * @param novelId 小说ID
 * @param pageIndex 页面索引（从0开始）
 * @param originalContent 原文内容
 * @param translatedContent 翻译内容
 * @param targetLanguage 目标语言
 * @param engine 翻译引擎
 * @param createdAt 创建时间戳（毫秒）
 * @param lastAccessedAt 最后访问时间戳（毫秒）
 */
@Entity(
    tableName = "novel_translation_cache",
    indices = [
        Index(value = ["novelId"]),
        Index(value = ["createdAt"])
    ]
)
data class NovelTranslationCacheEntity(
    @PrimaryKey
    val id: String,
    val novelId: String,
    val pageIndex: Int,
    val originalContent: String,
    val translatedContent: String,
    val targetLanguage: String,
    val engine: String,
    val createdAt: Long,
    val lastAccessedAt: Long
)
