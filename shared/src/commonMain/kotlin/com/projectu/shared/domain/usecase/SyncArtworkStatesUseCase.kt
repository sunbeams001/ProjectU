package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.Artwork

/**
 * 同步作品状态用例
 * 
 * 将全局状态缓存中的收藏状态应用到作品列表
 * 用于页面加载时批量更新作品状态
 */
class SyncArtworkStatesUseCase(
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 同步作品状态
     * 
     * @param artworks 作品列表
     * @return 更新了状态的作品列表
     */
    suspend operator fun invoke(artworks: List<Artwork>): List<Artwork> {
        return stateCacheManager.applyStatesToArtworks(artworks)
    }
}

