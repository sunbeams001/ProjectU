package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.illust.DiscoveryBody
import com.projectu.shared.data.remote.dto.illust.FollowLatestBody
import com.projectu.shared.data.remote.dto.novel.NovelBookmarkStatusBody
import com.projectu.shared.data.remote.dto.novel.NovelDetailBody
import com.projectu.shared.data.remote.dto.novel.NovelSearchBody
import io.ktor.http.encodeURLPath

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
