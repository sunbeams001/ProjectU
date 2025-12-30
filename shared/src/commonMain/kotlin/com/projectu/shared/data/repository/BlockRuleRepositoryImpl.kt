package com.projectu.shared.data.repository

import com.projectu.shared.data.local.dao.BlockRuleDao
import com.projectu.shared.data.local.entity.BlockRuleEntity
import com.projectu.shared.domain.model.BlockRule
import com.projectu.shared.domain.model.BlockRuleType
import com.projectu.shared.domain.repository.BlockRuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * 屏蔽规则仓储实现
 */
class BlockRuleRepositoryImpl(
    private val blockRuleDao: BlockRuleDao
) : BlockRuleRepository {
    
    override fun observeAllRules(): Flow<List<BlockRule>> {
        return blockRuleDao.observeAll().map { entities ->
            entities.map { it.toBlockRule() }
        }
    }
    
    override fun observeEnabledRules(): Flow<List<BlockRule>> {
        return blockRuleDao.observeEnabled().map { entities ->
            entities.map { it.toBlockRule() }
        }
    }
    
    override suspend fun getAllRules(): List<BlockRule> {
        return blockRuleDao.getAll().map { it.toBlockRule() }
    }
    
    override suspend fun getEnabledRules(): List<BlockRule> {
        return blockRuleDao.observeEnabled().map { entities ->
            entities.map { it.toBlockRule() }
        }.first()
    }
    
    override suspend fun addRule(rule: BlockRule) {
        blockRuleDao.insert(BlockRuleEntity.from(rule))
    }
    
    override suspend fun updateRule(rule: BlockRule) {
        val updatedRule = rule.copy(updatedAt = System.currentTimeMillis())
        blockRuleDao.update(BlockRuleEntity.from(updatedRule))
    }
    
    override suspend fun deleteRule(ruleId: String) {
        blockRuleDao.deleteById(ruleId)
    }
    
    override suspend fun toggleRuleEnabled(ruleId: String, enabled: Boolean) {
        blockRuleDao.updateEnabled(ruleId, enabled)
    }
    
    override suspend fun ruleExists(type: BlockRuleType, value: String): Boolean {
        return blockRuleDao.exists(type.name, value) > 0
    }
    
    override suspend fun initializeFixedRules() {
        // 检查 R-18 规则是否存在
        if (!ruleExists(BlockRuleType.R18_CONTENT, "")) {
            addRule(BlockRule.createR18Rule(enabled = false))
        }
        
        // 检查 AI 规则是否存在
        if (!ruleExists(BlockRuleType.AI_GENERATED, "")) {
            addRule(BlockRule.createAiGeneratedRule(enabled = false))
        }
    }
}
