package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelList
import com.projectu.shared.data.remote.mapper.toNovelRankingList
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.util.AgeLimitDeterminer

/**
 * 小说仓库实现
 */
class NovelRepositoryImpl(
    private val pixivApi: PixivApi,
    private val ageLimitDeterminer: AgeLimitDeterminer
) : NovelRepository {
    
    override suspend fun getNovelDetail(novelId: String): Result<Novel> {
        return try {
            val response = pixivApi.novelApi.getDetail(novelId.toLong())
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "获取小说详情失败"))
            } else {
                val novel = response.body!!.toNovel(ageLimitDeterminer)
                Result.success(novel)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun searchNovels(
        keyword: String,
        searchMode: String,
        order: String,
        mode: String,
        page: Int
    ): Result<List<Novel>> {
        return try {
            val response = pixivApi.novelApi.search(
                keyword = keyword,
                searchMode = searchMode,
                order = order,
                mode = mode,
                page = page
            )
            
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "搜索小说失败"))
            } else {
                val novels = response.body!!.novel.data.toNovelList(
                    tagTranslation = response.body!!.tagTranslation,
                    ageLimitDeterminer = ageLimitDeterminer
                )
                Result.success(novels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getDiscoveryNovels(
        mode: DiscoveryMode,
        limit: Int
    ): Result<List<Novel>> {
        return try {
            val response = pixivApi.novelApi.getDiscovery(
                mode = mode.value,
                limit = limit
            )
            
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "获取推荐小说失败"))
            } else {
                val novels = response.body!!.thumbnails.novel?.toNovelList(
                    tagTranslation = response.body!!.tagTranslation,
                    ageLimitDeterminer = ageLimitDeterminer
                ) ?: emptyList()
                Result.success(novels)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRankingNovels(
        mode: RankingMode,
        content: RankingContent,
        page: Int,
        date: String?
    ): Result<List<Novel>> {
        return try {
            val response = pixivApi.rankingApi.getNovelRankingJson(
                mode = mode,
                content = content,
                page = page,
                date = date
            )
            
            // 转换排行榜数据为小说列表
            val novels = response.displayA.rankA.toNovelRankingList(ageLimitDeterminer)
            Result.success(novels)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getRankingWithDateInfo(
        mode: RankingMode,
        content: RankingContent,
        page: Int,
        date: String?
    ): Result<Pair<List<Novel>, Triple<String?, String?, String?>>> {
        return try {
            val body = pixivApi.rankingApi.getNovelRankingJson(
                mode = mode,
                content = content,
                page = page,
                date = date
            )
            
            // 转换排行榜数据为小说列表
            val novels = body.displayA.rankA.toNovelRankingList(ageLimitDeterminer)
            // 提取日期信息 (注意：小说排行榜的日期字段在body中，可能是start/end，也可能是date)
            val currentDate = body.start ?: body.date
            val dateInfo = Triple(currentDate, body.displayA.prevDate, body.displayA.nextDate)
            Result.success(Pair(novels, dateInfo))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addBookmark(
        novelId: Long,
        isPrivate: Boolean,
        tags: List<String>
    ): Result<String> {
        return try {
            val response = pixivApi.bookmarkApi.addNovel(
                novelId = novelId,
                restrict = if (isPrivate) 1 else 0,
                tags = tags
            )
            if (response.error || response.body == null) {
                Result.failure(Exception(response.message ?: "添加收藏失败"))
            } else {
                Result.success(response.body!!) // 返回 bookmarkId
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun removeBookmark(bookmarkId: String): Result<Unit> {
        return try {
            pixivApi.bookmarkApi.deleteNovel(bookmarkId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addNovelMarker(
        novelId: Long,
        userId: Long,
        page: Int
    ): Result<Unit> {
        return try {
            pixivApi.markerApi.addNovelMarker(
                novelId = novelId,
                userId = userId,
                page = page
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteNovelMarker(
        novelId: Long,
        userId: Long
    ): Result<Unit> {
        return try {
            pixivApi.markerApi.deleteNovelMarker(
                novelId = novelId,
                userId = userId
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

