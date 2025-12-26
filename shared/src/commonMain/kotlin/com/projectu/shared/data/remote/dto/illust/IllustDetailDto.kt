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
    val bookStyle: String = "0",  // 注意：这是字符串类型
    val isHowto: Boolean = false,
    val isOriginal: Boolean = false,
    val imageResponseOutData: List<ImageResponseOutData>? = null,
    val imageResponseData: List<IllustSimple>? = null,
    val imageResponseCount: Int = 0,
    val pollData: PollData? = null,
    val seriesNavData: SeriesNavData? = null,
    val descriptionBoothId: String? = null,
    val descriptionYoutubeId: String? = null,
    val comicPromotion: ComicPromotion? = null,
    val fanboxPromotion: FanboxPromotion? = null,
    val contestBanners: List<ContestBanner>? = null,
    val isBookmarkable: Boolean = true,
    val bookmarkData: BookmarkData? = null,
    val contestData: List<ContestData>? = null,
    val zoneConfig: ZoneConfig? = null,
    val extraData: ExtraData? = null,
    val titleCaptionTranslation: TitleCaptionTranslation? = null,
    val isUnlisted: Boolean = false,
    val request: IllustRequestWrapper? = null,  // 改为复杂对象类型
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
 * 比赛/活动数据
 */
@Serializable
data class ContestData(
    val url: String,
    val icon: String,
    val title: String
)

/**
 * 投票数据
 */
@Serializable
data class PollData(
    val question: String,
    val choices: List<PollChoice>,
    val selectedValue: Int? = null,
    val total: Int
)

/**
 * 投票选项
 */
@Serializable
data class PollChoice(
    val id: Int,
    val text: String,
    val count: Int
)

/**
 * 比赛横幅数据
 */
@Serializable
data class ContestBanner(
    val url: String? = null,
    val icon: String? = null,
    val title: String? = null
)

/**
 * 系列导航数据
 */
@Serializable
data class SeriesNavData(
    val seriesType: String,  // "manga" 或 "illust"
    val seriesId: String,
    val title: String,
    val order: Int,  // 在系列中的顺序
    val isWatched: Boolean = false,
    val isNotifying: Boolean = false,
    val prev: SeriesNavItem? = null,  // 前一个作品
    val next: SeriesNavItem? = null   // 后一个作品
)

/**
 * 系列导航项
 */
@Serializable
data class SeriesNavItem(
    val id: String,
    val title: String,
    val order: Int
)

/**
 * 漫画推广信息
 */
@Serializable
data class ComicPromotion(
    val userId: Long,
    val author: String,
    val title: String,
    val workUrl: String,
    val description: String,
    val imgSrc: String,
    val amazonUrl: String? = null,
    val magazine: String? = null,
    val magazineUrl: String? = null
)

/**
 * Fanbox推广信息
 */
@Serializable
data class FanboxPromotion(
    val userName: String? = null,
    val userImageUrl: String? = null,
    val contentUrl: String? = null,
    val description: String? = null,
    val imageUrl: String? = null,
    val imageUrlMobile: String? = null,
    val hasAdultContent: Boolean = false
)

/**
 * 图片响应外部数据（其他用户对该作品的响应/参考作品信息）
 */
@Serializable
data class ImageResponseOutData(
    val type: String,  // "illust" 等
    val workId: String,
    val title: String,
    val userName: String,
    val imageUrl: String
)

/**
 * 点赞响应体
 */
@Serializable
data class LikeBody(
    val isLiked: Boolean
)

// ===================== Request 相关 DTO =====================

/**
 * 约稿请求包装器
 */
@Serializable
data class IllustRequestWrapper(
    val request: IllustRequestInfo? = null,
    val commentOff: Int = 0,
    val creator: RequestUserInfo? = null,
    val fan: RequestUserInfo? = null,
    val collaborateStatus: CollaborateStatus? = null,
    val editable: Boolean = false
)

/**
 * 约稿请求详情
 */
@Serializable
data class IllustRequestInfo(
    val requestId: String? = null,
    val requestStatus: String? = null,  // "complete" 等
    val requestProposal: RequestProposal? = null,
    val requestTags: List<String>? = null,
    val requestAdultFlg: Boolean = false,
    val requestPrice: Int = 0,
    val role: String? = null,  // "others" 等
    val postWork: String? = null,
    val plan: RequestPlan? = null
)

/**
 * 约稿提案
 */
@Serializable
data class RequestProposal(
    val requestOriginalProposal: String? = null,
    val requestOriginalProposalLang: String? = null,
    val requestTranslationProposal: Map<String, RequestTranslation>? = null
)

/**
 * 提案翻译
 */
@Serializable
data class RequestTranslation(
    val requestProposal: String? = null,
    val requestProposalLang: String? = null
)

/**
 * 约稿计划
 */
@Serializable
data class RequestPlan(
    val currentPlanId: String? = null,
    val planId: String? = null,
    val creatorUserId: String? = null,
    val planAcceptRequestFlg: Boolean = false,
    val planStandardPrice: Int = 0,
    val planTitle: PlanTitle? = null,
    val planDescription: PlanDescription? = null,
    val planAcceptAdultFlg: Boolean = false,
    val planAcceptAnonymousFlg: Boolean = false,
    val planAcceptIllustFlg: Boolean = false,
    val planAcceptUgoiraFlg: Boolean = false,
    val planAcceptMangaFlg: Boolean = false,
    val planAcceptNovelFlg: Boolean = false,
    val planCoverImage: PlanCoverImage? = null,
    val planAiType: Int = 0
)

/**
 * 计划标题
 */
@Serializable
data class PlanTitle(
    val planOriginalTitle: String? = null,
    val planOriginalTitleLang: String? = null,
    @Serializable(with = com.projectu.shared.data.remote.serializers.PlanTitleTranslationOrEmptyArraySerializer::class)
    val planTranslationTitle: Map<String, com.projectu.shared.data.remote.serializers.PlanTitleTranslationItem>? = null
)

/**
 * 计划描述
 */
@Serializable
data class PlanDescription(
    val planOriginalDescription: String? = null,
    val planOriginalDescriptionHtml: String? = null,
    val planOriginalLang: String? = null,
    val planTranslationDescription: Map<String, PlanTranslationItem>? = null
)

/**
 * 计划描述翻译项
 */
@Serializable
data class PlanTranslationItem(
    val planDescription: String? = null,
    val planDescriptionHtml: String? = null,
    val planLang: String? = null
)

/**
 * 计划封面图片
 */
@Serializable
data class PlanCoverImage(
    val urls: PlanCoverImageUrls? = null
)

/**
 * 计划封面图片URL
 */
@Serializable
data class PlanCoverImageUrls(
    val cover: String? = null,
    val card: String? = null
)

/**
 * 请求用户信息
 */
@Serializable
data class RequestUserInfo(
    val userId: String? = null,
    val userName: String? = null,
    val profileImg: String? = null
)

/**
 * 协作状态
 */
@Serializable
data class CollaborateStatus(
    val collaborating: Boolean = false,
    val collaborateAnonymousFlg: Boolean = false,
    val collaboratedCnt: Int = 0,
    val userSamples: List<String>? = null  // 可能为空数组
)

