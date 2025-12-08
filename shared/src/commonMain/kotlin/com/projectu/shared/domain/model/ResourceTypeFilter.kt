package com.projectu.shared.domain.model

/**
 * 资源类型过滤器
 */
enum class ResourceTypeFilter {
    /**
     * 仅匹配插画
     */
    ILLUSTRATION,
    
    /**
     * 仅匹配漫画
     */
    MANGA,
    
    /**
     * 仅匹配动图
     */
    UGOIRA,
    
    /**
     * 仅匹配小说
     */
    NOVEL,
    
    /**
     * 仅匹配小说系列
     */
    NOVEL_SERIES,
    
    /**
     * 匹配所有类型
     */
    ANY
}
