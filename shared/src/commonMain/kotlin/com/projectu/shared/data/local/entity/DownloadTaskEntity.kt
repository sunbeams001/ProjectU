package com.projectu.shared.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.projectu.shared.domain.model.DownloadStatus
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.ResourceType
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 下载任务数据库实体
 */
@Entity(tableName = "download_tasks")
data class DownloadTaskEntity(
    @PrimaryKey
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
    val tags: String, // JSON数组
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

/**
 * 将 DownloadTask 转换为 DownloadTaskEntity
 */
fun DownloadTask.toEntity(): DownloadTaskEntity {
    return DownloadTaskEntity(
        id = this.id,
        resourceType = this.resourceType.name,
        resourceId = this.resourceId,
        title = this.title,
        authorId = this.authorId,
        authorName = this.authorName,
        pageIndex = this.pageIndex,
        totalPages = this.totalPages,
        isR18 = this.isR18,
        isAi = this.isAi,
        tags = Json.encodeToString(this.tags),
        thumbnailUrl = this.thumbnailUrl,
        publishTime = this.publishTime,
        downloadTime = this.downloadTime,
        status = this.status.name,
        progress = this.progress,
        targetPath = this.targetPath,
        fileName = this.fileName,
        fileSize = this.fileSize,
        downloadedSize = this.downloadedSize,
        error = this.error,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}

/**
 * 将 DownloadTaskEntity 转换为 DownloadTask
 */
fun DownloadTaskEntity.toDownloadTask(): DownloadTask {
    return DownloadTask(
        id = this.id,
        resourceType = ResourceType.valueOf(this.resourceType),
        resourceId = this.resourceId,
        title = this.title,
        authorId = this.authorId,
        authorName = this.authorName,
        pageIndex = this.pageIndex,
        totalPages = this.totalPages,
        isR18 = this.isR18,
        isAi = this.isAi,
        tags = try {
            Json.decodeFromString<List<String>>(this.tags)
        } catch (e: Exception) {
            emptyList()
        },
        thumbnailUrl = this.thumbnailUrl,
        publishTime = this.publishTime,
        downloadTime = this.downloadTime,
        status = DownloadStatus.valueOf(this.status),
        progress = this.progress,
        targetPath = this.targetPath,
        fileName = this.fileName,
        fileSize = this.fileSize,
        downloadedSize = this.downloadedSize,
        error = this.error,
        createdAt = this.createdAt,
        updatedAt = this.updatedAt
    )
}
