package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.HistoryContentType
import com.projectu.shared.domain.repository.BrowseHistoryRepository

/**
 * 保存浏览历史 UseCase
 * 统一处理各类内容的浏览历史保存
 */
class SaveBrowseHistoryUseCase(
    private val browseHistoryRepository: BrowseHistoryRepository
) {
    
    /**
     * 保存作品浏览历史（插画/漫画/动图）
     */
    suspend fun saveArtworkHistory(
        artworkId: String,
        title: String,
        thumbnailUrl: String?,
        authorId: String?,
        authorName: String?,
        isR18: Boolean,
        isAi: Boolean,
        isUgoira: Boolean = false,
        isManga: Boolean = false
    ): Result<Unit> {
        val contentType = when {
            isUgoira -> HistoryContentType.UGOIRA
            isManga -> HistoryContentType.MANGA
            else -> HistoryContentType.ILLUST
        }
        
        return browseHistoryRepository.addOrUpdateHistory(
            contentType = contentType,
            contentId = artworkId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            authorId = authorId,
            authorName = authorName,
            isR18 = isR18,
            isAi = isAi
        )
    }
    
    /**
     * 保存小说浏览历史
     */
    suspend fun saveNovelHistory(
        novelId: String,
        title: String,
        thumbnailUrl: String?,
        authorId: String?,
        authorName: String?,
        isR18: Boolean,
        isAi: Boolean
    ): Result<Unit> {
        return browseHistoryRepository.addOrUpdateHistory(
            contentType = HistoryContentType.NOVEL,
            contentId = novelId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            authorId = authorId,
            authorName = authorName,
            isR18 = isR18,
            isAi = isAi
        )
    }
    
    /**
     * 保存小说系列浏览历史
     */
    suspend fun saveNovelSeriesHistory(
        seriesId: String,
        title: String,
        thumbnailUrl: String?,
        authorId: String?,
        authorName: String?,
        isR18: Boolean
    ): Result<Unit> {
        return browseHistoryRepository.addOrUpdateHistory(
            contentType = HistoryContentType.NOVEL_SERIES,
            contentId = seriesId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            authorId = authorId,
            authorName = authorName,
            isR18 = isR18,
            isAi = false // 系列本身不是AI作品
        )
    }
    
    /**
     * 保存漫画系列浏览历史
     */
    suspend fun saveMangaSeriesHistory(
        seriesId: String,
        title: String,
        thumbnailUrl: String?,
        authorId: String?,
        authorName: String?,
        isR18: Boolean
    ): Result<Unit> {
        return browseHistoryRepository.addOrUpdateHistory(
            contentType = HistoryContentType.MANGA_SERIES,
            contentId = seriesId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            authorId = authorId,
            authorName = authorName,
            isR18 = isR18,
            isAi = false // 系列本身不是AI作品
        )
    }
}
