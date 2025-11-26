package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 用户关注详情响应体
 * 
 * 用于查询指定用户的关注状态（公开/悄悄关注）
 * 
 * 接口: GET /ajax/following/user/details?user_id={userId}
 */
@Serializable
data class UserFollowDetailBody(
    /**
     * 用户ID
     */
    @SerialName("user_id")
    val userId: Long,
    
    /**
     * 用户名
     */
    @SerialName("user_name")
    val userName: String,
    
    /**
     * 关注类型
     * - "0": 公开关注
     * - "1": 悄悄关注（私密）
     */
    @SerialName("restrict")
    val restrict: String,
    
    /**
     * 标签列表（关注时添加的标签）
     */
    @SerialName("tags")
    val tags: List<String> = emptyList()
)

