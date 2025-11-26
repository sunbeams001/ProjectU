package com.projectu.shared.domain.model

/**
 * 全局状态缓存条目的类型
 */
enum class StateCacheType {
    /** 作品（插画/漫画/动图） */
    ARTWORK,
    /** 小说 */
    NOVEL,
    /** 用户 */
    USER
}

/**
 * 全局状态缓存条目
 * 用于存储作品收藏、小说收藏、用户关注等状态
 * 
 * @property id 条目ID（作品ID、小说ID、用户ID）
 * @property type 条目类型
 * @property bookmarkStatus 收藏状态（用于作品和小说）
 * @property bookmarkId 收藏ID（用于取消收藏操作）
 * @property followStatus 关注状态（用于用户）
 * @property lastUpdatedAt 最后更新时间戳
 */
data class StateCacheEntry(
    val id: String,
    val type: StateCacheType,
    val bookmarkStatus: BookmarkStatus = BookmarkStatus.NOT_BOOKMARKED,
    val bookmarkId: String? = null,
    val followStatus: FollowStatus = FollowStatus.NOT_FOLLOWING,
    val lastUpdatedAt: Long = System.currentTimeMillis()
) {
    /**
     * 是否已收藏（用于作品和小说）
     */
    val isBookmarked: Boolean
        get() = bookmarkStatus.isBookmarked
    
    /**
     * 是否已关注（用于用户）
     */
    val isFollowing: Boolean
        get() = followStatus.isFollowing
    
    /**
     * 复制并更新收藏状态
     */
    fun withBookmarkStatus(status: BookmarkStatus, bookmarkId: String?): StateCacheEntry {
        return copy(
            bookmarkStatus = status,
            bookmarkId = bookmarkId,
            lastUpdatedAt = System.currentTimeMillis()
        )
    }
    
    /**
     * 复制并更新关注状态
     */
    fun withFollowStatus(status: FollowStatus): StateCacheEntry {
        return copy(
            followStatus = status,
            lastUpdatedAt = System.currentTimeMillis()
        )
    }
}

