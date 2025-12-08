package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.DownloadStatus
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.model.ResourceType
import kotlinx.coroutines.flow.Flow

/**
 * 下载仓储接口
 * 定义下载相关的数据访问操作
 */
interface DownloadRepository {
    
    /**
     * 观察所有下载任务
     */
    fun observeAllTasks(): Flow<List<DownloadTask>>
    
    /**
     * 观察指定状态的下载任务
     */
    fun observeTasksByStatus(status: DownloadStatus): Flow<List<DownloadTask>>
    
    /**
     * 获取所有下载任务流
     */
    fun getAllDownloadTasks(): Flow<List<DownloadTask>>
    
    /**
     * 添加插画/漫画下载任务（从作品对象）
     * @param artwork 作品对象
     * @param pageIndex 页码索引（null表示下载所有页）
     * @return 任务ID
     */
    suspend fun addIllustrationDownload(artwork: Artwork, pageIndex: Int? = null): Result<String>
    
    /**
     * 添加插画/漫画下载任务（从作品ID，会发起网络请求）
     * @param illustId 作品ID
     * @param pageIndex 页码索引（null表示下载所有页）
     * @return 任务ID
     */
    suspend fun addIllustrationDownload(illustId: Long, pageIndex: Int? = null): Result<String>
    
    /**
     * 开始下载任务
     */
    suspend fun startDownload(taskId: String)
    
    /**
     * 暂停下载任务
     */
    fun pauseDownload(taskId: String)
    
    /**
     * 删除下载任务
     * @param taskId 任务ID
     * @param deleteFile 是否同时删除已下载的文件
     */
    suspend fun deleteTask(taskId: String, deleteFile: Boolean = false)
    
    /**
     * 删除下载任务
     * @param taskId 任务ID
     * @param deleteFile 是否同时删除已下载的文件
     */
    suspend fun deleteDownloadTask(taskId: String, deleteFile: Boolean = false)
    
    /**
     * 检查资源是否已下载
     */
    suspend fun isResourceDownloaded(
        resourceType: ResourceType,
        resourceId: String,
        pageIndex: Int? = null
    ): Boolean
}
