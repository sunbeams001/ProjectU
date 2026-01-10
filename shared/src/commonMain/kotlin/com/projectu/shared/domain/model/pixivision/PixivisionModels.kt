package com.projectu.shared.domain.model.pixivision

/**
 * Pixivision 文章信息 (Domain Model)
 * 用于列表展示
 */
data class PixivisionArticleInfo(
    val id: String,
    val title: String,
    val url: String,
    val thumbnailUrl: String,
    val category: String,
    val tags: List<String>,
    val publishDate: String
)

/**
 * Pixivision 详情 (Domain Model)
 * 用于详情页展示
 */
data class PixivisionDetail(
    val id: String,
    val title: String,
    val description: String,
    val url: String,
    val coverImageUrl: String,
    val category: String,
    val publishDate: String,
    val artworkIds: List<String>,
    val artworkAuthors: Map<String, PixivisionArtworkAuthor>
)

/**
 * Pixivision 作品作者信息
 */
data class PixivisionArtworkAuthor(
    val authorId: String,
    val authorName: String,
    val authorAvatarUrl: String
)
