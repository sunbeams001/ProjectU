package com.projectu.shared.data.cache

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.FollowStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.StateCacheEntry
import com.projectu.shared.domain.model.StateCacheType
import com.projectu.shared.domain.repository.StateCacheRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 全局状态缓存管理器
 * 
 * 提供便捷的API来管理作品、小说、用户的收藏/关注状态，
 * 并通过事件流通知UI更新
 * 
 * @property stateCacheRepository 状态缓存仓储
 */
class StateCacheManager(
    private val stateCacheRepository: StateCacheRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    
    // 状态变更事件流
    private val _stateChangeEvents = MutableSharedFlow<StateCacheEvent>(replay = 0, extraBufferCapacity = 64)
    val stateChangeEvents = _stateChangeEvents.asSharedFlow()
    
    // ==================== 作品相关 ====================
    
    /**
     * 获取作品状态
     */
    fun getArtworkState(artworkId: String): Flow<StateCacheEntry?> {
        return stateCacheRepository.getStateCacheEntry(artworkId, StateCacheType.ARTWORK)
    }
    
    /**
     * 批量获取作品状态
     */
    suspend fun getArtworkStates(artworkIds: List<String>): Map<String, StateCacheEntry> {
        return stateCacheRepository.getArtworkStates(artworkIds)
    }
    
    /**
     * 应用状态到作品列表
     * 从缓存中获取状态并更新作品的bookmarkStatus和bookmarkId字段
     */
    suspend fun applyStatesToArtworks(artworks: List<Artwork>): List<Artwork> {
        if (artworks.isEmpty()) return artworks
        
        val artworkIds = artworks.map { it.id }
        val states = getArtworkStates(artworkIds)
        
        return artworks.map { artwork ->
            val state = states[artwork.id]
            if (state != null) {
                artwork.copy(
                    bookmarkStatus = state.bookmarkStatus,
                    bookmarkId = state.bookmarkId
                )
            } else {
                artwork
            }
        }
    }
    
    /**
     * 更新作品收藏状态
     */
    suspend fun updateArtworkBookmarkStatus(
        artworkId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        stateCacheRepository.updateArtworkBookmarkStatus(artworkId, status, bookmarkId)
        
        // 发送状态变更事件
        _stateChangeEvents.emit(
            StateCacheEvent.ArtworkBookmarkChanged(
                artworkId = artworkId,
                status = status,
                bookmarkId = bookmarkId
            )
        )
    }
    
    /**
     * 收藏作品
     */
    suspend fun bookmarkArtwork(
        artworkId: String,
        isPrivate: Boolean,
        bookmarkId: String
    ) {
        val status = if (isPrivate) BookmarkStatus.PRIVATE else BookmarkStatus.PUBLIC
        updateArtworkBookmarkStatus(artworkId, status, bookmarkId)
    }
    
    /**
     * 取消收藏作品
     */
    suspend fun unbookmarkArtwork(artworkId: String) {
        updateArtworkBookmarkStatus(artworkId, BookmarkStatus.NOT_BOOKMARKED, null)
    }
    
    /**
     * 获取所有已收藏的作品ID列表
     */
    fun getBookmarkedArtworkIds(): Flow<List<String>> {
        return stateCacheRepository.getBookmarkedArtworkIds()
    }
    
    // ==================== 小说相关 ====================
    
    /**
     * 获取小说状态
     */
    fun getNovelState(novelId: String): Flow<StateCacheEntry?> {
        return stateCacheRepository.getStateCacheEntry(novelId, StateCacheType.NOVEL)
    }
    
    /**
     * 批量获取小说状态
     */
    suspend fun getNovelStates(novelIds: List<String>): Map<String, StateCacheEntry> {
        return stateCacheRepository.getNovelStates(novelIds)
    }
    
    /**
     * 应用状态到小说列表
     */
    suspend fun applyStatesToNovels(novels: List<Novel>): List<Novel> {
        if (novels.isEmpty()) return novels
        
        val novelIds = novels.map { it.id }
        val states = getNovelStates(novelIds)
        
        return novels.map { novel ->
            val state = states[novel.id]
            if (state != null) {
                novel.copy(
                    bookmarkStatus = state.bookmarkStatus,
                    bookmarkId = state.bookmarkId
                )
            } else {
                novel
            }
        }
    }
    
    /**
     * 更新小说收藏状态
     */
    suspend fun updateNovelBookmarkStatus(
        novelId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        stateCacheRepository.updateNovelBookmarkStatus(novelId, status, bookmarkId)
        
        // 发送状态变更事件
        _stateChangeEvents.emit(
            StateCacheEvent.NovelBookmarkChanged(
                novelId = novelId,
                status = status,
                bookmarkId = bookmarkId
            )
        )
    }
    
    /**
     * 收藏小说
     */
    suspend fun bookmarkNovel(
        novelId: String,
        isPrivate: Boolean,
        bookmarkId: String
    ) {
        val status = if (isPrivate) BookmarkStatus.PRIVATE else BookmarkStatus.PUBLIC
        updateNovelBookmarkStatus(novelId, status, bookmarkId)
    }
    
    /**
     * 取消收藏小说
     */
    suspend fun unbookmarkNovel(novelId: String) {
        updateNovelBookmarkStatus(novelId, BookmarkStatus.NOT_BOOKMARKED, null)
    }
    
    /**
     * 获取所有已收藏的小说ID列表
     */
    fun getBookmarkedNovelIds(): Flow<List<String>> {
        return stateCacheRepository.getBookmarkedNovelIds()
    }
    
    // ==================== 用户相关 ====================
    
    /**
     * 获取用户状态
     */
    fun getUserState(userId: String): Flow<StateCacheEntry?> {
        return stateCacheRepository.getStateCacheEntry(userId, StateCacheType.USER)
    }
    
    /**
     * 批量获取用户状态
     */
    suspend fun getUserStates(userIds: List<String>): Map<String, StateCacheEntry> {
        return stateCacheRepository.getUserStates(userIds)
    }
    
    /**
     * 更新用户关注状态
     */
    suspend fun updateUserFollowStatus(
        userId: String,
        status: FollowStatus
    ) {
        stateCacheRepository.updateUserFollowStatus(userId, status)
        
        // 发送状态变更事件
        _stateChangeEvents.emit(
            StateCacheEvent.UserFollowChanged(
                userId = userId,
                status = status
            )
        )
    }
    
    /**
     * 关注用户
     */
    suspend fun followUser(
        userId: String,
        isPrivate: Boolean
    ) {
        val status = if (isPrivate) FollowStatus.PRIVATE else FollowStatus.PUBLIC
        updateUserFollowStatus(userId, status)
    }
    
    /**
     * 取关用户
     */
    suspend fun unfollowUser(userId: String) {
        updateUserFollowStatus(userId, FollowStatus.NOT_FOLLOWING)
    }
    
    /**
     * 获取所有已关注的用户ID列表
     */
    fun getFollowedUserIds(): Flow<List<String>> {
        return stateCacheRepository.getFollowedUserIds()
    }
    
    // ==================== 缓存管理 ====================
    
    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() {
        stateCacheRepository.clearAllCache()
        _stateChangeEvents.emit(StateCacheEvent.AllCacheCleared)
    }
    
    /**
     * 清除作品缓存
     */
    suspend fun clearArtworkCache() {
        stateCacheRepository.clearCacheByType(StateCacheType.ARTWORK)
        _stateChangeEvents.emit(StateCacheEvent.ArtworkCacheCleared)
    }
    
    /**
     * 清除小说缓存
     */
    suspend fun clearNovelCache() {
        stateCacheRepository.clearCacheByType(StateCacheType.NOVEL)
        _stateChangeEvents.emit(StateCacheEvent.NovelCacheCleared)
    }
    
    /**
     * 清除用户缓存
     */
    suspend fun clearUserCache() {
        stateCacheRepository.clearCacheByType(StateCacheType.USER)
        _stateChangeEvents.emit(StateCacheEvent.UserCacheCleared)
    }
    
    /**
     * 清除过期缓存（默认30天）
     */
    suspend fun clearExpiredCache(daysOld: Int = 30) {
        stateCacheRepository.clearExpiredCache(daysOld)
        _stateChangeEvents.emit(StateCacheEvent.ExpiredCacheCleared(daysOld))
    }
}

