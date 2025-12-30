package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.model.BlockRuleType
import com.projectu.shared.domain.model.ContentScope
import com.projectu.shared.domain.model.TagMatchMode

/**
 * 屏蔽规则数据库实体
 */
@Entity(tableName = "block_rules")
data class BlockRuleEntity(
    @PrimaryKey
    val id: String,
    val type: String,  // BlockRuleType.name
    val value: String,
    val displayName: String,
    val matchMode: String,  // TagMatchMode.name (仅对 TAG 类型有效)
    val enabled: Boolean,
    val scopes: String,  // ContentScope 列表，逗号分隔
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * 转换为领域模型
     */
    fun toBlockRule(): BlockRule {
        val scopeSet = if (scopes.isBlank()) {
            ContentScope.DEFAULT_IMAGE_SCOPES
        } else {
            scopes.split(",")
                .mapNotNull { scopeName ->
                    try {
                        ContentScope.valueOf(scopeName.trim())
                    } catch (e: IllegalArgumentException) {
                        null
                    }
                }
                .toSet()
                .ifEmpty { ContentScope.DEFAULT_IMAGE_SCOPES }
        }
        
        return BlockRule(
            id = id,
            type = BlockRuleType.valueOf(type),
            value = value,
            displayName = displayName,
            matchMode = try {
                TagMatchMode.valueOf(matchMode)
            } catch (e: IllegalArgumentException) {
                TagMatchMode.EXACT  // 默认使用精确匹配
            },
            enabled = enabled,
            scopes = scopeSet,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    companion object {
        /**
         * 从领域模型创建
         */
        fun from(rule: BlockRule): BlockRuleEntity {
            return BlockRuleEntity(
                id = rule.id,
                type = rule.type.name,
                value = rule.value,
                displayName = rule.displayName,
                matchMode = rule.matchMode.name,
                enabled = rule.enabled,
                scopes = rule.scopes.joinToString(",") { it.name },
                createdAt = rule.createdAt,
                updatedAt = rule.updatedAt
            )
        }
    }
}
