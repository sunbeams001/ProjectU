package com.projectu.shared.data.remote.dto.novel_series

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.TitleCaptionTranslation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 小说系列内容响应体
 */
@Serializable
data class NovelSeriesContentBody(
    @SerialName("tagTranslation") val tagTranslation: List<String> = emptyList(),
    @SerialName("thumbnails") val thumbnails: NovelSeriesThumbnails? = null,
    @SerialName("illustSeries") val illustSeries: List<String> = emptyList(),
    @SerialName("requests") val requests: List<String> = emptyList(),
    @SerialName("users") val users: List<String> = emptyList(),
    @SerialName("page") val page: NovelSeriesPage
)

/**
 * 小说系列缩略图信息
 */
@Serializable
data class NovelSeriesThumbnails(
    @SerialName("illust") val illust: List<String> = emptyList(),
    @SerialName("novel") val novel: List<NovelThumbnail> = emptyList(),
    @SerialName("novelSeries") val novelSeries: List<String> = emptyList(),
    @SerialName("novelDraft") val novelDraft: List<String> = emptyList(),
    @SerialName("collection") val collection: List<String> = emptyList()
)

/**
 * 小说缩略图详情
 */
@Serializable
data class NovelThumbnail(
    val id: String,
    val title: String,
    val genre: String,
    @SerialName("xRestrict") val xRestrict: Int = 0,
    val restrict: Int = 0,
    val url: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("textCount") val textCount: Int,
    @SerialName("wordCount") val wordCount: Int,
    @SerialName("readingTime") val readingTime: Int,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    val description: String? = null,
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    @SerialName("bookmarkData") val bookmarkData: BookmarkData? = null,
    @SerialName("bookmarkCount") val bookmarkCount: Int = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    @SerialName("marker") val marker: JsonObject? = null,
    @SerialName("titleCaptionTranslation") val titleCaptionTranslation: TitleCaptionTranslation? = null,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("isMasked") val isMasked: Boolean = false,
    @SerialName("aiType") val aiType: Int = 0,
    @SerialName("seriesId") val seriesId: String? = null,
    @SerialName("seriesTitle") val seriesTitle: String? = null,
    @SerialName("isUnlisted") val isUnlisted: Boolean = false,
    @SerialName("visibilityScope") val visibilityScope: Int = 0,
    val language: String? = null
)

/**
 * 小说系列分页信息
 */
@Serializable
data class NovelSeriesPage(
    @SerialName("seriesContents") val seriesContents: List<NovelSeriesContent> = emptyList()
)

/**
 * 小说系列内容项
 * 注意：对于 viewableType=2 (好P友限定) 的作品，响应体只包含 id、userId 和 series 字段
 */
@Serializable
data class NovelSeriesContent(
    val id: String,
    @SerialName("userId") val userId: String,
    val series: NovelSeriesInfo? = null,
    val title: String? = null,
    @SerialName("commentHtml") val commentHtml: String? = null,
    val tags: List<String> = emptyList(),
    val restrict: Int = 0,
    @SerialName("xRestrict") val xRestrict: Int = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    @SerialName("textLength") val textLength: Int = 0,
    @SerialName("characterCount") val characterCount: Int = 0,
    @SerialName("wordCount") val wordCount: Int = 0,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    @SerialName("readingTime") val readingTime: Int = 0,
    @SerialName("bookmarkCount") val bookmarkCount: Int = 0,
    val url: String? = null,
    @SerialName("uploadTimestamp") val uploadTimestamp: Long = 0,
    @SerialName("reuploadTimestamp") val reuploadTimestamp: Long = 0,
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    @SerialName("bookmarkData") val bookmarkData: JsonObject? = null,
    @SerialName("aiType") val aiType: Int = 0
) {
    /**
     * 判断该内容是否可查看
     * viewableType: 0=公开, 2=好P友限定
     */
    val isViewable: Boolean
        get() = series?.viewableType == 0 || series?.viewableType == null
}

/**
 * 小说系列标题项
 */
@Serializable
data class NovelSeriesTitle(
    val id: String,
    val title: String,
    @SerialName("available") val available: Boolean = true
)
