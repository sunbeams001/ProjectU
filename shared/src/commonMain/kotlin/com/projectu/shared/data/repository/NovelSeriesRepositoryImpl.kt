package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.dto.novel_series.NovelSeriesContent
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelSeries
import com.projectu.shared.domain.model.AgeLimit
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.model.NovelGenre
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
                // 从 thumbnails.novel 获取详细信息（仅包含可查看的作品）
                val thumbnails = response.body!!.thumbnails?.novel ?: emptyList()
                val thumbnailMap = thumbnails.associateBy { it.id }
                
                // 获取所有 seriesContents（包括好P友限定的作品）
                val seriesContents = response.body!!.page.seriesContents
                
                // 合并数据：使用 seriesContents 作为基础，用 thumbnails 填充详细信息
                val novels = seriesContents.map { content ->
                    val thumbnail = thumbnailMap[content.id]
                    val order = content.series?.contentOrder ?: 0
                    val viewableType = content.series?.viewableType ?: 0
                    
                    if (thumbnail != null) {
                        // 可查看的作品：使用 thumbnail 的详细信息
                        thumbnail.toNovel(ageLimitDeterminer, order).copy(
                            viewableType = viewableType
                        )
                    } else {
                        // 不可查看的作品（如好P友限定）：使用 seriesContent 的基本信息
                        content.toRestrictedNovel(seriesId.toString(), order, viewableType)
                    }
                }.sortedBy { it.seriesOrder }
                
                Result.success(novels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 将受限的 NovelSeriesContent 转换为 Novel
     * 用于好P友限定等无法获取完整信息的作品
     */
    private fun NovelSeriesContent.toRestrictedNovel(
        seriesId: String,
        order: Int,
        viewableType: Int
    ): Novel {
        return Novel(
            id = id,
            title = title ?: "", // 好P友限定作品可能没有标题
            description = commentHtml ?: "",
            content = null,
            imageUrl = url ?: "",
            userId = userId,
            userName = "", // 受限作品可能没有用户名
            userProfileImageUrl = "",
            tags = tags.map { com.projectu.shared.domain.model.Tag(it, null) },
            viewCount = 0,
            likeCount = 0,
            bookmarkCount = bookmarkCount,
            commentCount = 0,
            markerCount = 0,
            createdTime = "", // 受限作品可能没有创建时间
            updatedTime = null,
            bookmarkStatus = BookmarkStatus.NOT_BOOKMARKED,
            bookmarkId = null,
            isMasked = false,
            isAiGenerated = aiType == 2,
            isOriginal = isOriginal,
            isBungei = false,
            textCount = textLength,
            wordCount = wordCount,
            readingTime = readingTime,
            useWordCount = useWordCount,
            genre = NovelGenre.OTHER,
            language = "ja",
            ageLimit = ageLimitDeterminer.determine(xRestrict = xRestrict, tags = tags),
            seriesId = seriesId,
            seriesTitle = null,
            seriesOrder = order,
            viewableType = viewableType, // 设置可见性类型
            isUnlisted = false,
            pageCount = 1,
            marker = null,
            embeddedImages = emptyMap()
        )
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
