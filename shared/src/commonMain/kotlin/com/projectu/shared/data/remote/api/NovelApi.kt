package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.illust.DiscoveryBody
import com.projectu.shared.data.remote.dto.novel.NovelBookmarkStatusBody
import com.projectu.shared.data.remote.dto.novel.NovelDetailBody

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

}