/**
 * 状态缓存事件
 * 用于通知UI状态变更
 */
sealed interface StateCacheEvent {
    /**
     * 作品收藏状态变更
     */
    data class ArtworkBookmarkChanged(
        val artworkId: String,
        val status: BookmarkStatus,
        val bookmarkId: String?
    ) : StateCacheEvent
    
    /**
     * 小说收藏状态变更
     */
    data class NovelBookmarkChanged(
        val novelId: String,
        val status: BookmarkStatus,
        val bookmarkId: String?
    ) : StateCacheEvent
    
    /**
     * 用户关注状态变更
     */
    data class UserFollowChanged(
        val userId: String,
        val status: FollowStatus
    ) : StateCacheEvent
    
    /**
     * 所有缓存已清除
     */
    data object AllCacheCleared : StateCacheEvent
    
    /**
     * 作品缓存已清除
     */
    data object ArtworkCacheCleared : StateCacheEvent
    
    /**
     * 小说缓存已清除
     */
    data object NovelCacheCleared : StateCacheEvent
    
    /**
     * 用户缓存已清除
     */
    data object UserCacheCleared : StateCacheEvent
    
    /**
     * 过期缓存已清除
     */
    data class ExpiredCacheCleared(val daysOld: Int) : StateCacheEvent
}

