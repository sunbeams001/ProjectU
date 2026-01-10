package com.projectu.shared.data.remote.dto.pixivision

import kotlinx.serialization.Serializable

/**
 * Pixivision 文章列表响应
 */
@Serializable
data class PixivisionArticleListResponse(
    val articles: List<PixivisionArticle>
)

/**
 * Pixivision 文章
 */
@Serializable
data class PixivisionArticle(
    /**
     * 文章ID (从URL中提取，如 "11311")
     */
    val id: String,
    
    /**
     * 文章标题
     */
    val title: String,
    
    /**
     * 文章详情URL (相对路径，如 "/zh/a/11311")
     */
    val url: String,
    
    /**
     * 缩略图URL
     */
    val thumbnailUrl: String,
    
    /**
     * 类别 (如 "插画"、"漫画")
     */
    val category: String,
    
    /**
     * 标签列表
     */
    val tags: List<String>,
    
    /**
     * 发布日期 (格式: "2026-01-09")
     */
    val publishDate: String
)

/**
 * Pixivision 文章详情响应
 */
@Serializable
data class PixivisionArticleDetail(
    /**
     * 文章ID
     */
    val id: String,
    
    /**
     * 文章标题
     */
    val title: String,
    
    /**
     * 简介/描述
     */
    val description: String,
    
    /**
     * 封面图片URL
     */
    val coverImageUrl: String,
    
    /**
     * 类别 (如 "插画"、"漫画")
     */
    val category: String,
    
    /**
     * 发布日期 (格式: "2026-01-09")
     */
    val publishDate: String,
    
    /**
     * 文章中的作品列表
     */
    val artworks: List<PixivisionArtwork>
)

/**
 * Pixivision 文章中的作品项
 */
@Serializable
data class PixivisionArtwork(
    /**
     * 作品ID
     */
    val artworkId: String,
    
    /**
     * 作品标题
     */
    val artworkTitle: String,
    
    /**
     * 作品图片URL
     */
    val artworkImageUrl: String,
    
    /**
     * 作者ID
     */
    val authorId: String,
    
    /**
     * 作者名
     */
    val authorName: String,
    
    /**
     * 作者头像URL
     */
    val authorAvatarUrl: String
)

/**
 * Pixivision 类别枚举
 */
enum class PixivisionCategory(val path: String, val displayName: String) {
    ILLUSTRATION("illustration", "插画"),
    MANGA("manga", "漫画");
    
    companion object {
        fun fromPath(path: String): PixivisionCategory? {
            return entries.find { it.path == path }
        }
    }
}
