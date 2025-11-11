package com.projectu.shared.data.remote.dto.pixiv

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
data class UserWorkspace(
    val userWorkspacePc: String? = null,
    val userWorkspaceMonitor: String? = null,
    val userWorkspaceTool: String? = null,
    val userWorkspaceScanner: String? = null,
    val userWorkspaceTablet: String? = null,
    val userWorkspaceMouse: String? = null,
    val userWorkspacePrinter: String? = null,
    val userWorkspaceDesktop: String? = null,
    val userWorkspaceMusic: String? = null,
    val userWorkspaceDesk: String? = null,
    val userWorkspaceChair: String? = null,
    val userWorkspaceComment: String? = null,
    val userWorkspaceImageUrl: String? = null
)

@Serializable
data class UserGroup(
    val id: String,
    val title: String,
    val iconUrl: String? = null
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
    val novelSeries: List<NovelSeriesInfo>? = null,
    val collections: List<CollectionInfo>? = null,  // 收藏集列表
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
data class CollectionInfo(
    val id: String,
    val title: String? = null
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
    val userId: Long,
    val userName: String,
    val profileImg: String,
    val userComment: String? = null,
    val following: Boolean = false,
    val followed: Boolean = false,
    val illusts: List<RecommendUserIllust>? = null
)

@Serializable
data class RecommendUserIllust(
    val id: Long,
    val title: String,
    val illustType: Int,
    val xRestrict: Int,
    val restrict: Int,
    val sl: Int,
    val url: String,
    val description: String,
    val tags: List<String>,
    val userId: Long,
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

