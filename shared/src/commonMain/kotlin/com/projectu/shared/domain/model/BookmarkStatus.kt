package com.projectu.shared.domain.model

/**
 * 作品收藏状态
 */
enum class BookmarkStatus {
    /**
     * 未收藏
     */
    NOT_BOOKMARKED,
    
    /**
     * 公开收藏
     */
    PUBLIC,
    
    /**
     * 私人收藏
     */
    PRIVATE;
    
    /**
     * 是否已收藏（公开或私人）
     */
    val isBookmarked: Boolean
        get() = this != NOT_BOOKMARKED
}
