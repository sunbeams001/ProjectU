package com.projectu.shared.data.remote.dto.pixiv

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

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

/**
 * 自定义序列化器：处理 Map 或空数组的情况
 * Pixiv API 在有数据时返回 Map，无数据时返回空数组 []
 */
object MapOrEmptyArraySerializer : KSerializer<Map<String, String?>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("MapOrEmptyArray")

    override fun deserialize(decoder: Decoder): Map<String, String?>? {
        val jsonDecoder = decoder as? JsonDecoder ?: return null
        val element = jsonDecoder.decodeJsonElement()
        
        return when {
            element is JsonObject -> {
                element.mapValues { (_, value) -> 
                    if (value is JsonNull) null else value.jsonPrimitive.contentOrNull
                }
            }
            element is JsonArray && element.isEmpty() -> {
                // 空数组返回空Map
                emptyMap()
            }
            else -> null
        }
    }

    override fun serialize(encoder: Encoder, value: Map<String, String?>?) {
        if (value == null) {
            encoder.encodeNull()
        } else {
            val jsonEncoder = encoder as JsonEncoder
            val jsonObject = buildJsonObject {
                value.forEach { (key, v) ->
                    put(key, if (v == null) JsonNull else JsonPrimitive(v))
                }
            }
            jsonEncoder.encodeJsonElement(jsonObject)
        }
    }
}

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

