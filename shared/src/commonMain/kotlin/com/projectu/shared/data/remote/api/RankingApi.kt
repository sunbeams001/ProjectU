package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.ranking.NovelRankingBody
import com.projectu.shared.data.remote.dto.ranking.RankingResponse
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingMode
import io.ktor.client.call.body
import io.ktor.client.request.get

/**
 * 排行榜 API
 * 提供插画和小说排行榜查询功能
 */
class RankingApi(private val client: PixivApiClient) {

    /**
     * 查询插画排行榜
     * @param mode 排行榜模式
     * @param page 页码
     * @param content 内容类型
     * @param date 日期（格式：yyyyMMdd，可选）
     * @return RankingResponse 排行榜响应数据
     * 
     * 📝 接口说明：
     * - 端点：/ranking.php
     * - 返回格式：JSON（直接返回RankingResponse对象，不包装在PixivResponse中）
     * - 支持分页：p参数
     * 
     * 示例 URL：
     * https://www.pixiv.net/ranking.php?mode=daily&content=all&p=1&format=json&lang=zh
     */
    suspend fun getIllustRanking(
        mode: RankingMode = RankingMode.DAILY,
        page: Int = 1,
        content: RankingContent = RankingContent.ALL,
        date: String? = null
    ): RankingResponse {
        val params = mutableMapOf<String, Any?>(
            "mode" to mode.value,
            "p" to page,
            "format" to "json",
            "content" to content.value
        )
        date?.let { params["date"] = it }

        // 排行榜API直接返回RankingResponse，不包装在PixivResponse中，使用getRaw方法
        return client.getRaw<RankingResponse>("/ranking.php", params)
    }

    /**
     * 查询小说排行榜（JSON 接口）
     * 
     * @param mode 排行榜模式
     * @param page 页码
     * @param content 内容类型
     * @param date 日期（格式：yyyyMMdd，可选）
     * @return NovelRankingBody 小说排行榜数据
     * 
     * 📝 接口说明：
     * - 端点：/ajax/ranking/novel
     * - 返回格式：JSON（包装在PixivResponse中）
     * - 支持分页：p参数
     * - 支持所有小说排行榜模式
     * 
     * 示例 URL：
     * https://www.pixiv.net/ajax/ranking/novel?mode=daily&content=novel&p=1&lang=zh
     */
    suspend fun getNovelRankingJson(
        mode: RankingMode = RankingMode.DAILY,
        page: Int = 1,
        content: RankingContent = RankingContent.NOVEL,
        date: String? = null
    ): NovelRankingBody {
        val params = mutableMapOf<String, Any?>(
            "mode" to mode.value,
            "content" to content.value,
            "p" to page
        )
        date?.let { params["date"] = it }
        
        // 小说排行榜JSON API返回标准的PixivResponse格式
        val response = client.get<NovelRankingBody>("/ajax/ranking/novel", params)
        return response.body ?: throw IllegalStateException("小说排行榜数据为空")
    }
}


