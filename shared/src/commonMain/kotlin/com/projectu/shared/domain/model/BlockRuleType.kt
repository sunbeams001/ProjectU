package com.projectu.shared.domain.model

/**
 * 屏蔽规则类型
 */
enum class BlockRuleType {
    /** R-18 作品（固定规则，基于设置的阈值判定） */
    R18_CONTENT,
    
    /** AI 生成作品（固定规则，基于 isAiGenerated 判定） */
    AI_GENERATED,
    
    /** 作者ID屏蔽 */
    AUTHOR_ID,
    
    /** 作品标签屏蔽 */
    TAG;
    
    /**
     * 是否为固定规则（不可删除）
     */
    val isFixed: Boolean
        get() = this == R18_CONTENT || this == AI_GENERATED
}
