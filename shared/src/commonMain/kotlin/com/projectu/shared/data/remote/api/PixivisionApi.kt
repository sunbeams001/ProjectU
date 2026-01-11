package com.projectu.shared.data.remote.api

import com.fleeksoft.ksoup.Ksoup
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticle
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticleDetail
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticleListResponse
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArtwork
import com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.statement.bodyAsText

/**
 * Pixivision API
 * 通过解析HTML获取Pixivision文章列表
 * 
 * Pixivision是Pixiv的附属网站，提供各种主题的插画、漫画特辑
 * 网站地址：https://www.pixivision.net/
 */
class PixivisionApi(private val client: PixivApiClient) {
    
    companion object {
        private const val PIXIVISION_HOST = "https://www.pixivision.net"
    }
    
    /**
     * 获取Pixivision文章列表
     * 
     * @param category 类别 (illustration/manga)
     * @param lang 语言代码 (en, zh, zh-tw, ko, ja)
     * @param page 页码（从1开始）
     * @return 文章列表响应
     * 
     * 📝 接口说明：
     * - 端点：/{lang}/c/{category}
     * - 返回格式：HTML
     * - 支持分页：?p={page}
     * 
     * 示例 URL：
     * - https://www.pixivision.net/zh/c/illustration
     * - https://www.pixivision.net/zh/c/manga?p=2
     */
    suspend fun getArticleList(
        category: PixivisionCategory,
        lang: String = "zh",
        page: Int = 1
    ): PixivisionArticleListResponse {
        val url = "$PIXIVISION_HOST/$lang/c/${category.path}"
        val cookieLang = lang.replace("-", "_")
        
        val html = client.httpClient.get(url) {
            if (page > 1) {
                parameter("p", page)
            }
            header("Cookie", "user_lang=$cookieLang")
        }.bodyAsText()
        
        val articles = parseArticlesWithKsoup(html)
        
        return PixivisionArticleListResponse(articles = articles)
    }
    
