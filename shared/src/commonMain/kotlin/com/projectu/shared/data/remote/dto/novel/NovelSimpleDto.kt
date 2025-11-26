package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说简要信息（通用）
 * 
 * 使用场景：
 * - 搜索接口 (/ajax/search/novels/{keyword})
 * - 发现接口 (/ajax/discovery/novels) 的 Thumbnails.novel
 * - 关注作者最新小说 (/ajax/follow_latest/novel)
 * 
 * 特点：
 * - 包含小说基本信息和统计数据
 * - 不包含正文内容（content）
 * - 包含阅读信息（字数、阅读时间）
 */
@Serializable
data class NovelSimple(
    val id: String,
    val title: String,
    val genre: String,
    val xRestrict: Int = 0,
    val restrict: Int = 0,
    val url: String,
    val tags: List<String> = emptyList(),
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String,
    @SerialName("textCount") val textCount: Int = 0,
    @SerialName("wordCount") val wordCount: Int = 0,
    @SerialName("readingTime") val readingTime: Int = 0,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    val description: String = "",
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    @SerialName("bookmarkData") val bookmarkData: BookmarkData? = null,
    @SerialName("bookmarkCount") val bookmarkCount: Int = 0,
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
    val language: String = "ja"
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
