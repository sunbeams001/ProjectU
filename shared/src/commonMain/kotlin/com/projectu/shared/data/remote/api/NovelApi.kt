package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.BookmarkData
import com.projectu.shared.data.remote.dto.pixiv.NovelBookmarkStatusBody
import com.projectu.shared.data.remote.dto.pixiv.DiscoveryBody
import com.projectu.shared.data.remote.dto.pixiv.FollowLatestBody
import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import io.ktor.http.encodeURLPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说标签信息
 */
@Serializable
data class NovelTagInfo(
    @SerialName("authorId") val authorId: String,
    @SerialName("isLocked") val isLocked: Boolean,
    val tags: List<NovelTag> = emptyList(),
    val writable: Boolean
)

/**
 * 小说标签
 */
@Serializable
data class NovelTag(
    val tag: String,
    val locked: Boolean,
    val deletable: Boolean,
    @SerialName("userId") val userId: String? = null,
    @SerialName("userName") val userName: String? = null
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
    @SerialName("pageCount") val pageCount: Int,
    @SerialName("isOriginal") val isOriginal: Boolean,
    @SerialName("isBungei") val isBungei: Boolean,
    @SerialName("xRestrict") val xRestrict: Int,
    val restrict: Int,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    val tags: NovelTagInfo
)

/**
 * 小说搜索响应体
 */
@Serializable
data class NovelSearchBody(
    val novel: NovelSearchData,
    val relatedTags: List<String> = emptyList(),
    @Serializable(with = com.projectu.shared.data.remote.serializers.NestedMapOrEmptyArraySerializer::class)
    val tagTranslation: Map<String, Map<String, String>>? = null,  // 简单的两层嵌套
    val zoneConfig: kotlinx.serialization.json.JsonElement? = null,  // 复杂嵌套，使用JsonElement
    val extraData: kotlinx.serialization.json.JsonElement? = null  // 复杂嵌套，使用JsonElement
)

/**
 * 小说搜索数据
 */
@Serializable
data class NovelSearchData(
    val data: List<NovelSearchItem> = emptyList(),
    val total: Int = 0,
    val lastPage: Int = 0,
    val bookmarkRanges: List<BookmarkRange> = emptyList()
)

/**
 * 收藏数范围
 */
@Serializable
data class BookmarkRange(
    val min: Int? = null,
    val max: Int? = null
)

/**
 * 小说搜索项
 */
@Serializable
data class NovelSearchItem(
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
    @SerialName("bookmarkData") val bookmarkData: com.projectu.shared.data.remote.dto.pixiv.BookmarkData? = null,
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
 * 标题说明翻译
 */
@Serializable
data class TitleCaptionTranslation(
    val workTitle: String? = null,
    val workCaption: String? = null
)

/**
 * 小说 API
 * 提供小说的查询、搜索、推荐等功能
 */
class NovelApi(private val client: PixivApiClient) {

    /**
     * 查询小说详情
     * @param novelId 小说ID
     */
    suspend fun getDetail(novelId: Long): PixivResponse<NovelDetailBody> {
        return client.get("/ajax/novel/$novelId")
    }

    /**
     * 查询小说收藏状态
     * @param novelId 小说ID
     */
    suspend fun getBookmarkData(novelId: Long): PixivResponse<NovelBookmarkStatusBody> {
        return client.get("/ajax/novel/$novelId/bookmarkData")
    }

    /**
     * 搜索小说
     * @param keyword 关键词（需要UTF-8编码，空格替换为%20）
     * @param searchMode 搜索模式：s_tag(标签部分匹配), s_tag_full(标签完全匹配), s_tc(标题说明)
     * @param order 排序：date_d(从新到旧), date(从旧到新)
     * @param mode 模式：all, safe, r18
     * @param page 页码
     * @param scd 发布时间起始（格式：yyyy-MM-dd）
     * @param ecd 发布时间结束（格式：yyyy-MM-dd）
     */
    suspend fun search(
        keyword: String,
        searchMode: String = "s_tag",
        order: String = "date_d",
        mode: String = "all",
        page: Int = 1,
        scd: String? = null,
        ecd: String? = null
    ): PixivResponse<NovelSearchBody> {
        // URL 编码关键词
        val encodedKeyword = keyword.encodeURLPath()
        
        val params = mutableMapOf<String, Any?>(
            "word" to keyword,
            "s_mode" to searchMode,
            "order" to order,
            "mode" to mode,
            "p" to page
        )
        scd?.let { params["scd"] = it }
        ecd?.let { params["ecd"] = it }

        return client.get("/ajax/search/novels/$encodedKeyword", params)
    }

    /**
     * 发现小说
     * @param mode 模式：all, safe, r18
     * @param limit 返回数量
     * @param sampleNovelId 参考小说ID（可选）
     */
    suspend fun getDiscovery(
        mode: String = "all",
        limit: Int = 100,
        sampleNovelId: Long? = null
    ): PixivResponse<DiscoveryBody> {
        val params = mutableMapOf<String, Any?>(
            "mode" to mode,
            "limit" to limit
        )
        sampleNovelId?.let { params["sampleNovelId"] = it }

        return client.get("/ajax/discovery/novels", params)
    }

    /**
     * 查询关注作者的最新小说
     * @param mode 模式：all, r18
     * @param page 页码
     */
    suspend fun getFollowLatest(
        mode: String = "all",
        page: Int = 1
    ): PixivResponse<FollowLatestBody> {
        return client.get("/ajax/follow_latest/novel", mapOf(
            "mode" to mode,
            "p" to page
        ))
    }
}
