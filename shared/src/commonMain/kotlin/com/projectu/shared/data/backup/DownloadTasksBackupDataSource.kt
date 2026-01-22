package com.projectu.shared.data.backup

import com.projectu.shared.data.local.dao.DownloadDao
import com.projectu.shared.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * 下载任务备份数据源
 * 负责导出和导入下载任务记录
 */
class DownloadTasksBackupDataSource(
    private val downloadDao: DownloadDao
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * 导出下载任务数据
     */
    suspend fun exportData(): String {
        val tasks = downloadDao.getAllTasks().first()
        val backupData = DownloadTasksBackupData(
            tasks = tasks.map { it.toBackupTask() }
        )
        return json.encodeToString(DownloadTasksBackupData.serializer(), backupData)
    }
    
    /**
     * 导入下载任务数据
     * 使用 upsertTasks 进行合并，已存在的任务将被更新
     */
    suspend fun importData(jsonData: String) {
        val backupData = json.decodeFromString(DownloadTasksBackupData.serializer(), jsonData)
        val entities = backupData.tasks.map { it.toEntity() }
        downloadDao.upsertTasks(entities)
    }
    
    /**
     * 下载任务实体转备份任务
     */
    private fun DownloadTaskEntity.toBackupTask(): BackupDownloadTask {
        return BackupDownloadTask(
            id = id,
            resourceType = resourceType,
            resourceId = resourceId,
            title = title,
            authorId = authorId,
            authorName = authorName,
            pageIndex = pageIndex,
            totalPages = totalPages,
            isR18 = isR18,
            isAi = isAi,
            tags = tags,
            thumbnailUrl = thumbnailUrl,
            publishTime = publishTime,
            downloadTime = downloadTime,
            status = status,
            progress = progress,
            targetPath = targetPath,
            fileName = fileName,
            fileSize = fileSize,
            downloadedSize = downloadedSize,
            error = error,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
    
    /**
     * 备份任务转下载任务实体
     */
    private fun BackupDownloadTask.toEntity(): DownloadTaskEntity {
        return DownloadTaskEntity(
            id = id,
            resourceType = resourceType,
            resourceId = resourceId,
            title = title,
            authorId = authorId,
            authorName = authorName,
            pageIndex = pageIndex,
            totalPages = totalPages,
            isR18 = isR18,
            isAi = isAi,
            tags = tags,
            thumbnailUrl = thumbnailUrl,
            publishTime = publishTime,
            downloadTime = downloadTime,
            status = status,
            progress = progress,
            targetPath = targetPath,
            fileName = fileName,
            fileSize = fileSize,
            downloadedSize = downloadedSize,
            error = error,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

/**
 * 下载任务备份数据容器
 */
@Serializable
data class DownloadTasksBackupData(
    val tasks: List<BackupDownloadTask>
)

/**
 * 备份用的下载任务数据
 */
@Serializable
data class BackupDownloadTask(
    val id: String,
    val resourceType: String,
    val resourceId: String,
    val title: String,
    val authorId: String,
    val authorName: String,
    val pageIndex: Int?,
    val totalPages: Int,
    val isR18: Boolean,
    val isAi: Boolean,
    val tags: String,
    val thumbnailUrl: String?,
    val publishTime: Long,
    val downloadTime: Long,
    val status: String,
    val progress: Float,
    val targetPath: String,
    val fileName: String,
    val fileSize: Long,
    val downloadedSize: Long,
    val error: String?,
    val createdAt: Long,
    val updatedAt: Long
)