    /**
     * 使用Ksoup解析HTML中的文章列表
     */
    private fun parseArticlesWithKsoup(html: String): List<PixivisionArticle> {
        val articles = mutableListOf<PixivisionArticle>()
        
        try {
            val doc = Ksoup.parse(html)
            val articleElements = doc.select("li.article-card-container")
            
            articleElements.forEach { li ->
                try {
                    val linkElement = li.selectFirst("a[href*='/a/']") ?: return@forEach
                    val url = linkElement.attr("href")
                    val id = url.substringAfterLast("/a/").substringBefore("?")
                    
                    val thumbnailElement = li.selectFirst("div._thumbnail")
                    val thumbnailStyle = thumbnailElement?.attr("style") ?: ""
                    val thumbnailUrl = if (thumbnailStyle.isNotEmpty()) {
                        val urlStart = thumbnailStyle.indexOf("url(") + 4
                        val urlEnd = thumbnailStyle.indexOf(")", urlStart)
                        if (urlStart > 3 && urlEnd > urlStart) {
                            thumbnailStyle.substring(urlStart, urlEnd).trim()
                        } else {
                            ""
                        }
                    } else {
                        ""
                    }
                    
                    val categoryElement = li.selectFirst("span.arc__thumbnail-label")
                    val category = categoryElement?.text() ?: ""
                    
                    val titleElement = li.selectFirst("h2.arc__title a")
                    val title = titleElement?.text() ?: ""
                    
                    val dateElement = li.selectFirst("time")
                    val publishDate = dateElement?.attr("datetime") ?: ""
                    
                    val tagElements = li.select("div.tls__list-item")
                    val tags = tagElements.map { it.text().trim() }.filter { it.isNotEmpty() }
                    
                    articles.add(
                        PixivisionArticle(
                            id = id,
                            title = title,
                            url = url,
                            thumbnailUrl = thumbnailUrl,
                            category = category,
                            tags = tags,
                            publishDate = publishDate
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        return articles
    }
    
    /**
     * 获取Pixivision文章详情
     * 
     * @param articleId 文章ID
     * @param lang 语言代码 (en, zh, zh-tw, ko, ja)
     * @return 文章详情
     * 
     * 📝 接口说明：
     * - 端点：/{lang}/a/{articleId}
     * - 返回格式：HTML
     * 
     * 示例 URL：
     * - https://www.pixivision.net/zh/a/11373
     */
    suspend fun getArticleDetail(
        articleId: String,
        lang: String = "zh"
    ): PixivisionArticleDetail {
        val url = "$PIXIVISION_HOST/$lang/a/$articleId"
        val cookieLang = lang.replace("-", "_")
        
        val html = client.httpClient.get(url) {
            header("Cookie", "user_lang=$cookieLang")
        }.bodyAsText()
        
        val detail = parseArticleDetailWithKsoup(html, articleId)
        
        return detail
    }
    
    /**
     * 使用Ksoup解析HTML中的文章详情
     */
    private fun parseArticleDetailWithKsoup(html: String, articleId: String): PixivisionArticleDetail {
        try {
            val doc = Ksoup.parse(html)
            
            val titleElement = doc.selectFirst("h1.am__title")
            val title = titleElement?.text() ?: ""
            
            val categoryElement = doc.selectFirst("span._category-label")
            val category = categoryElement?.text() ?: ""
            
            val dateElement = doc.selectFirst("time._date")
            val publishDate = dateElement?.attr("datetime") ?: ""
            
            val coverElement = doc.selectFirst("img.aie__image")
            val coverImageUrl = coverElement?.attr("src") ?: ""
            
            // 解析简介部分，支持多种HTML结构
            // 只选择第一个段落块作为简介，避免包含后续的活动宣传和广告内容
            // 1. 先尝试从第一个 div._feature-article-body__paragraph 中获取
            // 2. 如果为空，尝试从 meta 标签中获取
            val firstParagraphBlock = doc.selectFirst("div._feature-article-body__paragraph")
            
            val description = if (firstParagraphBlock != null) {
                // 在第一个段落块中查找 div 或 p 标签
                val descriptionDivs = firstParagraphBlock.select("div.fab__paragraph > div")
                val descriptionParas = firstParagraphBlock.select("div.fab__paragraph > p")
                
                when {
                    descriptionDivs.isNotEmpty() -> {
                        // 新版格式：使用 <div> 标签
                        descriptionDivs.joinToString("\n") { it.text().trim() }
                            .trim()
                            .takeIf { it.isNotEmpty() } ?: ""
                    }
                    descriptionParas.isNotEmpty() -> {
                        // 旧版格式：使用 <p> 标签
                        descriptionParas.joinToString("\n") { it.text().trim() }
                            .trim()
                            .takeIf { it.isNotEmpty() } ?: ""
                    }
                    else -> {
                        // 后备方案：从 meta 标签获取
                        doc.selectFirst("meta[property=og:description]")?.attr("content") ?: ""
                    }
                }
            } else {
                // 如果找不到段落块，从 meta 标签获取
                doc.selectFirst("meta[property=og:description]")?.attr("content") ?: ""
            }
            
            val artworks = mutableListOf<PixivisionArtwork>()
            val workElements = doc.select("div._feature-article-body__pixiv_illust div.am__work")
            
            workElements.forEach { workElement ->
                try {
                    val artworkLinkElement = workElement.selectFirst("h3.am__work__title a")
                    val artworkUrl = artworkLinkElement?.attr("href") ?: ""
                    val artworkId = artworkUrl.substringAfter("/artworks/")
                        .substringBefore("?")
                        .trim()
                    val artworkTitle = artworkLinkElement?.text() ?: ""
                    
                    val artworkImageElement = workElement.selectFirst("img.am__work__illust")
                    val artworkImageUrl = artworkImageElement?.attr("src") ?: ""
                    
                    val authorLinkElement = workElement.selectFirst("a.author-img-container, p.am__work__user-name a")
                    val authorUrl = authorLinkElement?.attr("href") ?: ""
                    val authorId = authorUrl.substringAfter("/users/")
                        .substringBefore("?")
                        .trim()
                    val authorName = authorLinkElement?.text() ?: ""
                    
                    val authorAvatarElement = workElement.selectFirst("img.am__work__uesr-icon")
                    val authorAvatarUrl = authorAvatarElement?.attr("src") ?: ""
                    
                    artworks.add(
                        PixivisionArtwork(
                            artworkId = artworkId,
                            artworkTitle = artworkTitle,
                            artworkImageUrl = artworkImageUrl,
                            authorId = authorId,
                            authorName = authorName,
                            authorAvatarUrl = authorAvatarUrl
                        )
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            
            return PixivisionArticleDetail(
                id = articleId,
                title = title,
                description = description,
                coverImageUrl = coverImageUrl,
                category = category,
                publishDate = publishDate,
                artworks = artworks
            )
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
