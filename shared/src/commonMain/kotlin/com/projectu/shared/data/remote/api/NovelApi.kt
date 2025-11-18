package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.BookmarkData
import com.projectu.shared.data.remote.dto.pixiv.DiscoveryBody
import com.projectu.shared.data.remote.dto.pixiv.FollowLatestBody
import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import io.ktor.http.encodeURLPath
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    @SerialName("isOriginal") val isOriginal: Boolean,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    val tags: List<String> = emptyList()
)

/**
 * 小说搜索响应体
 */
@Serializable
data class NovelSearchBody(
    val novels: List<NovelSearchItem> = emptyList(),
    val total: Int = 0
)

/**
 * 小说搜索项
 */
@Serializable
data class NovelSearchItem(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("createDate") val createDate: String,
    @SerialName("bookmarkCount") val bookmarkCount: Int,
    val tags: List<String> = emptyList()
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
    suspend fun getBookmarkData(novelId: Long): PixivResponse<BookmarkData> {
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

    /**
     * 获取新作小说
     * @param lastId 最后一个小说ID（用于分页）
     * @param limit 返回数量
     * @param mode 模式：all, r18
     */
    suspend fun getNew(
        lastId: String? = null,
        limit: Int = 20,
        mode: String = "all"
    ): PixivResponse<FollowLatestBody> {
        val params = mutableMapOf<String, Any?>(
            "limit" to limit,
            "mode" to mode
        )
        lastId?.let { params["lastId"] = it }

        return client.get("/ajax/novel/new", params)
    }
}
