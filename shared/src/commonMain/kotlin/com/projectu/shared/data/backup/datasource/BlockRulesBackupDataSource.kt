package com.projectu.shared.data.backup.datasource

import com.projectu.shared.data.local.dao.BlockRuleDao
import com.projectu.shared.data.local.entity.BlockRuleEntity
import kotlinx.serialization.Serializable

/**
 * 屏蔽列表备份数据源
 * 负责导出和导入屏蔽规则数据
 */
class BlockRulesBackupDataSource(
    private val blockRuleDao: BlockRuleDao
) {
    
    /**
     * 导出屏蔽规则数据
     */
    suspend fun exportData(): BlockRulesBackupData {
        val entities = blockRuleDao.getAll()
        val rules = entities.map { entity ->
            BlockRuleBackupItem(
                id = entity.id,
                type = entity.type,
                value = entity.value,
                displayName = entity.displayName,
                matchMode = entity.matchMode,
                enabled = entity.enabled,
                scopes = entity.scopes,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        return BlockRulesBackupData(
            version = 1,
            rules = rules
        )
    }
    
    /**
     * 导入屏蔽规则数据（合并策略：避免重复）
     */
    suspend fun importData(data: BlockRulesBackupData) {
        val entities = data.rules.map { item ->
            BlockRuleEntity(
                id = item.id,
                type = item.type,
                value = item.value,
                displayName = item.displayName,
                matchMode = item.matchMode,
                enabled = item.enabled,
                scopes = item.scopes,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
        
        // 使用insertAll with REPLACE策略，会覆盖已存在的相同ID的规则
        blockRuleDao.insertAll(entities)
    }
}

/**
 * 屏蔽规则备份数据容器
 */
@Serializable
data class BlockRulesBackupData(
    val version: Int = 1,
    val rules: List<BlockRuleBackupItem>
)

/**
 * 单个屏蔽规则备份项
 */
@Serializable
data class BlockRuleBackupItem(
    val id: String,
    val type: String,
    val value: String,
    val displayName: String,
    val matchMode: String,
    val enabled: Boolean,
    val scopes: String,
    val createdAt: Long,
    val updatedAt: Long
)
