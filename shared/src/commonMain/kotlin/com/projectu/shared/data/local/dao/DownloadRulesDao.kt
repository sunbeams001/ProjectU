package com.projectu.shared.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.projectu.shared.data.local.entity.DownloadRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadRulesDao {
    /**
     * 获取所有规则（按优先级排序）
     */
    @Query("SELECT * FROM download_rules ORDER BY ruleOrder ASC")
    fun getAllRules(): Flow<List<DownloadRuleEntity>>
    
    /**
     * 获取所有启用的规则（按优先级排序）
     */
    @Query("SELECT * FROM download_rules WHERE enabled = 1 ORDER BY ruleOrder ASC")
    fun getEnabledRules(): Flow<List<DownloadRuleEntity>>
    
    /**
     * 插入或更新规则
     */
    @Upsert
    suspend fun upsertRule(rule: DownloadRuleEntity)
    
    /**
     * 批量插入或更新规则
     */
    @Upsert
    suspend fun upsertRules(rules: List<DownloadRuleEntity>)
    
    /**
     * 删除规则
     */
    @Delete
    suspend fun deleteRule(rule: DownloadRuleEntity)
    
    /**
     * 根据ID删除规则
     */
    @Query("DELETE FROM download_rules WHERE id = :ruleId")
    suspend fun deleteRuleById(ruleId: Long)
    
    /**
     * 更新规则优先级
     */
    @Query("UPDATE download_rules SET ruleOrder = :newOrder, updatedAt = :timestamp WHERE id = :ruleId")
    suspend fun updateRuleOrder(
        ruleId: Long, 
        newOrder: Int, 
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 启用/禁用规则
     */
    @Query("UPDATE download_rules SET enabled = :enabled, updatedAt = :timestamp WHERE id = :ruleId")
    suspend fun setRuleEnabled(
        ruleId: Long, 
        enabled: Boolean, 
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 获取规则数量
     */
    @Query("SELECT COUNT(*) FROM download_rules")
    suspend fun getRuleCount(): Int
}
