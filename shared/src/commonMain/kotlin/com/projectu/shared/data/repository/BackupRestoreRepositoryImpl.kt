package com.projectu.shared.data.repository

import com.projectu.shared.data.backup.manager.BackupManager
import com.projectu.shared.data.backup.manager.RestoreManager
import com.projectu.shared.data.backup.storage.BackupStorage
import com.projectu.shared.domain.model.*
import com.projectu.shared.domain.repository.BackupRestoreRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 备份恢复仓储实现
 */
class BackupRestoreRepositoryImpl(
    private val backupManager: BackupManager,
    private val restoreManager: RestoreManager,
    private val backupStorage: BackupStorage
) : BackupRestoreRepository {
    
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    override fun createBackup(config: BackupConfig): Flow<BackupResult> {
        return backupManager.createBackup(config)
    }
    
    override fun restoreBackup(
        filePath: String,
        modules: Set<BackupModule>?
    ): Flow<RestoreResult> {
        return restoreManager.restoreBackup(filePath, modules)
    }
    
    override suspend fun validateBackupFile(filePath: String): Result<BackupMetadata> {
        return restoreManager.validateBackupFile(filePath)
    }
    
    override suspend fun listBackups(): Result<List<BackupInfo>> {
        return try {
            println("BackupRestoreRepositoryImpl: Starting to list backups")
            val backupFiles = backupStorage.listBackupFiles()
            println("BackupRestoreRepositoryImpl: Found ${backupFiles.size} backup files from storage")
            
            val backupInfoList = backupFiles.mapNotNull { file ->
                try {
                    println("BackupRestoreRepositoryImpl: Processing file: ${file.name} (path=${file.absolutePath}, exists=${file.exists()})")
                    // 尝试读取元数据
                    val metadataResult = validateBackupFile(file.absolutePath)
                    if (metadataResult.isSuccess) {
                        val metadata = metadataResult.getOrNull()!!
                        println("BackupRestoreRepositoryImpl: Successfully parsed metadata for ${file.name}")
                        BackupInfo(
                            id = file.nameWithoutExtension.removePrefix("backup_"),
                            fileName = file.name,
                            filePath = file.absolutePath,
                            metadata = metadata,
                            fileSize = file.length()
                        )
                    } else {
                        println("BackupRestoreRepositoryImpl: Failed to validate ${file.name}: ${metadataResult.exceptionOrNull()?.message}")
                        null
                    }
                } catch (e: Exception) {
                    println("BackupRestoreRepositoryImpl: Error processing ${file.name}: ${e.message}")
                    e.printStackTrace()
                    null
                }
            }.sortedByDescending { it.metadata.timestamp }
            
            println("BackupRestoreRepositoryImpl: Successfully parsed ${backupInfoList.size} backup info objects")
            Result.success(backupInfoList)
        } catch (e: Exception) {
            println("BackupRestoreRepositoryImpl: Error listing backups: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
    
    override suspend fun deleteBackup(id: String): Result<Unit> {
        return try {
            val fileName = "backup_$id.pbu.zip"
            val success = backupStorage.deleteBackupFile(fileName)
            if (success) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("删除失败"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun setBackupDirectoryUri(treeUri: String): Boolean {
        return backupStorage.setBackupDirectoryUri(treeUri)
    }
    
    override suspend fun getBackupDirectoryUri(): String? {
        return backupStorage.getBackupDirectoryUri()
    }
    
    override suspend fun hasBackupDirectoryAccess(): Boolean {
        return backupStorage.hasBackupDirectoryAccess()
    }
}
