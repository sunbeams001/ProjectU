package com.projectu.shared.domain.model

/**
 * 过滤器类型（三态逻辑）
 */
enum class FilterType {
    /**
     * 必须满足条件
     */
    MUST_BE,
    
    /**
     * 必须不满足条件
     */
    MUST_NOT_BE,
    
    /**
     * 不限制，都匹配
     */
    ANY
}
