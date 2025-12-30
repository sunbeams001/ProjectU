package com.projectu.shared.domain.model

/**
 * Tag 匹配模式
 */
enum class TagMatchMode {
    /**
     * 精确匹配（默认）
     * 例如："初音ミク" 只匹配完全相同的标签
     */
    EXACT,
    
    /**
     * 正则表达式匹配
     * 例如：
     * - "初音.*" 匹配所有以"初音"开头的标签
     * - "(初音|鏡音|巡音)" 匹配多个相关标签
     * 
     * 注意：正则模式性能略低于精确匹配
     */
    REGEX
}
