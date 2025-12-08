package com.projectu.shared.data.repository

import com.projectu.shared.data.local.entity.toDownloadRule
import com.projectu.shared.data.local.entity.toEntity
import com.projectu.shared.data.local.store.DownloadRulesStore
import com.projectu.shared.domain.model.DownloadRule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 下载规则仓储接口
 */
interface DownloadRulesRepository {
    /**
     * 获取所有规则
     */
    fun getRules(): Flow<List<DownloadRule>>
    
    /**
     * 添加规则
     */
    suspend fun addRule(rule: DownloadRule)
    
    /**
     * 更新规则
     */
    suspend fun updateRule(rule: DownloadRule)
    
    /**
     * 删除规则
     */
    suspend fun deleteRule(ruleId: Long)
    
    /**
     * 批量更新规则顺序
     */
    suspend fun updateRulesOrder(rules: List<DownloadRule>)
    
    /**
     * 启用/禁用规则
     */
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean)
}

/**
 * 下载规则仓储实现
 */
class DownloadRulesRepositoryImpl(
    private val downloadRulesStore: DownloadRulesStore
) : DownloadRulesRepository {
    
    override fun getRules(): Flow<List<DownloadRule>> {
        return downloadRulesStore.rules.map { entities ->
            entities.map { it.toDownloadRule() }
        }
    }
    
    override suspend fun addRule(rule: DownloadRule) {
        val entity = rule.toEntity().copy(
            id = 0, // 让数据库自动生成ID
            createdAt = System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis()
        )
        downloadRulesStore.upsertRule(entity)
    }
    
    override suspend fun updateRule(rule: DownloadRule) {
        val entity = rule.toEntity().copy(
            updatedAt = System.currentTimeMillis()
        )
        downloadRulesStore.upsertRule(entity)
    }
    
    override suspend fun deleteRule(ruleId: Long) {
        downloadRulesStore.deleteRule(ruleId)
    }
    
    override suspend fun updateRulesOrder(rules: List<DownloadRule>) {
        val entities = rules.map { it.toEntity() }
        downloadRulesStore.updateRulesOrder(entities)
    }
    
    override suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        downloadRulesStore.setRuleEnabled(ruleId, enabled)
    }
}
