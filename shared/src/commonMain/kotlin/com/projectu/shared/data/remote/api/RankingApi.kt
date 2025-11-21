package com.projectu.shared.data.remote.api

import com.projectu.shared.data.remote.dto.ranking.NovelRankingResponse
import com.projectu.shared.data.remote.dto.ranking.RankingResponse
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.parser.NovelRankingParser
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText

/**
 * 平台特定的文件保存功能
 */
internal expect fun saveHtmlToFile(html: String, filename: String)

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

        // 排行榜API直接返回RankingResponse，不包装在PixivResponse中
        return client.get<RankingResponse>("/ranking.php", params).body ?: throw IllegalStateException("插画排行榜数据为空")
    }

    /**
     * 查询小说排行榜
     * @param mode 排行榜模式（与插画排行榜相同）
     * @param page 页码
     * @param content 内容类型（小说排行榜可能不支持此参数，但保持接口一致性）
     * @param date 日期（格式：yyyyMMdd，可选）
     * @return NovelRankingResponse 解析后的小说排行榜数据
     * 
     * ⚠️ 注意：
     * - 小说排行榜返回HTML格式，不支持JSON（即使添加format=json也无效）
     * - 端点为 /novel/ranking.php
     * - 内部会自动解析HTML提取小说信息
     */
    suspend fun getNovelRanking(
        mode: RankingMode = RankingMode.DAILY,
        page: Int = 1,
        content: RankingContent = RankingContent.ALL,
        date: String? = null
    ): NovelRankingResponse {
        // 构建URL参数
        val urlBuilder = StringBuilder("${client.host}/novel/ranking.php?lang=${client.langProvider()}")
        urlBuilder.append("&mode=${mode.value}")
        urlBuilder.append("&p=$page")
        urlBuilder.append("&content=${content.value}")
        date?.let { urlBuilder.append("&date=$it") }

        // 直接使用 HttpClient 获取HTML文本
        val html = client.httpClient.get(urlBuilder.toString()) {
            header(PixivApiClient.HEADER_REFERER, PixivApiClient.DEFAULT_HOST)
            header(PixivApiClient.HEADER_COOKIE, client.cookie)
        }.bodyAsText()
        
        // 调试：将HTML保存到文件（仅用于调试，正式环境不需要）
        // try {
        //     saveHtmlToFile(html, "novel_ranking_response.html")
        //     println("✅ HTML响应已保存到文件")
        // } catch (e: Exception) {
        //     println("⚠️ 保存HTML文件失败: ${e.message}")
        // }
        
        // 使用解析器解析HTML
        return NovelRankingParser.parseNovelRanking(html, mode.value)
    }
}


