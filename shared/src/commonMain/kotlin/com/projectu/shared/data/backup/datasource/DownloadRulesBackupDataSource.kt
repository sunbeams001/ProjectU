package com.projectu.shared.data.backup.datasource

import com.projectu.shared.data.local.dao.DownloadRulesDao
import com.projectu.shared.data.local.entity.DownloadRuleEntity
import kotlinx.serialization.Serializable

/**
 * 下载路径规则备份数据源
 * 负责导出和导入下载路径规则数据
 */
class DownloadRulesBackupDataSource(
    private val downloadRulesDao: DownloadRulesDao
) {
    
    /**
     * 导出下载规则数据
     */
    suspend fun exportData(): DownloadRulesBackupData {
        val entities = downloadRulesDao.getAllRulesList()
        val rules = entities.map { entity ->
            DownloadRuleBackupItem(
                id = entity.id,
                ruleOrder = entity.ruleOrder,
                resourceTypes = entity.resourceTypes,
                r18Filter = entity.r18Filter,
                aiFilter = entity.aiFilter,
                authorGrouping = entity.authorGrouping,
                targetPath = entity.targetPath,
                subDirectory = entity.subDirectory,
                enabled = entity.enabled,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt
            )
        }
        return DownloadRulesBackupData(
            version = 1,
            rules = rules
        )
    }
    
    /**
     * 导入下载规则数据（合并策略：避免重复）
     * 注意：恢复时需要注意路径可能需要用户重新配置
     */
    suspend fun importData(data: DownloadRulesBackupData) {
        val entities = data.rules.map { item ->
            DownloadRuleEntity(
                id = item.id,
                ruleOrder = item.ruleOrder,
                resourceTypes = item.resourceTypes,
                r18Filter = item.r18Filter,
                aiFilter = item.aiFilter,
                authorGrouping = item.authorGrouping,
                targetPath = item.targetPath,
                subDirectory = item.subDirectory,
                enabled = item.enabled,
                createdAt = item.createdAt,
                updatedAt = item.updatedAt
            )
        }
        
        // 使用upsertRules，会自动处理冲突
        downloadRulesDao.upsertRules(entities)
    }
}

/**
 * 下载规则备份数据容器
 */
@Serializable
data class DownloadRulesBackupData(
    val version: Int = 1,
    val rules: List<DownloadRuleBackupItem>
)

/**
 * 单个下载规则备份项
 */
@Serializable
data class DownloadRuleBackupItem(
    val id: Long,
    val ruleOrder: Int,
    val resourceTypes: String,
    val r18Filter: String,
    val aiFilter: String,
    val authorGrouping: String,
    val targetPath: String,
    val subDirectory: String,
    val enabled: Boolean,
    val createdAt: Long,
    val updatedAt: Long
)
