package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.repository.ArtworkRepository

/**
 * 收藏作品用例
 * 
 * 执行收藏操作并更新全局状态缓存
 */
class BookmarkArtworkUseCase(
    private val artworkRepository: ArtworkRepository,
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 执行收藏作品
     * 
     * @param artworkId 作品ID
     * @param isPrivate 是否私人收藏
     * @param tags 收藏标签列表
     * @return Result<String> 成功返回bookmarkId，失败返回异常
     */
    suspend operator fun invoke(
        artworkId: Long,
        isPrivate: Boolean = false,
        tags: List<String> = emptyList()
    ): Result<String> {
        // 1. 调用API收藏作品
        val result = artworkRepository.addBookmark(artworkId, isPrivate, tags)
        
        return result.mapCatching { 
            // 2. 假设API返回bookmarkId，这里需要从实际的API响应中获取
            // 由于当前API返回Unit，我们使用artworkId作为bookmarkId的临时方案
            val bookmarkId = artworkId.toString()
            
            // 3. 更新全局状态缓存
            stateCacheManager.bookmarkArtwork(
                artworkId = artworkId.toString(),
                isPrivate = isPrivate,
                bookmarkId = bookmarkId
            )
            
            bookmarkId
        }
    }
}

