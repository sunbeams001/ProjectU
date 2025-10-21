package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 插画详情响应体
 */
@Serializable
data class IllustDetailBody(
    val illustId: String,
    val illustTitle: String,
    val illustComment: String,
    val id: String,
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
    val userId: String,
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
    val bookStyle: String? = null,
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
    val aiType: Int = 0
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
    val authorId: String? = null,
    val isLocked: Boolean = false,
    val tags: List<IllustTag>,
    val writable: Boolean = true
)

@Serializable
data class IllustTag(
    val tag: String,
    val locked: Boolean = false,
    val deletable: Boolean = false,
    val userId: String? = null,
    val userName: String? = null,
    val translation: Map<String, String>? = null,
    val romaji: String? = null
)

@Serializable
data class IllustSimple(
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
    val titleCaptionTranslation: TitleCaptionTranslation? = null,
    val createDate: String,
    val updateDate: String,
    val isUnlisted: Boolean = false,
    val isMasked: Boolean = false,
    val aiType: Int = 0,
    val profileImageUrl: String? = null
)

@Serializable
data class BookmarkData(
    val id: String? = null,
    val private: Boolean = false
)

@Serializable
data class ZoneConfig(
    val header: ZoneConfigItem? = null,
    val footer: ZoneConfigItem? = null,
    val logo: ZoneConfigItem? = null
)

@Serializable
data class ZoneConfigItem(
    val url: String
)

@Serializable
data class ExtraData(
    val meta: MetaData? = null
)

@Serializable
data class MetaData(
    val title: String,
    val description: String,
    val canonical: String,
    val alternateLanguages: Map<String, String>? = null,
    val descriptionHeader: String? = null,
    val ogp: OgpData? = null,
    val twitter: TwitterData? = null
)

@Serializable
data class OgpData(
    val description: String,
    val image: String,
    val title: String,
    val type: String
)

@Serializable
data class TwitterData(
    val description: String,
    val image: String,
    val title: String,
    val card: String
)

@Serializable
data class TitleCaptionTranslation(
    val workTitle: String? = null,
    val workCaption: String? = null
)

/**
 * Ugoira 元数据响应体
 */
@Serializable
data class UgoiraMetaBody(
    val src: String,
    val originalSrc: String,
    val mime_type: String,
    val frames: List<UgoiraFrame>
)

@Serializable
data class UgoiraFrame(
    val file: String,
    val delay: Int
)

/**
 * 搜索结果响应体
 */
@Serializable
data class IllustSearchBody(
    val illustManga: IllustMangaData,
    val popular: PopularData? = null,
    val relatedTags: List<String>? = null,
    val tagTranslation: Map<String, Map<String, String>>? = null,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null
)

@Serializable
data class IllustMangaData(
    val data: List<IllustSimple>,
    val total: Int,
    val bookmarkRanges: List<BookmarkRange>? = null
)

@Serializable
data class BookmarkRange(
    val min: Int,
    val max: Int
)

@Serializable
data class PopularData(
    val recent: List<IllustSimple>? = null,
    val permanent: List<IllustSimple>? = null
)

/**
 * 发现/推荐响应体
 */
@Serializable
data class DiscoveryBody(
    val thumbnails: Thumbnails,
    val users: List<String>? = null,
    val nextIds: List<Long>? = null
)

@Serializable
data class Thumbnails(
    val illust: List<IllustSimple>,
    val novel: List<String>? = null,
    val novelSeries: List<String>? = null,
    val novelDraft: List<String>? = null
)

/**
 * 推荐作品响应体
 */
@Serializable
data class IllustRecommendBody(
    val illusts: List<IllustSimple>,
    val nextIds: List<Long>
)

/**
 * 推荐初始化响应体
 */
@Serializable
data class IllustRecommendInitBody(
    val illusts: List<IllustSimple>,
    val nextIds: List<Long>,
    val details: Map<String, IllustDetailBody>? = null
)

/**
 * 关注最新响应体
 */
@Serializable
data class FollowLatestBody(
    val page: FollowLatestPage,
    val thumbnails: Thumbnails,
    val illustSeries: List<String>? = null,
    val requests: List<String>? = null,
    val users: List<String>? = null
)

@Serializable
data class FollowLatestPage(
    val ids: List<String>,
    val isLastPage: Boolean,
    @SerialName("tags")
    val tags: List<String>? = null
)

/**
 * 点赞响应体
 */
@Serializable
data class LikeBody(
    val isLiked: Boolean
)

