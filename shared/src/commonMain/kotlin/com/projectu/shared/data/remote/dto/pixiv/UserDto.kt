package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.Serializable

/**
 * 用户信息响应体
 */
@Serializable
data class UserInfoBody(
    val userId: String,
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
    val sketchLives: List<String>? = null
)

@Serializable
data class Background(
    val repeat: String? = null,
    val color: String? = null,
    val url: String? = null,
    val isPrivate: Boolean = false
)

/**
 * 用户作品概况响应体
 */
@Serializable
data class ProfileAllBody(
    val illusts: Map<String, String?>? = null,
    val manga: Map<String, String?>? = null,
    val novels: Map<String, String?>? = null,
    val mangaSeries: List<MangaSeriesInfo>? = null,
    val novelSeries: List<String>? = null,
    val pickup: List<String>? = null,
    val bookmarkCount: ProfileBookmarkCount? = null
)

@Serializable
data class MangaSeriesInfo(
    val id: String,
    val title: String
)

@Serializable
data class ProfileBookmarkCount(
    val public: ProfileBookmarkCountDetail,
    val private: ProfileBookmarkCountDetail
)

@Serializable
data class ProfileBookmarkCountDetail(
    val illust: Int = 0,
    val novel: Int = 0
)

/**
 * 用户作品响应体
 */
@Serializable
data class ProfileIllustsBody(
    val works: Map<String, IllustSimple?>? = null,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)

/**
 * 用户推荐响应体
 */
@Serializable
data class UserRecommendBody(
    val users: List<RecommendUser>,
    val thumbnails: Map<String, List<IllustSimple>>? = null
)

@Serializable
data class RecommendUser(
    val userId: String,
    val userName: String,
    val profileImg: String,
    val userComment: String? = null,
    val following: Boolean = false,
    val followed: Boolean = false,
    val illusts: List<RecommendUserIllust>? = null
)

@Serializable
data class RecommendUserIllust(
    val id: String,
    val title: String,
    val illustType: Int,
    val xRestrict: Int,
    val restrict: Int,
    val sl: Int,
    val url: String,
    val description: String,
    val tags: List<String>,
    val userId: String,
    val userName: String,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    val isBookmarkable: Boolean,
    val bookmarkData: BookmarkData? = null,
    val alt: String,
    val createDate: String,
    val updateDate: String
)

/**
 * 用户收藏响应体
 */
@Serializable
data class UserBookmarkBody(
    val works: List<IllustSimple>,
    val total: Int,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)

