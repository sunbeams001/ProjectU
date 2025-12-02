package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import com.projectu.shared.data.remote.serializers.FlexibleStringSerializer
import com.projectu.shared.data.remote.serializers.FlexibleIntOrNullSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说简要信息（通用）
 * 
 * 使用场景：
 * - 搜索接口 (/ajax/search/novels/{keyword})
 * - 发现接口 (/ajax/discovery/novels) 的 Thumbnails.novel
 * - 关注作者最新小说 (/ajax/follow_latest/novel)
 * - 用户收藏小说 (/ajax/user/{userId}/novels/bookmarks)
 * 
 * 特点：
 * - 包含小说基本信息和统计数据
 * - 不包含正文内容（content）
 * - 包含阅读信息（字数、阅读时间）
 * 
 * 注意：被删除/隐藏的作品可能返回异常值：
 * - genre: 可能是字符串 "fantasy" 或整数 0
 * - userId: 可能是字符串 "12345" 或整数 0
 * - bookmarkCount: 可能是整数或 null
 */
@Serializable
data class NovelSimple(
    val id: String,
    val title: String,
    // genre 可能是字符串或整数（被删除作品返回 0）
    @Serializable(with = FlexibleStringSerializer::class)
    val genre: String,
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val url: String,
    val tags: List<String> = emptyList(),
    // userId 可能是字符串或整数（被删除作品返回 0）
    @SerialName("userId")
    @Serializable(with = FlexibleStringSerializer::class)
    val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String,
    @SerialName("textCount") val textCount: Int = 0,
    @SerialName("wordCount") val wordCount: Int = 0,
    @SerialName("readingTime") val readingTime: Int = 0,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    val description: String = "",
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    @SerialName("bookmarkData") val bookmarkData: BookmarkData? = null,
    // bookmarkCount 可能是整数或 null（被删除作品返回 null）
    @SerialName("bookmarkCount")
    @Serializable(with = FlexibleIntOrNullSerializer::class)
    val bookmarkCount: Int? = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    val marker: Int? = null,
    @SerialName("titleCaptionTranslation") val titleCaptionTranslation: TitleCaptionTranslation? = null,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("isMasked") val isMasked: Boolean = false,
    @SerialName("aiType") val aiType: Int = 0,
    @SerialName("seriesId") val seriesId: String? = null,
    @SerialName("seriesTitle") val seriesTitle: String? = null,
    @SerialName("isUnlisted") val isUnlisted: Boolean = false,
    @SerialName("visibilityScope") val visibilityScope: Int = 0,
    val language: String = "ja",
    // 被删除作品特有字段
    val maskReason: String? = null
)

/**
 * 小说系列简要信息
 * 
 * 使用场景：
 * - Thumbnails.novelSeries 字段
 */
@Serializable
data class NovelSeriesSimple(
    val id: String,
    val title: String
)
