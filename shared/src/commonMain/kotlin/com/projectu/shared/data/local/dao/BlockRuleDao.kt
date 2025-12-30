package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.BlockRuleEntity
import kotlinx.coroutines.flow.Flow

/**
 * 屏蔽规则数据访问对象
 */
@Dao
interface BlockRuleDao {
    /**
     * 获取所有屏蔽规则（响应式）
     */
    @Query("SELECT * FROM block_rules ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<BlockRuleEntity>>
    
    /**
     * 获取所有启用的屏蔽规则（响应式）
     */
    @Query("SELECT * FROM block_rules WHERE enabled = 1")
    fun observeEnabled(): Flow<List<BlockRuleEntity>>
    
    /**
     * 获取所有屏蔽规则（一次性）
     */
    @Query("SELECT * FROM block_rules ORDER BY createdAt DESC")
    suspend fun getAll(): List<BlockRuleEntity>
    
    /**
     * 根据ID获取规则
     */
    @Query("SELECT * FROM block_rules WHERE id = :id")
    suspend fun getById(id: String): BlockRuleEntity?
    
    /**
     * 插入规则
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rule: BlockRuleEntity)
    
    /**
     * 批量插入规则
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rules: List<BlockRuleEntity>)
    
    /**
     * 更新规则
     */
    @Update
    suspend fun update(rule: BlockRuleEntity)
    
    /**
     * 删除规则
     */
    @Delete
    suspend fun delete(rule: BlockRuleEntity)
    
    /**
     * 根据ID删除
     */
    @Query("DELETE FROM block_rules WHERE id = :id")
    suspend fun deleteById(id: String)
    
    /**
     * 更新规则启用状态
     */
    @Query("UPDATE block_rules SET enabled = :enabled, updatedAt = :timestamp WHERE id = :id")
    suspend fun updateEnabled(id: String, enabled: Boolean, timestamp: Long = System.currentTimeMillis())
    
    /**
     * 检查是否存在指定类型和值的规则
     */
    @Query("SELECT COUNT(*) FROM block_rules WHERE type = :type AND value = :value")
    suspend fun exists(type: String, value: String): Int
    
    /**
     * 清空所有规则（危险操作，慎用）
     */
    @Query("DELETE FROM block_rules")
    suspend fun deleteAll()
}
