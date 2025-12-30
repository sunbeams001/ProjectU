package com.projectu.shared.domain.model

/**
 * 屏蔽规则领域模型
 */
data class BlockRule(
    /** 规则ID（自动生成） */
    val id: String = generateId(),
    
    /** 规则类型 */
    val type: BlockRuleType,
    
    /** 规则值（根据类型不同而不同）
     * - AUTHOR_ID: 用户ID（如 "123456"）
     * - TAG: 标签名（如 "初音ミク"）
     * - R18_CONTENT, AI_GENERATED: 空字符串
     */
    val value: String = "",
    
    /** 规则显示名称（用于UI展示）
     * - AUTHOR_ID: 用户名
     * - TAG: 标签名
     * - R18_CONTENT: "R-18 作品"
     * - AI_GENERATED: "AI 生成作品"
     */
    val displayName: String,
    
    /** Tag 匹配模式（仅对 TAG 类型有效）
     * - EXACT: 精确匹配（默认）
     * - REGEX: 正则表达式匹配
     */
    val matchMode: TagMatchMode = TagMatchMode.EXACT,
    
    /** 是否启用 */
    val enabled: Boolean = true,
    
    /** 适用范围（内容类型） */
    val scopes: Set<ContentScope> = ContentScope.DEFAULT_IMAGE_SCOPES,
    
    /** 创建时间 */
    val createdAt: Long = System.currentTimeMillis(),
    
    /** 更新时间 */
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private fun generateId(): String {
            return System.currentTimeMillis().toString() + (0..9999).random()
        }
        
        /**
         * 创建 R-18 固定规则
         */
        fun createR18Rule(enabled: Boolean = false): BlockRule {
            return BlockRule(
                id = "fixed_r18",
                type = BlockRuleType.R18_CONTENT,
                displayName = "R-18 作品",
                enabled = enabled,
                scopes = ContentScope.ALL_SCOPES  // R-18 默认应用于所有类型
            )
        }
        
        /**
         * 创建 AI 作品固定规则
         */
        fun createAiGeneratedRule(enabled: Boolean = false): BlockRule {
            return BlockRule(
                id = "fixed_ai",
                type = BlockRuleType.AI_GENERATED,
                displayName = "AI 生成作品",
                enabled = enabled,
                scopes = ContentScope.DEFAULT_IMAGE_SCOPES  // AI 默认仅应用于图像
            )
        }
        
        /**
         * 创建作者屏蔽规则
         */
        fun createAuthorRule(
            userId: String, 
            userName: String, 
            enabled: Boolean = true,
            scopes: Set<ContentScope> = ContentScope.ALL_SCOPES
        ): BlockRule {
            return BlockRule(
                type = BlockRuleType.AUTHOR_ID,
                value = userId,
                displayName = userName,
                enabled = enabled,
                scopes = scopes
            )
        }
        
        /**
         * 创建标签屏蔽规则
         */
        fun createTagRule(
            tag: String, 
            enabled: Boolean = true,
            scopes: Set<ContentScope> = ContentScope.DEFAULT_IMAGE_SCOPES,
            matchMode: TagMatchMode = TagMatchMode.EXACT
        ): BlockRule {
            return BlockRule(
                type = BlockRuleType.TAG,
                value = tag,
                displayName = tag,
                matchMode = matchMode,
                enabled = enabled,
                scopes = scopes
            )
        }
    }
}
