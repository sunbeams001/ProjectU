package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import com.projectu.shared.data.remote.dto.pixiv.TitleCaptionTranslation
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

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

/**
 * 小说系列 API
 * 提供小说系列的查询功能
 */
class NovelSeriesApi(private val client: PixivApiClient) {

    /**
     * 查询小说系列详情
     * @param seriesId 系列ID
     */
    suspend fun getDetail(seriesId: Long): PixivResponse<NovelSeriesBody> {
        return client.get("/ajax/novel/series/$seriesId")
    }

    /**
     * 查询系列中作品的基础信息
     * @param seriesId 系列ID
     * @param limit 返回数量
     * @param lastOrder 最后一个作品的序号（用于分页）
     * @param orderBy 排序方式：asc(升序), desc(降序)
     */
    suspend fun getContents(
        seriesId: Long,
        limit: Int = 30,
        lastOrder: Int? = null,
        orderBy: String = "asc"
    ): PixivResponse<NovelSeriesContentBody> {
        val params = mutableMapOf<String, Any?>(
            "limit" to limit,
            "order_by" to orderBy
        )
        lastOrder?.let { params["last_order"] = it }

        return client.get("/ajax/novel/series_content/$seriesId", params)
    }

    /**
     * 查询系列的各篇标题
     * @param seriesId 系列ID
     */
    suspend fun getTitles(seriesId: Long): PixivResponse<List<NovelSeriesTitle>> {
        return client.get("/ajax/novel/series/$seriesId/content_titles")
    }

    /**
     * 加入追更列表
     * @param seriesId 系列ID
     */
    suspend fun watch(seriesId: Long): PixivResponse<List<String>> {
        return client.postJson("/ajax/novel/series/$seriesId/watch", buildJsonObject {})
    }

    /**
     * 移除追更
     * @param seriesId 系列ID
     */
    suspend fun unwatch(seriesId: Long): PixivResponse<List<String>> {
        return client.postJson("/ajax/novel/series/$seriesId/unwatch", buildJsonObject {})
    }
}
