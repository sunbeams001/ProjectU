package com.projectu.shared.data.remote.dto.illust

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.common.PixivTag
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import com.projectu.shared.data.remote.dto.common.ZoneConfig
import kotlinx.serialization.Serializable

/**
 * 插画详情响应体
 */
@Serializable
data class IllustDetailBody(
    val illustId: String,  // 注意：这里是字符串类型，不是Long
    val illustTitle: String,
    val illustComment: String,
    val id: String,  // 注意：这里也是字符串类型
    val title: String,
    val description: String,
    val illustType: Int,
    val createDate: String,
    val uploadDate: String,
    val restrict: Int,
    val xRestrict: Int,
    val sl: Int,
    val urls: IllustUrls,
    val tags: IllustTags,
    val alt: String,
    val storableTags: List<String>? = null,
    val userId: String,  // 字符串类型
    val userName: String,
    val userAccount: String,
    val userIllusts: Map<String, IllustSimple?>? = null,
    val likeData: Boolean,
    val width: Int,
    val height: Int,
    val pageCount: Int,
    val bookmarkCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val responseCount: Int,
    val viewCount: Int,
    val bookStyle: Int = 0,
    val isHowto: Boolean = false,
    val isOriginal: Boolean = false,
    val imageResponseOutData: List<String>? = null,
    val imageResponseData: List<String>? = null,
    val imageResponseCount: Int = 0,
    val pollData: String? = null,
    val seriesNavData: String? = null,
    val descriptionBoothId: String? = null,
    val descriptionYoutubeId: String? = null,
    val comicPromotion: String? = null,
    val fanboxPromotion: String? = null,
    val contestBanners: List<String>? = null,
    val isBookmarkable: Boolean = true,
    val bookmarkData: BookmarkData? = null,
    val contestData: String? = null,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null,
    val titleCaptionTranslation: TitleCaptionTranslation? = null,
    val isUnlisted: Boolean = false,
    val request: String? = null,
    val commentOff: Int = 0,
    val aiType: Int = 0,
    val reuploadDate: String? = null,
    val locationMask: Boolean = false,
    val commissionLinkHidden: Boolean = false,
    val isLoginOnly: Boolean = false
)

@Serializable
data class IllustUrls(
    val mini: String,
    val thumb: String,
    val small: String,
    val regular: String,
    val original: String
)

@Serializable
data class IllustTags(
    val authorId: String? = null,  // 字符串类型
    val isLocked: Boolean = false,
    val tags: List<PixivTag>,
    val writable: Boolean = true
)

/**
 * 点赞响应体
 */
@Serializable
data class LikeBody(
    val isLiked: Boolean
)
