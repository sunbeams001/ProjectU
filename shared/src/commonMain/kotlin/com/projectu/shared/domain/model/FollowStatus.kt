package com.projectu.shared.domain.model

/**
 * 用户关注状态
 */
enum class FollowStatus {
    /**
     * 未关注
     */
    NOT_FOLLOWING,
    
    /**
     * 公开关注
     */
    PUBLIC,
    
    /**
     * 悄悄关注（私密关注）
     */
    PRIVATE;
    
    /**
     * 是否已关注（公开或私密）
     */
    val isFollowing: Boolean
        get() = this != NOT_FOLLOWING
}
