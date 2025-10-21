package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.pixiv.RankingResponse
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter

/**
 * 排行榜 API
 * 提供各类排行榜查询功能
 */
class RankingApi(private val client: PixivApiClient) {

    /**
     * 查询插画排行榜
     * @param mode 模式：daily(日榜), weekly(周榜), monthly(月榜), rookie(新人), 
     *             original(原创), male(男性向), female(女性向), 
     *             daily_r18(R18日榜), weekly_r18(R18周榜), male_r18(R18男性向), female_r18(R18女性向)
     * @param page 页码
     * @param content 内容类型：all(全部), illust(插画), manga(漫画), ugoira(动图)
     * @param date 日期（格式：yyyyMMdd，可选）
     */
    suspend fun getIllustRanking(
        mode: String = "daily",
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ): RankingResponse {
        val params = mutableMapOf<String, Any?>(
            "mode" to mode,
            "p" to page,
            "format" to "json",
            "content" to content
        )
        date?.let { params["date"] = it }

        // 排行榜API直接返回RankingResponse，不包装在PixivResponse中
        // 需要访问 client 内部的 httpClient，所以需要暴露它
        return client.get<RankingResponse>("/ranking.php", params).body ?: throw IllegalStateException("排行榜数据为空")
    }

    /**
     * 获取日榜
     */
    suspend fun getDailyRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("daily", page, content, date)

    /**
     * 获取周榜
     */
    suspend fun getWeeklyRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("weekly", page, content, date)

    /**
     * 获取月榜
     */
    suspend fun getMonthlyRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("monthly", page, content, date)

    /**
     * 获取新人榜
     */
    suspend fun getRookieRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("rookie", page, content, date)

    /**
     * 获取原创榜
     */
    suspend fun getOriginalRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("original", page, content, date)

    /**
     * 获取男性向榜
     */
    suspend fun getMaleRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("male", page, content, date)

    /**
     * 获取女性向榜
     */
    suspend fun getFemaleRanking(
        page: Int = 1,
        content: String = "all",
        date: String? = null
    ) = getIllustRanking("female", page, content, date)
}

