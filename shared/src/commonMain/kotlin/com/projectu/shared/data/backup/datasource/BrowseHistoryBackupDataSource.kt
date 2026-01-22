package com.projectu.shared.data.backup.datasource

import com.projectu.shared.data.local.dao.BrowseHistoryDao
import com.projectu.shared.data.local.entity.BrowseHistoryEntity
import kotlinx.serialization.Serializable

/**
 * 浏览历史备份数据源
 * 负责导出和导入浏览历史数据
 */
class BrowseHistoryBackupDataSource(
    private val browseHistoryDao: BrowseHistoryDao
) {
    
    /**
     * 导出浏览历史数据
     * @param daysRange 时间范围（天数，null表示全部）
     * @param maxRecords 最大记录数（null表示全部）
     */
    suspend fun exportData(
        daysRange: Int? = null,
        maxRecords: Int? = null
    ): BrowseHistoryBackupData {
        // 获取所有历史记录
        // 注意：Flow需要收集，这里我们需要获取当前所有数据
        // 由于DAO返回Flow，我们需要使用firstOrNull或类似方法
        // 但为了简化，直接使用suspend查询方法
        val allHistory = mutableListOf<BrowseHistoryEntity>()
        
        // 使用一个临时方法来收集所有数据
        // 实际上，BrowseHistoryDao应该有一个suspend方法返回List
        // 这里我们假设可以通过查询获取所有数据
        // 由于getAllHistory()返回Flow，我们需要另一种方式
        
        // 暂时使用getAllHistoryList方法（需要在DAO中添加）
        val allHistoryFromDb = getAllHistoryFromDao()
        
        // 应用时间范围过滤
        val filteredByTime = if (daysRange != null) {
            val cutoffTime = System.currentTimeMillis() - (daysRange * 24 * 60 * 60 * 1000L)
            allHistoryFromDb.filter { it.viewedAt >= cutoffTime }
        } else {
            allHistoryFromDb
        }
        
        // 应用数量限制（取最新的记录）
        val filteredHistory = if (maxRecords != null && maxRecords < filteredByTime.size) {
            filteredByTime
                .sortedByDescending { it.viewedAt }
                .take(maxRecords)
        } else {
            filteredByTime
        }
        
        val items = filteredHistory.map { entity ->
            BrowseHistoryBackupItem(
                id = entity.id,
                contentType = entity.contentType,
                contentId = entity.contentId,
                title = entity.title,
                thumbnailUrl = entity.thumbnailUrl,
                authorId = entity.authorId,
                authorName = entity.authorName,
                isR18 = entity.isR18,
                isAi = entity.isAi,
                viewedAt = entity.viewedAt,
                createdAt = entity.createdAt
            )
        }
        
        return BrowseHistoryBackupData(
            version = 1,
            history = items
        )
    }
    
    /**
     * 从DAO获取所有历史记录
     * 由于getAllHistory返回Flow，这里收集一次
     */
    private suspend fun getAllHistoryFromDao(): List<BrowseHistoryEntity> {
        // 使用kotlinx.coroutines.flow的first()方法获取第一个值
        return browseHistoryDao.getAllHistoryList()
    }
    
    /**
     * 导入浏览历史数据（合并策略：保留最新的viewedAt）
     */
    suspend fun importData(data: BrowseHistoryBackupData) {
        val entities = data.history.map { item ->
            BrowseHistoryEntity(
                id = item.id,
                contentType = item.contentType,
                contentId = item.contentId,
                title = item.title,
                thumbnailUrl = item.thumbnailUrl,
                authorId = item.authorId,
                authorName = item.authorName,
                isR18 = item.isR18,
                isAi = item.isAi,
                viewedAt = item.viewedAt,
                createdAt = item.createdAt
            )
        }
        
        // 使用upsertHistories，会自动处理冲突
        // 对于相同ID的记录，会更新为最新的数据
        browseHistoryDao.upsertHistories(entities)
    }
}



/**
 * 浏览历史备份数据容器
 */
@Serializable
data class BrowseHistoryBackupData(
    val version: Int = 1,
    val history: List<BrowseHistoryBackupItem>
)

/**
 * 单个浏览历史备份项
 */
@Serializable
data class BrowseHistoryBackupItem(
    val id: String,
    val contentType: String,
    val contentId: String,
    val title: String,
    val thumbnailUrl: String?,
    val authorId: String?,
    val authorName: String?,
    val isR18: Boolean,
    val isAi: Boolean,
    val viewedAt: Long,
    val createdAt: Long
)
