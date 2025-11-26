package com.projectu.shared.domain.usecase

import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.Novel

/**
 * 同步小说状态用例
 * 
 * 将全局状态缓存中的收藏状态应用到小说列表
 */
class SyncNovelStatesUseCase(
    private val stateCacheManager: StateCacheManager
) {
    /**
     * 同步小说状态
     * 
     * @param novels 小说列表
     * @return 更新了状态的小说列表
     */
    suspend operator fun invoke(novels: List<Novel>): List<Novel> {
        return stateCacheManager.applyStatesToNovels(novels)
    }
}

