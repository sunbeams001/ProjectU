package com.projectu.shared.data.local.store

import com.projectu.shared.data.local.dao.DownloadRulesDao
import com.projectu.shared.data.local.entity.DownloadRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * 下载规则存储层
 */
class DownloadRulesStore(
    private val downloadRulesDao: DownloadRulesDao
) {
    /**
     * 规则列表（响应式）
     */
    val rules: Flow<List<DownloadRuleEntity>> = downloadRulesDao.getEnabledRules()
    
    /**
     * 添加或更新规则
     */
    suspend fun upsertRule(rule: DownloadRuleEntity) {
        downloadRulesDao.upsertRule(rule)
    }
    
    /**
     * 删除规则
     */
    suspend fun deleteRule(ruleId: Long) {
        downloadRulesDao.deleteRuleById(ruleId)
    }
    
    /**
     * 批量更新规则顺序（用于拖拽排序）
     */
    suspend fun updateRulesOrder(rules: List<DownloadRuleEntity>) {
        val updatedRules = rules.mapIndexed { index, rule ->
            rule.copy(
                ruleOrder = index,
                updatedAt = System.currentTimeMillis()
            )
        }
        downloadRulesDao.upsertRules(updatedRules)
    }
    
    /**
     * 启用/禁用规则
     */
    suspend fun setRuleEnabled(ruleId: Long, enabled: Boolean) {
        downloadRulesDao.setRuleEnabled(ruleId, enabled)
    }
    
    /**
     * 获取规则数量
     */
    suspend fun getRuleCount(): Int {
        return downloadRulesDao.getRuleCount()
    }
}
