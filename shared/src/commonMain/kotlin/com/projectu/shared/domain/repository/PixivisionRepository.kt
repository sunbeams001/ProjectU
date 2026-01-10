package com.projectu.shared.domain.repository

import com.projectu.shared.data.remote.dto.pixivision.PixivisionCategory
import com.projectu.shared.domain.model.pixivision.PixivisionArticleInfo
import com.projectu.shared.domain.model.pixivision.PixivisionDetail

/**
 * Pixivision Repository 接口
 */
interface PixivisionRepository {
    
    /**
     * 获取 Pixivision 文章列表
     * 
     * @param category 类别 (illustration/manga)
     * @param lang 语言代码
     * @param page 页码（从1开始）
     * @return Result<List<PixivisionArticleInfo>>
     */
    suspend fun getArticleList(
        category: PixivisionCategory,
        lang: String,
        page: Int
    ): Result<List<PixivisionArticleInfo>>
    
    /**
     * 获取 Pixivision 文章详情
     * 
     * @param articleId 文章ID
     * @param lang 语言代码
     * @return Result<PixivisionDetail>
     */
    suspend fun getArticleDetail(
        articleId: String,
        lang: String
    ): Result<PixivisionDetail>
}
