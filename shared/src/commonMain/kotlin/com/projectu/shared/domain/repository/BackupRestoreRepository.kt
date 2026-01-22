package com.projectu.shared.domain.repository

import com.projectu.shared.domain.model.BackupConfig
import com.projectu.shared.domain.model.BackupInfo
import com.projectu.shared.domain.model.BackupMetadata
import com.projectu.shared.domain.model.BackupModule
import com.projectu.shared.domain.model.BackupResult
import com.projectu.shared.domain.model.RestoreResult
import kotlinx.coroutines.flow.Flow

/**
 * 备份恢复仓储接口
 */
interface BackupRestoreRepository {
    
    /**
     * 创建备份
     * @param config 备份配置
     * @return 备份结果Flow
     */
    fun createBackup(config: BackupConfig): Flow<BackupResult>
    
    /**
     * 恢复备份
     * @param filePath 备份文件路径
     * @param modules 要恢复的模块（null表示恢复所有）
     * @return 恢复结果Flow
     */
    fun restoreBackup(
        filePath: String,
        modules: Set<BackupModule>? = null
    ): Flow<RestoreResult>
    
    /**
     * 验证备份文件
     * @param filePath 备份文件路径
     * @return 备份元数据，如果文件无效则返回null
     */
    suspend fun validateBackupFile(filePath: String): Result<BackupMetadata>
    
    /**
     * 列出备份历史
     * @return 备份信息列表
     */
    suspend fun listBackups(): Result<List<BackupInfo>>
    
    /**
     * 删除备份
     * @param id 备份ID
     */
    suspend fun deleteBackup(id: String): Result<Unit>
    
    /**
     * 设置备份目录URI（Android SAF）
     * @param treeUri 目录tree URI
     * @return 是否成功
     */
    suspend fun setBackupDirectoryUri(treeUri: String): Boolean
    
    /**
     * 获取当前备份目录URI
     * @return 目录URI，如果未设置则返回null
     */
    suspend fun getBackupDirectoryUri(): String?
    
    /**
     * 检查是否有备份目录访问权限
     * @return 是否有权限
     */
    suspend fun hasBackupDirectoryAccess(): Boolean
}
