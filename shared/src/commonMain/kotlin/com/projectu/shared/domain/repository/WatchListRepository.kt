package com.projectu.shared.domain.repository

import com.projectu.shared.data.remote.dto.follow.WatchedIllustSeries
import com.projectu.shared.data.remote.dto.follow.WatchedNovelSeries

/**
 * 追更列表仓储接口
 * 提供漫画和小说追更列表的获取功能
 */
interface WatchListRepository {
    
    /**
     * 获取漫画追更列表
     * @param page 页码
     * @return Pair<系列列表, 是否最后一页>
     */
    suspend fun getWatchListManga(page: Int = 1): Result<Pair<List<WatchedIllustSeries>, Boolean>>
    
    /**
     * 获取小说追更列表
     * @param page 页码
     * @return Pair<系列列表, 是否最后一页>
     */
    suspend fun getWatchListNovels(page: Int = 1): Result<Pair<List<WatchedNovelSeries>, Boolean>>
}
