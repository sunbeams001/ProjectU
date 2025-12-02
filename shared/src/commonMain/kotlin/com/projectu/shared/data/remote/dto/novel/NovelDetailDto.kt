package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.PixivTag
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说标签信息
 */
@Serializable
data class NovelTagInfo(
    @SerialName("authorId") val authorId: String,
    @SerialName("isLocked") val isLocked: Boolean,
    val tags: List<PixivTag> = emptyList(),
    val writable: Boolean
)

/**
 * 小说系列导航数据
 */
@Serializable
data class NovelSeriesNavData(
    @SerialName("seriesType") val seriesType: String? = null,
    @SerialName("seriesId") val seriesId: Long? = null,
    val title: String? = null,
    @SerialName("isConcluded") val isConcluded: Boolean = false,
    @SerialName("isReplaceable") val isReplaceable: Boolean = false,
    @SerialName("isWatched") val isWatched: Boolean = false,
    @SerialName("isNotifying") val isNotifying: Boolean = false,
    val order: Int? = null,
    val prev: NovelSeriesNavItem? = null,
    val next: NovelSeriesNavItem? = null
)

/**
 * 系列中的小说导航项
 */
@Serializable
data class NovelSeriesNavItem(
    val title: String,
    val order: Int,
    val id: String,
    val available: Boolean = true
)

/**
 * 内嵌图片信息
 */
@Serializable
data class NovelEmbeddedImage(
    @SerialName("novelImageId") val novelImageId: String,
    val sl: String? = null,  // 敏感级别
    val urls: NovelEmbeddedImageUrls
)

/**
 * 内嵌图片的URL集合
 */
@Serializable
data class NovelEmbeddedImageUrls(
    @SerialName("240mw") val small: String? = null,
    @SerialName("480mw") val medium: String? = null,
    @SerialName("1200x1200") val large: String? = null,
    @SerialName("128x128") val thumbnail: String? = null,
    val original: String? = null
)

/**
 * 小说详情响应体
 */
@Serializable
data class NovelDetailBody(
    val id: String,
    val title: String,
    val content: String,
    @SerialName("createDate") val createDate: String,
    @SerialName("uploadDate") val uploadDate: String,
    val description: String,
    @SerialName("bookmarkCount") val bookmarkCount: Int,
    @SerialName("likeCount") val likeCount: Int,
    @SerialName("viewCount") val viewCount: Int,
    @SerialName("commentCount") val commentCount: Int,
    @SerialName("markerCount") val markerCount: Int,
    val marker: Int? = null, // 当前用户的阅读书签位置（页码）
    @SerialName("pageCount") val pageCount: Int,
    @SerialName("isOriginal") val isOriginal: Boolean,
    @SerialName("isBungei") val isBungei: Boolean,
    @SerialName("xRestrict") val xRestrict: Int,
    val restrict: Int,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    val tags: NovelTagInfo,
    @SerialName("bookmarkData") val bookmarkData: BookmarkData? = null,
    // 新增字段
    @SerialName("coverUrl") val coverUrl: String? = null,
    @SerialName("characterCount") val characterCount: Int? = null,  // 总字符数
    @SerialName("wordCount") val wordCount: Int? = null,  // 单词/词语数
    @SerialName("readingTime") val readingTime: Int? = null,  // 预计阅读时间（分钟）
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    val language: String? = null,  // 语言代码，如 "zh-cn"
    val genre: String? = null,  // 小说类型
    @SerialName("aiType") val aiType: Int = 0,  // AI类型：0=未设置，1=人工，2=AI辅助
    @SerialName("isUnlisted") val isUnlisted: Boolean = false,
    @SerialName("isLoginOnly") val isLoginOnly: Boolean = false,
    @SerialName("likeData") val likeData: Boolean = false,  // 是否已点赞
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    // 系列信息
    @SerialName("seriesNavData") val seriesNavData: NovelSeriesNavData? = null,
    // 内嵌图片映射（key为图片ID，value为图片信息）
    @SerialName("textEmbeddedImages") val textEmbeddedImages: Map<String, NovelEmbeddedImage>? = null
)
