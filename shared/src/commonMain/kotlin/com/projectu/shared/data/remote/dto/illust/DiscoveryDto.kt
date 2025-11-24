package com.projectu.shared.data.remote.dto.illust

import com.projectu.shared.data.remote.dto.novel.NovelSearchItem
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

/**
 * 发现/推荐响应体
 * 
 * ⚡ 性能优化：仅反序列化业务需要的字段（tagTranslation、thumbnails）
 * 📝 其他字段使用 @Transient 标记，保留结构信息但不进行反序列化
 */
@Serializable
data class DiscoveryBody(
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,  // 标签翻译，空时返回[]，非空时返回嵌套Map
    val thumbnails: Thumbnails,
    
    // ========== 以下字段不反序列化（性能优化） ==========
    @Transient
    val illustSeries: List<String>? = null,  // 插画系列ID列表（不反序列化）
    @Transient
    val requests: List<String>? = null,  // 请求列表（不反序列化）
    @Transient
    val users: List<String>? = null,  // 用户列表（不反序列化）
    @Transient
    val recommendedIllusts: List<RecommendedIllust>? = null,  // 推荐插画列表-包含推荐算法信息（不反序列化）
    @Transient
    val recommendedNovelIds: List<String>? = null,  // 推荐小说ID列表（不反序列化）
    @Transient
    val recommendNovelDetails: Map<String, String>? = null,  // 推荐小说详情-JSON字符串（不反序列化）
    @Transient
    val nextIds: List<Long>? = null  // 下一页ID列表（不反序列化）
)

/**
 * 推荐插画信息（包含推荐算法元数据）
 */
@Serializable
data class RecommendedIllust(
    val illustId: String,  // 推荐的插画ID
    val recommendMethods: List<String>,  // 推荐方法列表，如 ["clustering_bqalgc"]
    val recommendScore: Double,  // 推荐分数
    val recommendSeedIllustIds: List<String>  // 推荐种子插画ID列表
)

/**
 * 缩略图内容
 * 
 * ⚡ 性能优化：仅反序列化业务需要的字段（illust、novel）
 */
@Serializable
data class Thumbnails(
    val illust: List<IllustSimple>? = null,  // 插画列表，小说发现时为空
    val novel: List<NovelSearchItem>? = null,  // 小说列表，插画发现时为空
    
    // ========== 以下字段不反序列化（性能优化） ==========
    @Transient
    val novelSeries: List<NovelSeriesSimple>? = null,  // 小说系列列表（不反序列化）
    @Transient
    val novelDraft: List<NovelSearchItem>? = null,  // 小说草稿列表（不反序列化）
    @Transient
    val collection: List<String>? = null  // 收藏集ID列表（不反序列化）
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
