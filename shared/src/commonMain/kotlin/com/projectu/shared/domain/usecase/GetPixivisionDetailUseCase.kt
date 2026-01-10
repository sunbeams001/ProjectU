package com.projectu.shared.domain.usecase

import com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory
import com.projectu.shared.domain.model.pixivision.PixivisionDetail
import com.projectu.shared.domain.repository.PixivisionRepository

/**
 * 获取 Pixivision 文章详情 UseCase
 */
class GetPixivisionDetailUseCase(
    private val pixivisionRepository: PixivisionRepository
) {
    suspend operator fun invoke(
        articleId: String,
        lang: String
    ): Result<PixivisionDetail> {
        return pixivisionRepository.getArticleDetail(articleId, lang)
    }
}
