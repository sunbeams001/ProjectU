package com.projectu.shared.data.repository

import com.projectu.shared.data.manager.DownloadManager
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.DownloadStatus
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.ResourceType
import com.projectu.shared.domain.repository.DownloadRepository
import kotlinx.coroutines.flow.Flow

/**
 * 下载仓储实现
 * 委托给DownloadManager处理具体业务逻辑
 */
class DownloadRepositoryImpl(
    private val downloadManager: DownloadManager
) : DownloadRepository {
    
    override fun observeAllTasks(): Flow<List<DownloadTask>> {
        return downloadManager.getAllTasks()
    }
    
    override fun observeTasksByStatus(status: DownloadStatus): Flow<List<DownloadTask>> {
        return downloadManager.getTasksByStatus(status)
    }
    
    override fun getAllDownloadTasks(): Flow<List<DownloadTask>> {
        return downloadManager.getAllTasks()
    }
    
    override suspend fun addIllustrationDownload(artwork: Artwork, pageIndex: Int?): Result<String> {
        return downloadManager.addIllustrationDownloadTask(artwork, pageIndex)
    }
    
    override suspend fun addIllustrationDownload(illustId: Long, pageIndex: Int?): Result<String> {
        return downloadManager.addIllustrationDownloadTask(illustId, pageIndex)
    }
    
    override suspend fun addUgoiraDownload(artwork: Artwork): Result<String> {
        return downloadManager.addUgoiraDownloadTask(artwork)
    }
    
    override suspend fun addNovelDownload(novelId: String): Result<String> {
        return downloadManager.addNovelDownloadTask(novelId)
    }
    
    override suspend fun addNovelSeriesDownload(seriesId: String): Result<String> {
        return downloadManager.addNovelSeriesDownloadTask(seriesId)
    }
    
    override suspend fun startDownload(taskId: String) {
        downloadManager.startDownload(taskId)
    }
    
    override fun pauseDownload(taskId: String) {
        downloadManager.pauseDownload(taskId)
    }
    
    override suspend fun deleteTask(taskId: String, deleteFile: Boolean) {
        downloadManager.deleteTask(taskId, deleteFile)
    }
    
    override suspend fun deleteDownloadTask(taskId: String, deleteFile: Boolean) {
        downloadManager.deleteTask(taskId, deleteFile)
    }
    
    override suspend fun isResourceDownloaded(
        resourceType: ResourceType,
        resourceId: String,
        pageIndex: Int?
    ): Boolean {
        return downloadManager.isDownloaded(resourceType, resourceId, pageIndex)
    }
}
