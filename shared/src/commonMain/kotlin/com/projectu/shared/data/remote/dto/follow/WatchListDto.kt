package com.projectu.shared.data.remote.dto.follow

import com.projectu.shared.data.remote.dto.illust.Thumbnails
import com.projectu.shared.data.remote.dto.user.UserInfoBody
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 漫画追更列表响应体
 * 端点: /ajax/watch_list/manga
 */
@Serializable
data class WatchListMangaBody(
    val page: WatchListPage,
    val zoneConfig: JsonElement? = null,
    val extraData: JsonElement? = null,
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,
    val thumbnails: Thumbnails? = null,
    val illustSeries: List<WatchedIllustSeries>? = null,
    val requests: List<String>? = null,
    val users: List<UserInfoBody>? = null  // 复用 UserInfoBody，结构兼容
)

/**
 * 小说追更列表响应体
 * 端点: /ajax/watch_list/novel
 */
@Serializable
data class WatchListNovelBody(
    val page: WatchListPage,
    val zoneConfig: JsonElement? = null,
    val extraData: JsonElement? = null,
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,
    val thumbnails: WatchListNovelThumbnails? = null,
    val illustSeries: List<WatchedIllustSeries>? = null,  // 通常为空
    val requests: List<String>? = null,
    val users: List<UserInfoBody>? = null
)

/**
 * 小说追更列表缩略图
 */
@Serializable
data class WatchListNovelThumbnails(
    val illust: List<JsonElement>? = null,
    val novel: List<JsonElement>? = null,
    val novelSeries: List<WatchedNovelSeries>? = null,
    val novelDraft: List<JsonElement>? = null,
    val collection: List<JsonElement>? = null
)

/**
 * 追更列表分页信息
 */
@Serializable
data class WatchListPage(
    val total: String,           // 总数（字符串类型）
    val maxPage: Int,            // 最大页数
    val watchedSeriesIds: List<String>  // 追更的系列ID列表
)

/**
 * 追更的插画/漫画系列信息
 */
@Serializable
data class WatchedIllustSeries(
    val id: String,                      // 系列ID
    val userId: String,                  // 作者ID
    val title: String,                   // 系列标题
    val description: String? = null,     // 系列描述
    val caption: String? = null,         // 系列说明
    val total: Int,                      // 总作品数
    val content_order: String? = null,   // 内容排序
    val url: String? = null,             // 封面图URL
    val coverImageSl: Int? = null,       // 封面图敏感度级别
    val firstIllustId: String? = null,   // 首个作品ID
    val latestIllustId: String? = null,  // 最新作品ID
    val createDate: String? = null,      // 创建时间
    val updateDate: String? = null,      // 更新时间
    val watchCount: Int? = null,         // 追更人数
    val isWatched: Boolean = false,      // 是否已追更
    val isNotifying: Boolean = false,    // 是否开启通知
    val isRestrictedContent: Boolean = false  // 是否为限制内容
)

/**
 * 追更的小说系列信息
 */
@Serializable
data class WatchedNovelSeries(
    val id: String,                           // 系列ID
    val title: String,                        // 系列标题
    val titleCaptionTranslation: JsonElement? = null,  // 标题翻译
    val cover: NovelSeriesCover? = null,      // 封面信息
    val tags: List<String>? = null,           // 标签列表
    val xRestrict: Int = 0,                   // 限制级别 (0=全年龄, 1=R-18, 2=R-18G)
    val isOriginal: Boolean = false,          // 是否原创
    val genre: String? = null,                // 类型
    val createDateTime: String? = null,       // 创建时间
    val updateDateTime: String? = null,       // 更新时间
    val userId: String? = null,               // 作者ID
    val userName: String? = null,             // 作者名
    val profileImageUrl: String? = null,      // 作者头像
    val bookmarkCount: Int = 0,               // 收藏数
    val isOneshot: Boolean = false,           // 是否单篇
    val caption: String? = null,              // 系列简介
    val isConcluded: Boolean = false,         // 是否完结
    val episodeCount: Int = 0,                // 章节总数
    val publishedEpisodeCount: Int = 0,       // 已发布章节数
    val latestPublishDateTime: String? = null, // 最新发布时间
    val latestEpisodeId: String? = null,      // 最新章节ID
    val isWatched: Boolean = false,           // 是否已追更
    val isNotifying: Boolean = false,         // 是否开启通知
    val restrict: Int = 0,                    // 公开限制
    val textLength: Int = 0,                  // 总字数
    val wordCount: Int = 0,                   // 词数
    val readingTime: Int = 0,                 // 阅读时间（秒）
    val publishedTextLength: Int = 0,         // 已发布字数
    val publishedWordCount: Int = 0,          // 已发布词数
    val publishedReadingTime: Int = 0,        // 已发布阅读时间
    val useWordCount: Boolean = false,        // 是否使用词数
    val aiType: Int = 0                       // AI类型 (1=非AI, 2=AI辅助)
)

/**
 * 小说系列封面信息
 */
@Serializable
data class NovelSeriesCover(
    val urls: NovelSeriesCoverUrls? = null
)

/**
 * 小说系列封面URL
 */
@Serializable
data class NovelSeriesCoverUrls(
    val `240mw`: String? = null,
    val `480mw`: String? = null,
    val `1200x1200`: String? = null,
    val `128x128`: String? = null,
    val original: String? = null
)
