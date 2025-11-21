package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import kotlinx.serialization.Serializable

/**
 * 用户推荐响应体
 */
@Serializable
data class UserRecommendBody(
    val recommendUsers: List<RecommendUser>,
    val thumbnails: RecommendThumbnails? = null,
    val users: List<RecommendUserDetail>? = null
)

@Serializable
data class RecommendThumbnails(
    val illust: List<IllustSimple>? = null,
    val novel: List<NovelThumbnail>? = null
)

@Serializable
data class RecommendUser(
    val userId: String,
    val illustIds: List<String> = emptyList(),
    val novelIds: List<String> = emptyList()
)

/**
 * 推荐用户详细信息（包含在推荐响应的users字段中）
 */
@Serializable
data class RecommendUserDetail(
    val userId: String,
    val name: String,
    val image: String,
    val imageBig: String,
    val comment: String? = null,
    val followedBack: Boolean = false,
    val isFollowed: Boolean = false,
    val isMypixiv: Boolean = false,
    val isBlocking: Boolean = false,
    val premium: Boolean = false,
    val background: String? = null,
    val partial: Int = 0,
    val commission: Commission? = null
)

/**
 * 小说缩略信息（用于推荐用户的作品展示）
 */
@Serializable
data class NovelThumbnail(
    val id: String,
    val title: String,
    val genre: String,
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val url: String,
    val tags: List<String> = emptyList(),
    val userId: String,
    val userName: String,
    val profileImageUrl: String,
    val textCount: Int = 0,
    val wordCount: Int = 0,
    val readingTime: Int = 0,
    val useWordCount: Boolean = false,
    val description: String? = null,
    val isBookmarkable: Boolean = false,
    val bookmarkData: BookmarkData? = null,
    val bookmarkCount: Int = 0,
    val isOriginal: Boolean = false,
    val marker: String? = null,
    val titleCaptionTranslation: TitleCaptionTranslation? = null,
    val createDate: String? = null,
    val updateDate: String? = null,
    val isMasked: Boolean = false,
    val aiType: Int = 0,
    val seriesId: String? = null,
    val seriesTitle: String? = null,
    val isUnlisted: Boolean = false,
    val visibilityScope: Int = 0,
    val language: String? = null
)
