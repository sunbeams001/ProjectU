package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelSeries
import com.projectu.shared.domain.repository.NovelSeriesRepository
import com.projectu.shared.util.AgeLimitDeterminer

/**
 * 小说系列仓库实现
 */
class NovelSeriesRepositoryImpl(
    private val pixivApi: PixivApi,
    private val ageLimitDeterminer: AgeLimitDeterminer
) : NovelSeriesRepository {
    
    override suspend fun getSeriesDetail(seriesId: Long): Result<NovelSeries> {
        return try {
            val response = pixivApi.novelSeriesApi.getDetail(seriesId)
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "Failed to get series detail"))
            } else {
                val series = response.body!!.toNovelSeries()
                Result.success(series)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSeriesContents(
        seriesId: Long,
        limit: Int,
        lastOrder: Int?,
        orderBy: String
    ): Result<List<Novel>> {
        return try {
            val response = pixivApi.novelSeriesApi.getContents(
                seriesId = seriesId,
                limit = limit,
                lastOrder = lastOrder,
                orderBy = orderBy
            )
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "Failed to get series contents"))
            } else {
                // 从 thumbnails.novel 获取详细信息
                val thumbnails = response.body!!.thumbnails?.novel ?: emptyList()
                
                // 同时获取 seriesContents 的顺序信息
                val contentOrderMap = response.body!!.page.seriesContents.associate { 
                    it.id to (it.series?.contentOrder ?: 0) 
                }
                
                // 将 thumbnails 转换为 Novel，并设置正确的顺序
                val novels = thumbnails.mapIndexed { index, thumbnail ->
                    val order = contentOrderMap[thumbnail.id] ?: (index + 1)
                    thumbnail.toNovel(ageLimitDeterminer, order)
                }.sortedBy { it.seriesOrder }
                
                Result.success(novels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun watchSeries(seriesId: Long): Result<Unit> {
        return try {
            val response = pixivApi.novelSeriesApi.watch(seriesId)
            if (response.error) {
                Result.failure(Exception(response.message ?: "Failed to watch series"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun unwatchSeries(seriesId: Long): Result<Unit> {
        return try {
            val response = pixivApi.novelSeriesApi.unwatch(seriesId)
            if (response.error) {
                Result.failure(Exception(response.message ?: "Failed to unwatch series"))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
