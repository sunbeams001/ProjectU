package com.projectu.shared.data.repository

import com.projectu.shared.data.local.dao.BrowseHistoryDao
import com.projectu.shared.data.local.entity.BrowseHistoryEntity
import com.projectu.shared.domain.model.BrowseHistoryItem
import com.projectu.shared.domain.model.HistoryContentType
import com.projectu.shared.domain.repository.BrowseHistoryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * 浏览历史仓储实现
 */
class BrowseHistoryRepositoryImpl(
    private val browseHistoryDao: BrowseHistoryDao
) : BrowseHistoryRepository {
    
    override fun getAllHistory(): Flow<List<BrowseHistoryItem>> {
        return browseHistoryDao.getAllHistory().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override fun getHistoryByType(contentType: HistoryContentType): Flow<List<BrowseHistoryItem>> {
        return browseHistoryDao.getHistoryByType(contentType.value).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }
    
    override suspend fun addOrUpdateHistory(
        contentType: HistoryContentType,
        contentId: String,
        title: String,
        thumbnailUrl: String?,
        authorId: String?,
        authorName: String?,
        isR18: Boolean,
        isAi: Boolean
    ): Result<Unit> {
        return try {
            val id = "${contentType.value}_$contentId"
            val currentTime = System.currentTimeMillis()
            
            // 检查是否已存在
            val existing = browseHistoryDao.getHistoryById(id)
            
            val entity = BrowseHistoryEntity(
                id = id,
                contentType = contentType.value,
                contentId = contentId,
                title = title,
                thumbnailUrl = thumbnailUrl,
                authorId = authorId,
                authorName = authorName,
                isR18 = isR18,
                isAi = isAi,
                viewedAt = currentTime,
                createdAt = existing?.createdAt ?: currentTime
            )
            
            browseHistoryDao.upsertHistory(entity)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteHistoryById(id: String): Result<Unit> {
        return try {
            browseHistoryDao.deleteHistoryById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun deleteHistoryByType(contentType: HistoryContentType): Result<Unit> {
        return try {
            browseHistoryDao.deleteHistoryByType(contentType.value)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun clearAllHistory(): Result<Unit> {
        return try {
            browseHistoryDao.clearAllHistory()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getHistoryCount(): Int {
        return browseHistoryDao.getHistoryCount()
    }
    
    override suspend fun getHistoryCountByType(contentType: HistoryContentType): Int {
        return browseHistoryDao.getHistoryCountByType(contentType.value)
    }
    
    /**
     * Entity转Domain Model
     */
    private fun BrowseHistoryEntity.toDomainModel(): BrowseHistoryItem {
        return BrowseHistoryItem(
            id = id,
            contentType = HistoryContentType.fromValue(contentType),
            contentId = contentId,
            title = title,
            thumbnailUrl = thumbnailUrl,
            authorId = authorId,
            authorName = authorName,
            isR18 = isR18,
            isAi = isAi,
            viewedAt = viewedAt,
            createdAt = createdAt
        )
    }
}
