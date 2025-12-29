package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.BrowseHistoryItem
import com.projectu.shared.domain.model.HistoryContentType
import kotlinx.coroutines.flow.Flow

/**
 * 浏览历史仓储接口
 */
interface BrowseHistoryRepository {
    
    /**
     * 获取所有浏览历史
     * @return Flow形式的历史记录列表（按浏览时间倒序）
     */
    fun getAllHistory(): Flow<List<BrowseHistoryItem>>
    
    /**
     * 根据内容类型获取浏览历史
     * @param contentType 内容类型
     * @return Flow形式的历史记录列表（按浏览时间倒序）
     */
    fun getHistoryByType(contentType: HistoryContentType): Flow<List<BrowseHistoryItem>>
    
    /**
     * 添加或更新浏览历史
     * 如果已存在相同的记录，则更新浏览时间
     * 
     * @param contentType 内容类型
     * @param contentId 内容ID
     * @param title 标题
     * @param thumbnailUrl 缩略图URL
     * @param authorId 作者ID
     * @param authorName 作者名称
     * @param isR18 是否为R18内容
     * @param isAi 是否为AI作品
     */
    suspend fun addOrUpdateHistory(
        contentType: HistoryContentType,
        contentId: String,
        title: String,
        thumbnailUrl: String?,
        authorId: String?,
        authorName: String?,
        isR18: Boolean,
        isAi: Boolean
    ): Result<Unit>
    
    /**
     * 删除指定ID的浏览历史
     */
    suspend fun deleteHistoryById(id: String): Result<Unit>
    
    /**
     * 删除指定内容类型的所有浏览历史
     */
    suspend fun deleteHistoryByType(contentType: HistoryContentType): Result<Unit>
    
    /**
     * 清空所有浏览历史
     */
    suspend fun clearAllHistory(): Result<Unit>
    
    /**
     * 获取浏览历史总数
     */
    suspend fun getHistoryCount(): Int
    
    /**
     * 根据内容类型获取历史记录数
     */
    suspend fun getHistoryCountByType(contentType: HistoryContentType): Int
}
