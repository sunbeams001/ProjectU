package com.projectu.shared.data.remote.dto.user

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import com.projectu.shared.data.remote.dto.common.ZoneConfig
import com.projectu.shared.data.remote.dto.common.ExtraData
import com.projectu.shared.data.remote.dto.illust.IllustSimple
import com.projectu.shared.data.remote.serializers.MapOrEmptyArraySerializer
import kotlinx.serialization.Serializable

/**
 * 用户作品概况响应体
 */
@Serializable
data class ProfileAllBody(
    @Serializable(with = MapOrEmptyArraySerializer::class)
    val illusts: Map<String, String?>? = null,
    @Serializable(with = MapOrEmptyArraySerializer::class)
    val manga: Map<String, String?>? = null,
    @Serializable(with = MapOrEmptyArraySerializer::class)
    val novels: Map<String, String?>? = null,
    val mangaSeries: List<MangaSeriesInfo>? = null,
    val novelSeries: List<NovelSeriesInfo>? = null,
    @Serializable(with = MapOrEmptyArraySerializer::class)
    val collections: Map<String, String?>? = null,  // 收藏集Map (ID -> null)
    val collectionIds: List<String>? = null,  // 收藏集 ID 列表
    val pickup: List<PickupInfo>? = null,
    val bookmarkCount: ProfileBookmarkCount? = null,
    val externalSiteWorksStatus: ExternalSiteWorksStatus? = null,  // 外部站点作品状态
    val request: UserRequestInfo? = null,  // 请求相关信息
    val shouldShowSensitiveNotice: Boolean = false  // 是否显示敏感内容提示
)

@Serializable
data class MangaSeriesInfo(
    val id: String,  // 改为 String，与实际返回一致
    val title: String
)

@Serializable
data class NovelSeriesInfo(
    val id: String,
    val userId: String,
    val userName: String? = null,
    val profileImageUrl: String? = null,
    val xRestrict: Int = 0,
    val isOriginal: Boolean = false,
    val isConcluded: Boolean = false,
    val genreId: String? = null,
    val title: String,
    val caption: String? = null,
    val language: String? = null,
    val tags: List<String>? = null,
    val publishedContentCount: Int = 0,
    val publishedTotalCharacterCount: Int = 0,
    val publishedTotalWordCount: Int = 0,
    val publishedReadingTime: Int = 0,
    val useWordCount: Boolean = false,
    val lastPublishedContentTimestamp: Long = 0,
    val createdTimestamp: Long = 0,
    val updatedTimestamp: Long = 0,
    val createDate: String? = null,
    val updateDate: String? = null,
    val firstNovelId: String? = null,
    val latestNovelId: String? = null,
    val displaySeriesContentCount: Int = 0,
    val shareText: String? = null,
    val total: Int = 0,
    val firstEpisode: EpisodeCover? = null,
    val watchCount: Int? = null,
    val maxXRestrict: Int? = null,
    val cover: NovelSeriesCover? = null,
    val coverSettingData: String? = null,
    val isWatched: Boolean = false,
    val isNotifying: Boolean = false,
    val aiType: Int = 0
)

@Serializable
data class EpisodeCover(
    val url: String
)

@Serializable
data class NovelSeriesCover(
    val urls: NovelSeriesCoverUrls
)

@Serializable
data class NovelSeriesCoverUrls(
    val `240mw`: String? = null,
    val `480mw`: String? = null,
    val `1200x1200`: String? = null,
    val `128x128`: String? = null,
    val original: String? = null
)

@Serializable
data class PickupInfo(
    val id: String,
    val title: String,
    val illustType: Int = 0,
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val sl: Int = 0,
    val url: String,
    val description: String? = null,
    val tags: List<String>? = null,
    val userId: String,
    val userName: String,
    val width: Int = 0,
    val height: Int = 0,
    val pageCount: Int = 0,
    val isBookmarkable: Boolean = false,
    val bookmarkData: BookmarkData? = null,
    val alt: String? = null,
    val titleCaptionTranslation: TitleCaptionTranslation? = null,
    val createDate: String? = null,
    val updateDate: String? = null,
    val isUnlisted: Boolean = false,
    val isMasked: Boolean = false,
    val aiType: Int = 0,
    val visibilityScope: Int = 0,
    val type: String? = null,
    val deletable: Boolean = false,
    val draggable: Boolean = false,
    val contentUrl: String? = null
)

@Serializable
data class ExternalSiteWorksStatus(
    val booth: Boolean = false,
    val sketch: Boolean = false,
    val vroidHub: Boolean = false
)

@Serializable
data class UserRequestInfo(
    val showRequestTab: Boolean = false,
    val showRequestSentTab: Boolean = false,
    val postWorks: RequestPostWorks? = null
)

@Serializable
data class RequestPostWorks(
    val artworks: List<String>? = null,
    val novels: List<String>? = null
)

@Serializable
data class ProfileBookmarkCount(
    val public: ProfileBookmarkCountDetail,
    val private: ProfileBookmarkCountDetail
)

@Serializable
data class ProfileBookmarkCountDetail(
    val illust: Int = 0,
    val novel: Int = 0,
    val collection: Int = 0
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
