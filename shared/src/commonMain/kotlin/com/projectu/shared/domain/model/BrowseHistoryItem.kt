package com.projectu.shared.domain.model

/**
 * 浏览历史内容类型
 */
enum class HistoryContentType(val value: String) {
    ILLUST("ILLUST"),          // 插画
    MANGA("MANGA"),            // 漫画
    UGOIRA("UGOIRA"),          // 动图
    NOVEL("NOVEL"),            // 小说
    NOVEL_SERIES("NOVEL_SERIES"), // 小说系列
    MANGA_SERIES("MANGA_SERIES"); // 漫画系列
    
    companion object {
        fun fromValue(value: String): HistoryContentType {
            return entries.find { it.value == value } ?: ILLUST
        }
    }
}

/**
 * 浏览历史条目
 * Domain层模型
 */
data class BrowseHistoryItem(
    /**
     * 唯一ID
     */
    val id: String,
    
    /**
     * 内容类型
     */
    val contentType: HistoryContentType,
    
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
