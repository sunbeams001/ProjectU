package com.projectu.shared.data.remote.dto.novel

import com.projectu.shared.data.remote.dto.pixiv.TitleCaptionTranslation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 小说系列详情
 */
@Serializable
data class NovelSeriesBody(
    val id: String,
    val title: String,
    val caption: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("profileImageUrl") val profileImageUrl: String? = null,
    @SerialName("xRestrict") val xRestrict: Int = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    @SerialName("isConcluded") val isConcluded: Boolean = false,
    @SerialName("genreId") val genreId: String? = null,
    val language: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("publishedContentCount") val publishedContentCount: Int,
    @SerialName("publishedTotalCharacterCount") val publishedTotalCharacterCount: Int? = null,
    @SerialName("publishedTotalWordCount") val publishedTotalWordCount: Int? = null,
    @SerialName("publishedReadingTime") val publishedReadingTime: Int? = null,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    @SerialName("lastPublishedContentTimestamp") val lastPublishedContentTimestamp: Long? = null,
    @SerialName("createdTimestamp") val createdTimestamp: Long? = null,
    @SerialName("updatedTimestamp") val updatedTimestamp: Long? = null,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("firstNovelId") val firstNovelId: String? = null,
    @SerialName("latestNovelId") val latestNovelId: String? = null,
    @SerialName("displaySeriesContentCount") val displaySeriesContentCount: Int,
    @SerialName("shareText") val shareText: String? = null,
    @SerialName("total") val total: Int,
    @SerialName("firstEpisode") val firstEpisode: NovelSeriesFirstEpisode? = null,
    @SerialName("watchCount") val watchCount: Int? = null,
    @SerialName("maxXRestrict") val maxXRestrict: Int? = null,
    @SerialName("cover") val cover: NovelSeriesCover? = null,
    @SerialName("coverSettingData") val coverSettingData: JsonObject? = null,
    @SerialName("isWatched") val isWatched: Boolean = false,
    @SerialName("isNotifying") val isNotifying: Boolean = false,
    @SerialName("aiType") val aiType: Int = 0,
    @SerialName("hasGlossary") val hasGlossary: Boolean = false,
    @SerialName("extraData") val extraData: JsonObject? = null,
    @SerialName("zoneConfig") val zoneConfig: JsonObject? = null
)

/**
 * 小说系列首集信息
 */
@Serializable
data class NovelSeriesFirstEpisode(
    val url: String
)

/**
 * 小说系列封面信息
 */
@Serializable
data class NovelSeriesCover(
    val urls: NovelSeriesCoverUrls
)

/**
 * 小说系列封面URL集合
 */
@Serializable
data class NovelSeriesCoverUrls(
    @SerialName("240mw") val size240mw: String? = null,
    @SerialName("480mw") val size480mw: String? = null,
    @SerialName("1200x1200") val size1200x1200: String? = null,
    @SerialName("128x128") val size128x128: String? = null,
    val original: String? = null
)

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
    @SerialName("bookmarkData") val bookmarkData: JsonObject? = null,
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
 */
@Serializable
data class NovelSeriesContent(
    val id: String,
    @SerialName("userId") val userId: String,
    val series: NovelSeriesInfo? = null,
    val title: String,
    @SerialName("commentHtml") val commentHtml: String? = null,
    val tags: List<String> = emptyList(),
    val restrict: Int = 0,
    @SerialName("xRestrict") val xRestrict: Int = 0,
    @SerialName("isOriginal") val isOriginal: Boolean = false,
    @SerialName("textLength") val textLength: Int,
    @SerialName("characterCount") val characterCount: Int,
    @SerialName("wordCount") val wordCount: Int,
    @SerialName("useWordCount") val useWordCount: Boolean = false,
    @SerialName("readingTime") val readingTime: Int,
    @SerialName("bookmarkCount") val bookmarkCount: Int = 0,
    val url: String? = null,
    @SerialName("uploadTimestamp") val uploadTimestamp: Long,
    @SerialName("reuploadTimestamp") val reuploadTimestamp: Long,
    @SerialName("isBookmarkable") val isBookmarkable: Boolean = true,
    @SerialName("bookmarkData") val bookmarkData: JsonObject? = null,
    @SerialName("aiType") val aiType: Int = 0
)

/**
 * 小说系列信息
 */
@Serializable
data class NovelSeriesInfo(
    val id: Long,
    @SerialName("viewableType") val viewableType: Int = 0,
    @SerialName("contentOrder") val contentOrder: Int
)

/**
 * 小说系列标题项
 */
@Serializable
data class NovelSeriesTitle(
    val id: String,
    val title: String,
    @SerialName("available") val available: Boolean = true
)
