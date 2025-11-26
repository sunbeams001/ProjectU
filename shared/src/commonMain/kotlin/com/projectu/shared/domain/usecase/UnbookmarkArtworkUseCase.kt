package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.repository.ArtworkRepository

/**
 * 取消收藏作品用例
 * 
 * 执行取消收藏操作并更新全局状态缓存
 */
class UnbookmarkArtworkUseCase(
    private val artworkRepository: ArtworkRepository,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 执行取消收藏作品
     * 
     * @param artworkId 作品ID
     * @return Result<Unit> 成功返回Unit，失败返回异常
     */
    suspend operator fun invoke(artworkId: Long): Result<Unit> {
        // 1. 调用API取消收藏
        val result = artworkRepository.removeBookmark(artworkId)
        
        return result.onSuccess {
            // 2. 更新全局状态缓存
            stateCacheManager.unbookmarkArtwork(artworkId.toString())
        }
    }
}

