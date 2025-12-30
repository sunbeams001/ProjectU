package com.projectu.shared.domain.model

/**
 * 内容范围枚举
 * 用于定义屏蔽规则的适用范围
 */
enum class ContentScope {
    /** 插画 */
    ILLUST,
    
    /** 漫画 */
    MANGA,
    
    /** 动图 */
    UGOIRA,
    
    /** 小说 */
    NOVEL,
    
    /** 小说系列 */
    NOVEL_SERIES,
    
    /** 漫画系列 */
    MANGA_SERIES;
    
    companion object {
        /**
         * 默认图像类型范围（插画、漫画、动图）
         */
        val DEFAULT_IMAGE_SCOPES = setOf(ILLUST, MANGA, UGOIRA)
        
        /**
         * 所有范围
         */
        val ALL_SCOPES = entries.toSet()
        
        /**
         * 仅小说相关范围
         */
        val NOVEL_SCOPES = setOf(NOVEL, NOVEL_SERIES)
        
        /**
         * 仅漫画系列范围
         */
        val MANGA_SERIES_SCOPES = setOf(MANGA_SERIES)
    }
}
