package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toArtwork
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.MangaSeries
import com.projectu.shared.domain.model.MangaSeriesWork
import com.projectu.shared.domain.repository.MangaSeriesRepository
import com.projectu.shared.domain.repository.SeriesWorksResult
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil

/**
 * 漫画系列仓库实现
 */
class MangaSeriesRepositoryImpl(
    private val pixivApi: PixivApi,
    private val ageLimitDeterminer: AgeLimitDeterminer,
    private val tagTranslationUtil: TagTranslationUtil
) : MangaSeriesRepository {
    
    override suspend fun getSeriesDetail(seriesId: Long, page: Int): Result<MangaSeries> {
        return try {
            val response = pixivApi.illustSeriesApi.getDetail(seriesId, page)
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message.ifEmpty { "Failed to get series detail" }))
            } else {
                val body = response.body
                
                // 从 illustSeries 获取系列基本信息
                val seriesInfo = body.illustSeries.firstOrNull()
                
                // 从 users 获取用户信息
                val userInfo = body.users.firstOrNull()
                
                // 从 page 获取分页和状态信息
                val pageInfo = body.page
                
                // 构建作品列表
                val works = pageInfo?.series?.map { work ->
                    MangaSeriesWork(
                        workId = work.workId,
                        order = work.order
                    )
                } ?: emptyList()
                
                val series = MangaSeries(
                    id = seriesInfo?.id ?: seriesId.toString(),
                    title = seriesInfo?.title ?: "",
                    description = seriesInfo?.description ?: "",
                    caption = seriesInfo?.caption ?: "",
                    userId = seriesInfo?.userId ?: userInfo?.userId ?: "",
                    userName = userInfo?.name ?: "",
                    profileImageUrl = userInfo?.image,
                    isFollowed = userInfo?.isFollowed ?: false,
                    coverUrl = seriesInfo?.url,
                    isWatched = pageInfo?.isWatched ?: seriesInfo?.isWatched ?: false,
                    isNotifying = pageInfo?.isNotifying ?: seriesInfo?.isNotifying ?: false,
                    total = pageInfo?.total ?: seriesInfo?.total ?: 0,
                    watchCount = seriesInfo?.watchCount,
                    createDate = seriesInfo?.createDate ?: "",
                    updateDate = seriesInfo?.updateDate ?: "",
                    firstIllustId = seriesInfo?.firstIllustId,
                    latestIllustId = seriesInfo?.latestIllustId,
                    works = works
                )
                
                Result.success(series)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getSeriesWorks(seriesId: Long, page: Int): Result<SeriesWorksResult> {
        return try {
            val response = pixivApi.illustSeriesApi.getDetail(seriesId, page)
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message.ifEmpty { "Failed to get series works" }))
            } else {
                val body = response.body
                
                // 从 page.series 获取当前页的作品ID列表（按顺序）
                val seriesWorks = body.page?.series ?: emptyList()
                val workIds = seriesWorks.map { it.workId }
                
                // 从 thumbnails.illust 构建作品ID到作品详情的映射
                val illustMap = (body.thumbnails?.illust ?: emptyList()).associateBy { it.id }
                
                // 获取作品总数
                val total = body.page?.total ?: 0
                
                // 获取标签翻译
                val tagTranslation = body.tagTranslation.mapValues { (_, translation) ->
                    mapOf(
                        "en" to (translation.en ?: ""),
                        "zh" to (translation.zh ?: ""),
                        "zh_tw" to (translation.zhTw ?: ""),
                        "ko" to (translation.ko ?: "")
                    ).filterValues { it.isNotEmpty() }
                }
                
                // 按 page.series 中的顺序获取作品，只保留能找到的
                val artworks = workIds.mapNotNull { workId ->
                    illustMap[workId]?.toArtwork(
                        tagTranslationUtil = tagTranslationUtil,
                        tagTranslation = tagTranslation,
                        ageLimitDeterminer = ageLimitDeterminer
                    )
                }
                
                Result.success(SeriesWorksResult(artworks = artworks, total = total))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun watchSeries(seriesId: Long): Result<Unit> {
        return try {
            val response = pixivApi.illustSeriesApi.watch(seriesId)
            if (response.error) {
                Result.failure(Exception(response.message.ifEmpty { "Failed to watch series" }))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun unwatchSeries(seriesId: Long): Result<Unit> {
        return try {
            val response = pixivApi.illustSeriesApi.unwatch(seriesId)
            if (response.error) {
                Result.failure(Exception(response.message.ifEmpty { "Failed to unwatch series" }))
            } else {
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
