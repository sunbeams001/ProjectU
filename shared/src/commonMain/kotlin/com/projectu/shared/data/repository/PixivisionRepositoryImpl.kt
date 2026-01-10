package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory
import com.projectu.shared.data.remote.mapper.toDomainModel
import com.projectu.shared.data.remote.mapper.toDomainModelList
import com.projectu.shared.domain.model.pixivision.PixivisionArticleInfo
import com.projectu.shared.domain.model.pixivision.PixivisionDetail
import com.projectu.shared.domain.repository.PixivisionRepository

/**
 * Pixivision Repository 实现
 */
class PixivisionRepositoryImpl(
    private val pixivApi: PixivApi
) : PixivisionRepository {
    
    override suspend fun getArticleList(
        category: PixivisionCategory,
        lang: String,
        page: Int
    ): Result<List<PixivisionArticleInfo>> = runCatching {
        val response = pixivApi.pixivisionApi.getArticleList(
            category = category,
            lang = lang,
            page = page
        )
        response.articles.toDomainModelList()
    }
    
    override suspend fun getArticleDetail(
        articleId: String,
        lang: String
    ): Result<PixivisionDetail> = runCatching {
        val detail = pixivApi.pixivisionApi.getArticleDetail(
            articleId = articleId,
            lang = lang
        )
        detail.toDomainModel(language = lang)
    }
}
