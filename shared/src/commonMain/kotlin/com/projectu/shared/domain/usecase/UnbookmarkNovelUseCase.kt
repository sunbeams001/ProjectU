package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.StateCacheType
import com.projectu.shared.domain.repository.NovelRepository
import kotlinx.coroutines.flow.first

/**
 * 取消收藏小说用例
 * 
 * 执行取消收藏操作并更新全局状态缓存
 */
class UnbookmarkNovelUseCase(
    private val novelRepository: NovelRepository,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 执行取消收藏小说
     * 
     * @param novelId 小说ID
     * @return Result<Unit> 成功返回Unit，失败返回异常
     */
    suspend operator fun invoke(novelId: Long): Result<Unit> {
        // 1. 转换ID为字符串
        val novelIdStr = novelId.toString()
        
        // 2. 从缓存中获取bookmarkId
        val state = stateCacheManager.getNovelState(novelIdStr).first()
        val bookmarkId = state?.bookmarkId
        
        // 3. 如果没有bookmarkId，说明未收藏，直接返回成功
        if (bookmarkId == null) {
            return Result.success(Unit)
        }
        
        // 4. 调用API取消收藏
        val result = novelRepository.removeBookmark(bookmarkId)
        
        return result.onSuccess {
            // 5. 更新全局状态缓存
            stateCacheManager.unbookmarkNovel(novelIdStr)
        }
    }
}

