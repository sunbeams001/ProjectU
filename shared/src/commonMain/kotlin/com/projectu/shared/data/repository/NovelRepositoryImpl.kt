package com.projectu.shared.data.repository

import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toNovel
import com.projectu.shared.data.remote.mapper.toNovelList
import com.projectu.shared.data.remote.model.DiscoveryMode
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
}

