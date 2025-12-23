package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.common.ZoneConfig
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import com.projectu.shared.data.remote.dto.novel.NovelSimple
import com.projectu.shared.data.remote.serializers.StringToListUserWorkInfoSerializer
import kotlinx.serialization.Serializable

/**
 * 用户搜索响应体
 */
@Serializable
data class UserSearchBody(
    val data: List<String> = emptyList(),
    val page: UserSearchPage,
    val tagTranslation: Map<String, Map<String, String>> = emptyMap(),
    val thumbnails: UserSearchThumbnails,
    val users: List<UserSearchItem>,
    val zoneConfig: ZoneConfig? = null
)

/**
 * 用户搜索分页信息
 */
@Serializable
data class UserSearchPage(
    val userIds: List<Long>,
    @Serializable(with = StringToListUserWorkInfoSerializer::class)
    val workIds: Map<String, List<UserWorkInfo>> = emptyMap(),
    val total: Int
)

/**
 * 用户作品信息
 */
@Serializable
data class UserWorkInfo(
    val id: String,
    val type: String, // "illust" or "novel"
    val created_at: String
)

/**
 * 用户搜索缩略图
 */
@Serializable
data class UserSearchThumbnails(
    val illust: List<IllustSimple> = emptyList(),
    val novel: List<NovelSimple> = emptyList(),
    val novelSeries: List<String> = emptyList(),
    val novelDraft: List<String> = emptyList(),
    val collection: List<String> = emptyList()
)

/**
 * 用户搜索结果项
 */
@Serializable
data class UserSearchItem(
    val partial: Int,
    val comment: String,
    val followedBack: Boolean,
    val userId: String,
    val name: String,
    val image: String,
    val imageBig: String,
    val premium: Boolean,
    val isFollowed: Boolean,
    val isMypixiv: Boolean,
    val isBlocking: Boolean,
    val background: String? = null,
    val commission: UserCommission? = null
)
