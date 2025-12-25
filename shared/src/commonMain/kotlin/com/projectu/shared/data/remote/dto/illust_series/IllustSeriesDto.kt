package com.projectu.shared.data.remote.dto.illust_series

import com.projectu.shared.data.remote.dto.illust.IllustSimple
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 漫画系列详情响应体
 * 对应 /ajax/series/{seriesId}?p=1&lang=zh
 */
@Serializable
data class IllustSeriesBody(
    /**
     * 标签翻译映射
     * key: 原始标签名, value: 各语言翻译
     */
    @SerialName("tagTranslation")
    val tagTranslation: Map<String, TagTranslation> = emptyMap(),
    
    /**
     * 缩略图信息
     */
    val thumbnails: IllustSeriesThumbnails? = null,
    
    /**
     * 系列列表（当前用户的漫画系列）
     */
    val illustSeries: List<IllustSeriesInfo> = emptyList(),
    
    /**
     * 请求信息
     */
    val requests: List<JsonObject> = emptyList(),
    
    /**
     * 用户信息列表
     */
    val users: List<IllustSeriesUser> = emptyList(),
    
    /**
     * 分页信息
     */
    val page: IllustSeriesPage? = null,
    
    /**
     * 额外数据（SEO等）
     */
    val extraData: IllustSeriesExtraData? = null,
    
    /**
     * 广告区配置
     */
    val zoneConfig: JsonObject? = null
)

/**
 * 标签翻译
 */
@Serializable
data class TagTranslation(
    val en: String? = null,
    val ko: String? = null,
    val zh: String? = null,
    @SerialName("zh_tw")
    val zhTw: String? = null,
    val romaji: String? = null
)

/**
 * 缩略图信息
 */
@Serializable
data class IllustSeriesThumbnails(
    val illust: List<IllustSimple> = emptyList(),
    val novel: List<JsonObject> = emptyList(),
    val novelSeries: List<JsonObject> = emptyList(),
    val novelDraft: List<JsonObject> = emptyList(),
    val collection: List<JsonObject> = emptyList()
)

/**
 * 漫画系列信息
 */
@Serializable
data class IllustSeriesInfo(
    val id: String,
    val userId: String,
    val title: String,
    val description: String,
    val caption: String,
    val total: Int,
    @SerialName("content_order")
    val contentOrder: String? = null,
    val url: String? = null,
    val coverImageSl: Int? = null,
    val firstIllustId: String? = null,
    val latestIllustId: String? = null,
    val createDate: String,
    val updateDate: String,
    val watchCount: Int? = null,
    val isWatched: Boolean = false,
    val isNotifying: Boolean = false
)

/**
 * 系列用户信息
 */
@Serializable
data class IllustSeriesUser(
    val partial: Int = 0,
    val comment: String? = null,
    val followedBack: Boolean = false,
    val userId: String,
    val name: String,
    val image: String? = null,
    val imageBig: String? = null,
    val premium: Boolean = false,
    val isFollowed: Boolean = false,
    val isMypixiv: Boolean = false,
    val isBlocking: Boolean = false,
    val background: JsonObject? = null,
    val commission: JsonObject? = null
)

/**
 * 分页信息
 */
@Serializable
data class IllustSeriesPage(
    /**
     * 系列作品列表
     */
    val series: List<IllustSeriesWork> = emptyList(),
    
    /**
     * 是否已设置封面
     */
    val isSetCover: Boolean = false,
    
    /**
     * 当前系列ID
     */
    val seriesId: Long = 0,
    
    /**
     * 其他系列ID
     */
    val otherSeriesId: String? = null,
    
    /**
     * 最近更新的作品ID列表
     */
    val recentUpdatedWorkIds: List<Long> = emptyList(),
    
    /**
     * 作品总数
     */
    val total: Int = 0,
    
    /**
     * 是否已追更
     */
    val isWatched: Boolean = false,
    
    /**
     * 是否接收通知
     */
    val isNotifying: Boolean = false
)

/**
 * 系列中的作品
 */
@Serializable
data class IllustSeriesWork(
    val workId: String,
    val order: Int
)

/**
 * 额外数据
 */
@Serializable
data class IllustSeriesExtraData(
    val meta: IllustSeriesMeta? = null
)

/**
 * SEO元数据
 */
@Serializable
data class IllustSeriesMeta(
    val title: String? = null,
    val description: String? = null,
    val canonical: String? = null,
    val ogp: IllustSeriesOgp? = null,
    val twitter: IllustSeriesTwitter? = null
)

/**
 * OGP元数据
 */
@Serializable
data class IllustSeriesOgp(
    val type: String? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null
)

/**
 * Twitter卡片
 */
@Serializable
data class IllustSeriesTwitter(
    val card: String? = null,
    val site: String? = null,
    val title: String? = null,
    val description: String? = null,
    val image: String? = null
)
