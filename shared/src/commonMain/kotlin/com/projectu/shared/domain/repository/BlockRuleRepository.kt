package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.model.BlockRuleType
import kotlinx.coroutines.flow.Flow

/**
 * 屏蔽规则仓储接口
 */
interface BlockRuleRepository {
    /**
     * 观察所有屏蔽规则
     */
    fun observeAllRules(): Flow<List<BlockRule>>
    
    /**
     * 观察所有启用的屏蔽规则
     */
    fun observeEnabledRules(): Flow<List<BlockRule>>
    
    /**
     * 获取所有屏蔽规则（一次性）
     */
    suspend fun getAllRules(): List<BlockRule>
    
    /**
     * 获取所有启用的屏蔽规则（一次性）
     */
    suspend fun getEnabledRules(): List<BlockRule>
    
    /**
     * 添加屏蔽规则
     */
    suspend fun addRule(rule: BlockRule)
    
    /**
     * 更新屏蔽规则
     */
    suspend fun updateRule(rule: BlockRule)
    
    /**
     * 删除屏蔽规则
     */
    suspend fun deleteRule(ruleId: String)
    
    /**
     * 更新规则启用状态
     */
    suspend fun toggleRuleEnabled(ruleId: String, enabled: Boolean)
    
    /**
     * 检查规则是否已存在
     */
    suspend fun ruleExists(type: BlockRuleType, value: String): Boolean
    
    /**
     * 初始化固定规则（R-18 和 AI，如果不存在）
     */
    suspend fun initializeFixedRules()
}
