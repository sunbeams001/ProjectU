package com.projectu.shared.data.remote.mapper

import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticle
import com.projectu.shared.data.remote.dto.pixivision.PixivisionArticleDetail
import com.projectu.shared.domain.model.pixivision.PixivisionArticleInfo
import com.projectu.shared.domain.model.pixivision.PixivisionDetail
import com.projectu.shared.domain.model.pixivision.PixivisionArtworkAuthor

/**
 * Pixivision DTO → Domain Model 映射器
 */

/**
 * 将 PixivisionArticle DTO 转换为 Domain Model
 */
fun PixivisionArticle.toDomainModel(): PixivisionArticleInfo {
    return PixivisionArticleInfo(
        id = id,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl,
        category = category,
        tags = tags,
        publishDate = publishDate
    )
}

/**
 * 将 PixivisionArticle 列表转换为 Domain Model 列表
 */
fun List<PixivisionArticle>.toDomainModelList(): List<PixivisionArticleInfo> {
    return map { it.toDomainModel() }
}

/**
 * 将 PixivisionArticleDetail DTO 转换为 Domain Model
 */
fun PixivisionArticleDetail.toDomainModel(language: String = "zh"): PixivisionDetail {
    // 根据ID和语言构造Pixivision URL
    val articleUrl = "https://www.pixivision.net/$language/a/$id"
    
    return PixivisionDetail(
        id = id,
        title = title,
        description = description,
        url = articleUrl,
        coverImageUrl = coverImageUrl,
        category = category,
        publishDate = publishDate,
        artworkIds = artworks.map { it.artworkId },
        artworkAuthors = artworks.associate { artwork ->
            artwork.artworkId to PixivisionArtworkAuthor(
                authorId = artwork.authorId,
                authorName = artwork.authorName,
                authorAvatarUrl = artwork.authorAvatarUrl
            )
        }
    )
}
