package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 浏览历史数据库实体
 * 记录用户浏览过的插画、漫画、动图、小说、小说系列、漫画系列等
 */
@Entity(tableName = "browse_history")
data class BrowseHistoryEntity(
    /**
     * 唯一ID，由内容类型和内容ID组合而成：{type}_{contentId}
     * 例如: ILLUST_12345678, NOVEL_SERIES_87654321
     * 确保同一作品只保留一条记录
     */
    @PrimaryKey
    val id: String,
    
    /**
     * 内容类型
     * ILLUST, MANGA, UGOIRA, NOVEL, NOVEL_SERIES, MANGA_SERIES
     */
    val contentType: String,
    
    /**
     * 内容ID（作品ID或系列ID）
     */
    val contentId: String,
    
    /**
     * 标题
     */
    val title: String,
    
    /**
     * 缩略图URL
     */
    val thumbnailUrl: String?,
    
    /**
     * 作者ID
     */
    val authorId: String?,
    
    /**
     * 作者名称
     */
    val authorName: String?,
    
    /**
     * 是否为R18内容
     */
    val isR18: Boolean,
    
    /**
     * 是否为AI作品
     */
    val isAi: Boolean,
    
    /**
     * 浏览时间戳（毫秒）
     */
    val viewedAt: Long,
    
    /**
     * 创建时间戳（毫秒）
     * 记录首次浏览时间
     */
    val createdAt: Long
)
