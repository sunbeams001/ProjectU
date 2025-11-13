package com.projectu.shared.data.remote.parser

import com.projectu.shared.data.remote.dto.novel.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.intOrNull

/**
 * 小说排行榜JSON解析器
 * 
 * Pixiv小说排行榜页面使用Next.js渲染，数据存储在 `<script id="__NEXT_DATA__">` 标签中的JSON里
 * 
 * JSON结构：
 * {
 *   "props": {
 *     "pageProps": {
 *       "assign": {
 *         "display_a": {
 *           "rank_a": [ ... 小说数据数组 ... ]
 *         }
 *       }
 *     }
 *   }
 * }
 */
object NovelRankingParser {
    
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 解析小说排行榜HTML响应（从Next.js __NEXT_DATA__ 提取JSON）
     * 
     * @param html HTML响应体
     * @param mode 排行榜模式（daily, weekly等）
     * @return 解析后的排行榜数据
     */
    fun parseNovelRanking(html: String, mode: String = "daily"): NovelRankingResponse {
        println("=== 开始解析小说排行榜 ===")
        println("HTML长度: ${html.length} 字符")
        
        // 从HTML中提取 __NEXT_DATA__ JSON
        val jsonData = extractNextData(html)
        println("成功提取JSON数据，长度: ${jsonData.length} 字符")
        
        // 解析JSON
        val jsonObject = json.parseToJsonElement(jsonData).jsonObject
        
        // 提取排行榜数据
        val novels = extractNovelsFromJson(jsonObject)
        println("成功提取小说数量: ${novels.size}")
        
        return NovelRankingResponse(
            mode = mode,
            date = extractDateFromJson(jsonObject),
            currentPage = 1, // TODO: 从JSON提取
            totalPages = 1,  // TODO: 从JSON提取
            rankRange = "",  // TODO: 从JSON提取
            novels = novels,
            nextPageUrl = null,
            previousPageUrl = null
        )
    }
    
    /**
     * 从HTML中提取 __NEXT_DATA__ JSON字符串
     */
    private fun extractNextData(html: String): String {
        val regex = Regex("""<script id="__NEXT_DATA__" type="application/json">(.*?)</script>""", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(html)
            ?: throw IllegalArgumentException("未找到 __NEXT_DATA__ JSON数据")
        
        return match.groupValues[1]
    }
    
    /**
     * 从JSON中提取日期
     */
    private fun extractDateFromJson(jsonObject: JsonObject): String {
        return try {
            // TODO: 从实际JSON结构中提取日期
            ""
        } catch (e: Exception) {
            println("提取日期失败: ${e.message}")
            ""
        }
    }
    
    /**
     * 从JSON中提取小说列表
     */
    private fun extractNovelsFromJson(jsonObject: JsonObject): List<NovelRankingItem> {
        return try {
            val rankArray = jsonObject
                .jsonObject["props"]!!
                .jsonObject["pageProps"]!!
                .jsonObject["assign"]!!
                .jsonObject["display_a"]!!
                .jsonObject["rank_a"]!!
                .jsonArray
            
            rankArray.mapNotNull { novelElement ->
                try {
                    parseNovelFromJson(novelElement.jsonObject)
                } catch (e: Exception) {
                    println("解析小说失败: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }
        } catch (e: Exception) {
            println("提取小说列表失败: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 从JSON对象解析单个小说信息
     */
    private fun parseNovelFromJson(novel: JsonObject): NovelRankingItem {
        val rank = novel["rank"]?.jsonPrimitive?.intOrNull ?: 0
        val novelId = novel["id"]?.jsonPrimitive?.content ?: ""
        val title = novel["title"]?.jsonPrimitive?.content ?: ""
        
        // 作者信息
        val userName = novel["user_name"]?.jsonPrimitive?.content ?: ""
        val userId = novel["user_id"]?.jsonPrimitive?.content ?: ""
        val profileImg = novel["profile_img"]?.jsonPrimitive?.content ?: ""
        
        // 封面
        val coverUrl = novel["url"]?.jsonPrimitive?.content ?: ""
        
        // 字符数和书签数
        val characterCount = novel["character_count"]?.jsonPrimitive?.intOrNull ?: 0
        val bookmarkCount = novel["bookmark_count"]?.jsonPrimitive?.intOrNull ?: 0
        
        // 标签 (tag_a 是数组)
        val tags = novel["tag_a"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
        
        // 简介 (comment 字段)
        val caption = novel["comment"]?.jsonPrimitive?.content ?: ""
        
        // 系列信息
        val series = extractSeriesFromJson(novel)
        
        // 书签信息
        val isBookmarked = novel["is_bookmarked"]?.jsonPrimitive?.content == "true"
        val bookmarkId = novel["bookmark_id"]?.jsonPrimitive?.content
        val bookmarkRestrict = novel["bookmark_restrict"]?.jsonPrimitive?.content
        val marker = novel["marker"]?.jsonPrimitive?.intOrNull
        
        println("✅ 解析小说 rank=$rank, id=$novelId, title=$title, tags=${tags.size}, bookmarks=$bookmarkCount, isBookmarked=$isBookmarked, marker=$marker")
        
        return NovelRankingItem(
            rank = rank,
            novelId = novelId,
            title = title,
            author = AuthorInfo(
                userId = userId,
                userName = userName,
                profileImageUrl = profileImg,
                novelListUrl = "/users/$userId/novels"
            ),
            coverImageUrl = coverUrl,
            characterCount = characterCount,
            bookmarkCount = bookmarkCount,
            tags = tags,
            caption = caption,
            series = series,
            novelUrl = "/novel/show.php?id=$novelId",
            isBookmarked = isBookmarked,
            bookmarkId = bookmarkId,
            bookmarkRestrict = bookmarkRestrict,
            marker = marker
        )
    }
    
    /**
     * 从JSON对象提取系列信息
     */
    private fun extractSeriesFromJson(novel: JsonObject): SeriesInfo? {
        return try {
            val seriesId = novel["series_id"]?.jsonPrimitive?.content
            val seriesTitle = novel["series_title"]?.jsonPrimitive?.content
            
            if (seriesId != null && seriesTitle != null) {
                SeriesInfo(
                    seriesId = seriesId,
                    seriesTitle = seriesTitle,
                    seriesUrl = "/novel/series/$seriesId"
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
