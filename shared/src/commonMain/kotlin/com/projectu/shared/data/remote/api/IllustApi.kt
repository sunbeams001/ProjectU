package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.BookmarkData
import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.illust.DiscoveryBody
import com.projectu.shared.data.remote.dto.illust.IllustDetailBody
import com.projectu.shared.data.remote.dto.illust.IllustRecommendBody
import com.projectu.shared.data.remote.dto.illust.IllustRecommendInitBody
import com.projectu.shared.data.remote.dto.illust.LikeBody
import com.projectu.shared.data.remote.dto.illust.PageInfo
import com.projectu.shared.data.remote.dto.illust.UgoiraMetaBody

/**
 * 插画作品 API
 * 提供插画的查询、搜索、推荐等功能
 */
class IllustApi(private val client: PixivApiClient) {

    /**
     * 查询插画详情
     * @param pid 作品ID
     */
    suspend fun getDetail(pid: Long): PixivResponse<IllustDetailBody> {
        return client.get("/ajax/illust/$pid")
    }

    /**
     * 查询插画收藏状态
     * @param pid 作品ID
     */
    suspend fun getBookmarkData(pid: Long): PixivResponse<BookmarkData> {
        return client.get("/ajax/illust/$pid/bookmarkData")
    }

    /**
     * 查询多页插画的所有页面详情
     * 
     * 用于获取多页作品（漫画）的每一页原图 URL
     * 
     * @param pid 作品ID
     * @return 页面列表，每个元素包含该页的各种尺寸图片 URL
     */
    suspend fun getPages(pid: Long): PixivResponse<List<PageInfo>> {
        return client.get("/ajax/illust/$pid/pages")
    }

    /**
     * 查询Ugoira动图元数据
     * @param pid 动图作品ID
     */
    suspend fun getUgoiraMeta(pid: Long): PixivResponse<UgoiraMetaBody> {
        return client.get("/ajax/illust/$pid/ugoira_meta")
    }

    /**
     * 发现插画
     * @param mode 模式：all, safe, r18
     * @param limit 返回数量
     * @param sampleIllustId 参考作品ID（可选）
     */
    suspend fun getDiscovery(
        mode: String = "all",
        limit: Int = 100,
        sampleIllustId: Long? = null
    ): PixivResponse<DiscoveryBody> {
        val params = mutableMapOf<String, Any?>(
            "mode" to mode,
            "limit" to limit
        )
        sampleIllustId?.let { params["sampleIllustId"] = it }

        return client.get("/ajax/discovery/artworks", params)
    }

    /**
     * 查询推荐作品（初始化）
     * @param pid 基准作品ID
     * @param limit 返回数量
     */
    suspend fun getRecommendInit(
        pid: Long,
        limit: Int = 18
    ): PixivResponse<IllustRecommendInitBody> {
        return client.get("/ajax/illust/$pid/recommend/init", mapOf(
            "limit" to limit
        ))
    }

    /**
     * 查询推荐作品
     * @param illustIds 基准作品ID列表
     */
    suspend fun getRecommendIllusts(
        illustIds: List<Long>
    ): PixivResponse<IllustRecommendBody> {
        return client.get("/ajax/illust/recommend/illusts", mapOf(
            "illust_ids[]" to illustIds
        ))
    }

    /**
     * 点赞插画
     * @param pid 作品ID
     */
    suspend fun postLike(pid: Long): PixivResponse<LikeBody> {
        return client.postJson("/ajax/illusts/like", mapOf(
            "illust_id" to pid
        ))
    }
}

