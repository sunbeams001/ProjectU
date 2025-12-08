package com.projectu.shared.data.local.dao

import androidx.room.*
import com.projectu.shared.data.local.entity.DownloadTaskEntity
import kotlinx.coroutines.flow.Flow

/**
 * 下载任务数据访问对象
 * 定义下载任务的数据库操作
 */
@Dao
interface DownloadDao {
    
    /**
     * 获取所有下载任务（按创建时间倒序）
     */
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllTasks(): Flow<List<DownloadTaskEntity>>
    
    /**
     * 获取指定状态的下载任务
     */
    @Query("SELECT * FROM download_tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: String): Flow<List<DownloadTaskEntity>>
    
    /**
     * 根据ID获取下载任务
     */
    @Query("SELECT * FROM download_tasks WHERE id = :taskId")
    suspend fun getTask(taskId: String): DownloadTaskEntity?
    
    /**
     * 检查资源是否已下载完成
     * @param type 资源类型
     * @param id 资源ID
     * @param page 页码索引（可为null）
     * @return 是否已下载
     */
    @Query("""
        SELECT EXISTS(
            SELECT 1 FROM download_tasks 
            WHERE resourceType = :type 
            AND resourceId = :id 
            AND (:page IS NULL OR pageIndex = :page OR pageIndex IS NULL)
            AND status = 'COMPLETED'
        )
    """)
    suspend fun isResourceDownloaded(type: String, id: String, page: Int?): Boolean
    
    /**
     * 插入或更新下载任务
     */
    @Upsert
    suspend fun upsertTask(task: DownloadTaskEntity)
    
    /**
     * 批量插入或更新下载任务
     */
    @Upsert
    suspend fun upsertTasks(tasks: List<DownloadTaskEntity>)
    
    /**
     * 删除指定下载任务
     */
    @Query("DELETE FROM download_tasks WHERE id = :taskId")
    suspend fun deleteTask(taskId: String)
    
    /**
     * 删除所有已完成的任务
     */
    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun deleteCompletedTasks()
    
    /**
     * 删除所有失败的任务
     */
    @Query("DELETE FROM download_tasks WHERE status = 'FAILED'")
    suspend fun deleteFailedTasks()
    
    /**
     * 更新任务状态
     */
    @Query("""
        UPDATE download_tasks 
        SET status = :status, 
            progress = :progress, 
            downloadedSize = :downloadedSize, 
            error = :error, 
            updatedAt = :timestamp 
        WHERE id = :taskId
    """)
    suspend fun updateTaskStatus(
        taskId: String, 
        status: String, 
        progress: Float = 0f, 
        downloadedSize: Long = 0L, 
        error: String? = null, 
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 更新任务进度
     */
    @Query("""
        UPDATE download_tasks 
        SET progress = :progress, 
            downloadedSize = :downloadedSize, 
            updatedAt = :timestamp 
        WHERE id = :taskId
    """)
    suspend fun updateTaskProgress(
        taskId: String,
        progress: Float,
        downloadedSize: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    /**
     * 获取正在下载的任务数量
     */
    @Query("SELECT COUNT(*) FROM download_tasks WHERE status = 'DOWNLOADING'")
    suspend fun getDownloadingTaskCount(): Int
    
    /**
     * 获取待下载的任务列表
     */
    @Query("SELECT * FROM download_tasks WHERE status = 'PENDING' ORDER BY createdAt ASC")
    suspend fun getPendingTasks(): List<DownloadTaskEntity>
}
