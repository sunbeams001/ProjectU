package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.common.PixivResponse
import com.projectu.shared.data.remote.dto.illust_series.IllustSeriesBody
import kotlinx.serialization.json.buildJsonObject

/**
 * 漫画系列 API
 * 提供漫画系列的查询和追更功能
 */
class IllustSeriesApi(private val client: PixivApiClient) {

    /**
     * 查询漫画系列详情
     * @param seriesId 系列ID
     * @param page 页码（从1开始）
     */
    suspend fun getDetail(seriesId: Long, page: Int = 1): PixivResponse<IllustSeriesBody> {
        return client.get("/ajax/series/$seriesId", mapOf("p" to page))
    }

    /**
     * 加入追更列表
     * @param seriesId 系列ID
     */
    suspend fun watch(seriesId: Long): PixivResponse<List<String>> {
        return client.postJson("/ajax/illust/series/$seriesId/watch", buildJsonObject {})
    }

    /**
     * 移除追更
     * @param seriesId 系列ID
     */
    suspend fun unwatch(seriesId: Long): PixivResponse<List<String>> {
        return client.postJson("/ajax/illust/series/$seriesId/unwatch", buildJsonObject {})
    }
}
