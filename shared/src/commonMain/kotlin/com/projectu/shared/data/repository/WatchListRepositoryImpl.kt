package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.FollowApi
import com.projectu.shared.data.remote.dto.follow.WatchedIllustSeries
import com.projectu.shared.data.remote.dto.follow.WatchedNovelSeries
import com.projectu.shared.domain.repository.WatchListRepository

/**
 * 追更列表仓储实现
 * 使用 FollowApi 获取漫画和小说追更列表
 */
class WatchListRepositoryImpl(
    private val followApi: FollowApi
) : WatchListRepository {
    
    override suspend fun getWatchListManga(page: Int): Result<Pair<List<WatchedIllustSeries>, Boolean>> {
        return try {
            val response = followApi.getWatchListManga(page)
            
            if (response.error) {
                Result.failure(Exception(response.message))
            } else {
                val body = response.body
                val series = body?.illustSeries ?: emptyList()
                val pageInfo = body?.page
                val maxPage = pageInfo?.maxPage ?: 1
                val isLastPage = page >= maxPage || series.isEmpty()
                
                Result.success(Pair(series, isLastPage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getWatchListNovels(page: Int): Result<Pair<List<WatchedNovelSeries>, Boolean>> {
        return try {
            val response = followApi.getWatchListNovel(page)
            
            if (response.error) {
                Result.failure(Exception(response.message))
            } else {
                val body = response.body
                val series = body?.thumbnails?.novelSeries ?: emptyList()
                val pageInfo = body?.page
                val maxPage = pageInfo?.maxPage ?: 1
                val isLastPage = page >= maxPage || series.isEmpty()
                
                Result.success(Pair(series, isLastPage))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
