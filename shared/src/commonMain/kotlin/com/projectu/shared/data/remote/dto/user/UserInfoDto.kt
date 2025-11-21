package com.projectu.shared.data.remote.dto.user

import kotlinx.serialization.Serializable

/**
 * 用户信息响应体
 */
@Serializable
data class UserInfoBody(
    val userId: String,  // 修改为 String 类型
    val name: String,
    val image: String,
    val imageBig: String,
    val premium: Boolean = false,
    val isFollowed: Boolean = false,
    val isMypixiv: Boolean = false,
    val isBlocking: Boolean = false,
    val background: Background? = null,
    val sketchLiveId: String? = null,
    val partial: Int = 0,
    val acceptRequest: Boolean = false,
    val sketchLives: List<SketchLive>? = null,
    val commission: Commission? = null,
    val following: Int = 0,  // 关注数量
    val mypixivCount: Int = 0,  // 好P友数量
    val followedBack: Boolean = false,  // 是否被关注回
    val comment: String? = null,  // 用户简介（纯文本）
    val commentHtml: String? = null,  // 用户简介（HTML）
    val webpage: String? = null,  // 个人网站
    val social: SocialLinks? = null,  // 社交媒体链接
    val canSendMessage: Boolean = false,  // 是否可发送消息
    val region: UserRegion? = null,  // 地区信息
    val age: PrivacyField? = null,  // 年龄信息
    val birthDay: PrivacyField? = null,  // 生日信息
    val gender: PrivacyField? = null,  // 性别信息
    val job: PrivacyField? = null,  // 职业信息
    val workspace: UserWorkspace? = null,  // 工作环境
    val official: Boolean = false,  // 是否官方账号
    val group: List<UserGroup>? = null  // 加入的群组
)

@Serializable
data class Background(
    val repeat: String? = null,
    val color: String? = null,
    val url: String? = null,
    val isPrivate: Boolean = false
)

@Serializable
data class SketchLive(
    val id: String? = null,
    val name: String? = null
)

@Serializable
data class Commission(
    val requestStatus: String? = null,
    val fanRequestStatus: String? = null
)

@Serializable
data class SocialLinks(
    val twitter: SocialLink? = null,
    val pawoo: SocialLink? = null,
    val instagram: SocialLink? = null,
    val tumblr: SocialLink? = null
)

@Serializable
data class SocialLink(
    val url: String
)

@Serializable
data class UserRegion(
    val name: String? = null,
    val region: String? = null,
    val prefecture: String? = null,
    val privacyLevel: String? = null
)

@Serializable
data class PrivacyField(
    val name: String? = null,
    val privacyLevel: String? = null
)

@Serializable
data class UserGroup(
    val id: String,
    val title: String,
    val iconUrl: String? = null
)
