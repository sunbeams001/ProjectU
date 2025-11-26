package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.repository.NovelRepository

/**
 * 收藏小说用例
 * 
 * 执行收藏操作并更新全局状态缓存
 */
class BookmarkNovelUseCase(
    private val novelRepository: NovelRepository,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 执行收藏小说
     * 
     * @param novelId 小说ID
     * @param isPrivate 是否私人收藏
     * @param tags 收藏标签列表
     * @return Result<String> 成功返回bookmarkId，失败返回异常
     */
    suspend operator fun invoke(
        novelId: Long,
        isPrivate: Boolean = false,
        tags: List<String> = emptyList()
    ): Result<String> {
        // 1. 调用API收藏小说
        val result = novelRepository.addBookmark(novelId, isPrivate, tags)
        
        return result.mapCatching { 
            // 2. 假设API返回bookmarkId
            val bookmarkId = novelId.toString()
            
            // 3. 更新全局状态缓存
            stateCacheManager.bookmarkNovel(
                novelId = novelId.toString(),
                isPrivate = isPrivate,
                bookmarkId = bookmarkId
            )
            
            bookmarkId
        }
    }
}

