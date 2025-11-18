package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.PixivResponse
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 小说系列详情
 */
@Serializable
data class NovelSeriesBody(
    val id: String,
    val title: String,
    val description: String,
    @SerialName("userId") val userId: String,
    @SerialName("userName") val userName: String,
    @SerialName("createDate") val createDate: String,
    @SerialName("updateDate") val updateDate: String,
    @SerialName("contentCount") val contentCount: Int,
    @SerialName("total") val total: Int,
    @SerialName("watchCount") val watchCount: Int? = null,
    @SerialName("isWatched") val isWatched: Boolean = false,
    @SerialName("isNotifying") val isNotifying: Boolean = false
)

/**
 * 小说系列内容
 */
@Serializable
data class NovelSeriesContentBody(
    @SerialName("seriesContents") val seriesContents: List<NovelSeriesContent> = emptyList(),
    @SerialName("page") val page: NovelSeriesPage
)

/**
 * 小说系列内容项
 */
@Serializable
data class NovelSeriesContent(
    val id: String,
    @SerialName("seriesId") val seriesId: String,
    @SerialName("seriesOrder") val seriesOrder: Int,
    @SerialName("novelId") val novelId: String,
    val title: String,
    @SerialName("textCount") val textCount: Int,
    @SerialName("createDate") val createDate: String
)

/**
 * 小说系列分页信息
 */
@Serializable
data class NovelSeriesPage(
    val total: Int,
    val limit: Int,
    val offset: Int,
    val next: Int? = null,
    val prev: Int? = null
)

/**
 * 小说系列标题响应
 */
@Serializable
data class NovelSeriesTitlesResponse(
    @SerialName("seriesContents") val seriesContents: List<NovelSeriesTitle> = emptyList()
)

/**
 * 小说系列标题项
 */
@Serializable
data class NovelSeriesTitle(
    val id: String,
    @SerialName("seriesId") val seriesId: String,
    @SerialName("seriesOrder") val seriesOrder: Int,
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
    suspend fun getTitles(seriesId: Long): PixivResponse<NovelSeriesTitlesResponse> {
        return client.get("/ajax/novel/series/$seriesId/content_titles")
    }
}
