package com.projectu.shared.data.remote.dto.illust

import com.projectu.shared.data.remote.dto.novel.NovelSearchItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 发现/推荐响应体
 */
@Serializable
data class DiscoveryBody(
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,  // 标签翻译，空时返回[]，非空时返回嵌套Map
    val thumbnails: Thumbnails,
    val illustSeries: List<String>? = null,  // 插画系列ID列表
    val requests: List<String>? = null,  // 请求列表
    val users: List<String>? = null,
    val recommendedNovelIds: List<String>? = null,  // 推荐小说ID列表
    val recommendNovelDetails: Map<String, String>? = null,  // 推荐小说详情（JSON字符串）
    val nextIds: List<Long>? = null
)

@Serializable
data class Thumbnails(
    val illust: List<IllustSimple>,
    val novel: List<NovelSearchItem>? = null,  // 使用 NovelSearchItem 而非 NovelSimple
    val novelSeries: List<NovelSeriesSimple>? = null,
    val novelDraft: List<NovelSearchItem>? = null,  // 使用 NovelSearchItem
    val collection: List<String>? = null  // 收藏集ID列表
)

/**
 * 关注最新响应体
 */
@Serializable
data class FollowLatestBody(
    val page: FollowLatestPage,
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,
    val thumbnails: Thumbnails,
    val illustSeries: List<String>? = null,
    val requests: List<String>? = null,
    val users: List<String>? = null,
    val zoneConfig: kotlinx.serialization.json.JsonElement? = null
)

@Serializable
data class FollowLatestPage(
    val ids: List<String>,
    val isLastPage: Boolean,
    @SerialName("tags")
    val tags: List<String>? = null
)

/**
 * 小说系列简要信息（用于 Thumbnails）
 */
@Serializable
data class NovelSeriesSimple(
    val id: String,
    val title: String
)
