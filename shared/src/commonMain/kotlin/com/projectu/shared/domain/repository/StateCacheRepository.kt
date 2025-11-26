package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.StateCacheEntry
import com.projectu.shared.domain.model.StateCacheType
import kotlinx.coroutines.flow.Flow

/**
 * 全局状态缓存仓储接口
 * 
 * 负责管理作品、小说、用户的收藏/关注状态，
 * 保证跨页面、跨组件的状态一致性
 */
interface StateCacheRepository {
    
    // ==================== 查询操作 ====================
    
    /**
     * 获取状态缓存条目
     * @param id 条目ID
     * @param type 条目类型
     * @return Flow包装的状态缓存条目，如果不存在则返回null
     */
    fun getStateCacheEntry(id: String, type: StateCacheType): Flow<StateCacheEntry?>
    
    /**
     * 获取所有已收藏的作品ID列表
     */
    fun getBookmarkedArtworkIds(): Flow<List<String>>
    
    /**
     * 获取所有已收藏的小说ID列表
     */
    fun getBookmarkedNovelIds(): Flow<List<String>>
    
    /**
     * 获取所有已关注的用户ID列表
     */
    fun getFollowedUserIds(): Flow<List<String>>
    
    /**
     * 批量获取作品状态
     * @param artworkIds 作品ID列表
     * @return Map<作品ID, 状态缓存条目>
     */
    suspend fun getArtworkStates(artworkIds: List<String>): Map<String, StateCacheEntry>
    
    /**
     * 批量获取小说状态
     * @param novelIds 小说ID列表
     * @return Map<小说ID, 状态缓存条目>
     */
    suspend fun getNovelStates(novelIds: List<String>): Map<String, StateCacheEntry>
    
    /**
     * 批量获取用户状态
     * @param userIds 用户ID列表
     * @return Map<用户ID, 状态缓存条目>
     */
    suspend fun getUserStates(userIds: List<String>): Map<String, StateCacheEntry>
    
    // ==================== 更新操作 ====================
    
    /**
     * 更新作品收藏状态
     * @param artworkId 作品ID
     * @param status 收藏状态
     * @param bookmarkId 收藏ID（用于取消收藏）
     */
    suspend fun updateArtworkBookmarkStatus(
        artworkId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    )
    
    /**
     * 更新小说收藏状态
     * @param novelId 小说ID
     * @param status 收藏状态
     * @param bookmarkId 收藏ID（用于取消收藏）
     */
    suspend fun updateNovelBookmarkStatus(
        novelId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    )
    
    /**
     * 更新用户关注状态
     * @param userId 用户ID
     * @param status 关注状态
     */
    suspend fun updateUserFollowStatus(
        userId: String,
        status: FollowStatus
    )
    
    /**
     * 批量更新状态缓存
     * @param entries 状态缓存条目列表
     */
    suspend fun updateStateCacheEntries(entries: List<StateCacheEntry>)
    
    // ==================== 删除操作 ====================
    
    /**
     * 删除状态缓存条目
     * @param id 条目ID
     * @param type 条目类型
     */
    suspend fun deleteStateCacheEntry(id: String, type: StateCacheType)
    
    /**
     * 清除所有状态缓存
     */
    suspend fun clearAllCache()
    
    /**
     * 清除指定类型的状态缓存
     * @param type 条目类型
     */
    suspend fun clearCacheByType(type: StateCacheType)
    
    /**
     * 清除过期的状态缓存（超过指定天数）
     * @param daysOld 天数，默认30天
     */
    suspend fun clearExpiredCache(daysOld: Int = 30)
}

