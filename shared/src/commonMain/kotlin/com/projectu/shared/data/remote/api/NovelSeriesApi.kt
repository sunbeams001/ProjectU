package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.novel.NovelSeriesBody
import com.projectu.shared.data.remote.dto.novel.NovelSeriesContent
import com.projectu.shared.data.remote.dto.novel.NovelSeriesContentBody
import com.projectu.shared.data.remote.dto.novel.NovelSeriesCover
import com.projectu.shared.data.remote.dto.novel.NovelSeriesCoverUrls
import com.projectu.shared.data.remote.dto.novel.NovelSeriesFirstEpisode
import com.projectu.shared.data.remote.dto.novel.NovelSeriesInfo
import com.projectu.shared.data.remote.dto.novel.NovelSeriesPage
import com.projectu.shared.data.remote.dto.novel.NovelSeriesThumbnails
import com.projectu.shared.data.remote.dto.novel.NovelSeriesTitle
import com.projectu.shared.data.remote.dto.novel.NovelThumbnail
import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject

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
