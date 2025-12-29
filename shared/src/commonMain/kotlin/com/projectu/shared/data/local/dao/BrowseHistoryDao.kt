package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.BrowseHistoryEntity
import kotlinx.coroutines.flow.Flow

/**
 * 浏览历史数据访问对象
 */
@Dao
interface BrowseHistoryDao {
    
    /**
     * 获取所有浏览历史（按浏览时间倒序）
     */
    @Query("SELECT * FROM browse_history ORDER BY viewedAt DESC")
    fun getAllHistory(): Flow<List<BrowseHistoryEntity>>
    
    /**
     * 根据内容类型获取浏览历史（按浏览时间倒序）
     */
    @Query("SELECT * FROM browse_history WHERE contentType = :contentType ORDER BY viewedAt DESC")
    fun getHistoryByType(contentType: String): Flow<List<BrowseHistoryEntity>>
    
    /**
     * 根据ID获取单条历史记录
     */
    @Query("SELECT * FROM browse_history WHERE id = :id")
    suspend fun getHistoryById(id: String): BrowseHistoryEntity?
    
    /**
     * 插入或更新浏览历史
     * 如果记录已存在（相同ID），会更新viewedAt时间
     */
    @Upsert
    suspend fun upsertHistory(history: BrowseHistoryEntity)
    
    /**
     * 批量插入或更新浏览历史
     */
    @Upsert
    suspend fun upsertHistories(histories: List<BrowseHistoryEntity>)
    
    /**
     * 删除指定ID的浏览历史
     */
    @Query("DELETE FROM browse_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)
    
    /**
     * 删除指定内容类型的所有浏览历史
     */
    @Query("DELETE FROM browse_history WHERE contentType = :contentType")
    suspend fun deleteHistoryByType(contentType: String)
    
    /**
     * 清空所有浏览历史
     */
    @Query("DELETE FROM browse_history")
    suspend fun clearAllHistory()
    
    /**
     * 获取浏览历史总数
     */
    @Query("SELECT COUNT(*) FROM browse_history")
    suspend fun getHistoryCount(): Int
    
    /**
     * 根据内容类型获取历史记录数
     */
    @Query("SELECT COUNT(*) FROM browse_history WHERE contentType = :contentType")
    suspend fun getHistoryCountByType(contentType: String): Int
    
    /**
     * 删除最旧的N条历史记录
     * 用于限制历史记录数量
     */
    @Query("DELETE FROM browse_history WHERE id IN (SELECT id FROM browse_history ORDER BY viewedAt ASC LIMIT :count)")
    suspend fun deleteOldestHistory(count: Int)
}
