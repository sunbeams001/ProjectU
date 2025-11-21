package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.common.ZoneConfig
import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * 用户关注列表响应体
 */
@Serializable
data class UserFollowingBody(
    val users: List<FollowingUser>,
    val total: Int,
    val followUserTags: List<String> = emptyList(),
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)

/**
 * 关注用户信息
 */
@Serializable
data class FollowingUser(
    val userId: String,
    val userName: String,
    val profileImageUrl: String,
    val profileImageSmallUrl: String? = null,
    val userComment: String? = null,
    val premium: Boolean = false,
    val following: Boolean = false,
    val followed: Boolean = false,
    val isBlocking: Boolean = false,
    val isMypixiv: Boolean = false,
    val illusts: List<IllustSimple> = emptyList(),
    val novels: List<NovelSimple> = emptyList(),
    val commission: UserCommission? = null
)

/**
 * 用户接稿信息
 */
@Serializable
data class UserCommission(
    val acceptRequest: Boolean = false,
    val isSubscribedReopenNotification: Boolean = false
)

/**
 * 小说简要信息
 */
@Serializable
data class NovelSimple(
    val id: String,
    val title: String,
    val description: String? = null,
    val tags: List<String> = emptyList(),
    val userId: String,
    val userName: String,
    val createDate: String,
    val updateDate: String
)

/**
 * 取消关注用户响应体
 */
@Serializable
data class UnfollowUserResponse(
    @SerialName("user_id")
    val userId: String,
    val type: String
)
