package com.projectu.shared.data.backup.manager

import com.projectu.shared.data.backup.datasource.CredentialsBackupData
import com.projectu.shared.data.backup.datasource.CredentialsBackupDataSource
import com.projectu.shared.data.backup.datasource.SettingsBackupData
import com.projectu.shared.data.backup.datasource.SettingsBackupDataSource
import com.projectu.shared.data.backup.serializer.ChecksumCalculator
import com.projectu.shared.data.backup.serializer.CompressionHelper
import com.projectu.shared.data.backup.storage.BackupStorage
import com.projectu.shared.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import java.io.File

/**
 * 恢复管理器
 * 协调P0模块的恢复流程
 */
class RestoreManager(
    private val settingsDataSource: SettingsBackupDataSource,
    private val credentialsDataSource: CredentialsBackupDataSource,
    private val backupStorage: BackupStorage
) {
    
    private val json = Json {
        ignoreUnknownKeys = true
    }
    
    /**
     * 恢复备份
     */
    fun restoreBackup(
        filePath: String,
        modules: Set<BackupModule>?
    ): Flow<RestoreResult> = flow {
        try {
            // 1. 验证备份文件
            emit(RestoreResult.Progress(
                currentModule = BackupModule.SETTINGS,
                progress = 0f,
                message = "验证备份文件..."
            ))
            
            // 处理content:// URI或普通文件路径
            val backupFile = if (filePath.startsWith("content://")) {
                // 从content URI复制到临时文件
                val tempFile = File(backupStorage.createTempDirectory("restore_uri_${System.currentTimeMillis()}"), "backup.pbu")
                if (!backupStorage.copyFile(filePath, tempFile)) {
                    emit(RestoreResult.Failure(RestoreError.InvalidBackupFile()))
                    return@flow
                }
                tempFile
            } else {
                File(filePath)
            }
            
            if (!backupFile.exists() || !CompressionHelper.isValidZip(backupFile)) {
                emit(RestoreResult.Failure(RestoreError.InvalidBackupFile()))
                return@flow
            }
            
            // 2. 解压到临时目录
            val tempDir = backupStorage.createTempDirectory("restore_temp_${System.currentTimeMillis()}")
            CompressionHelper.decompressZip(backupFile, tempDir)
            
            // 3. 读取元数据
            val metadataFile = File(tempDir, "metadata.json")
            if (!metadataFile.exists()) {
                emit(RestoreResult.Failure(RestoreError.InvalidBackupFile("缺少元数据文件")))
                tempDir.deleteRecursively()
                return@flow
            }
            
            val metadata = json.decodeFromString<BackupMetadata>(metadataFile.readText())
            
            // 4. 验证校验和
            emit(RestoreResult.Progress(
                currentModule = BackupModule.SETTINGS,
                progress = 0.1f,
                message = "验证数据完整性..."
            ))
            
            val checksumsFile = File(tempDir, "checksums.json")
            if (checksumsFile.exists()) {
                val checksums = json.decodeFromString<Map<String, String>>(checksumsFile.readText())
                if (!verifyChecksums(tempDir, checksums)) {
                    emit(RestoreResult.Failure(RestoreError.ChecksumMismatch()))
                    tempDir.deleteRecursively()
                    return@flow
                }
            }
            
            // 5. 确定要恢复的模块
            val modulesToRestore = modules ?: metadata.modules.mapNotNull {
                try {
                    BackupModule.valueOf(it)
                } catch (e: Exception) {
                    null
                }
            }.toSet()
            
            var successRecords = 0
            var failedRecords = 0
            val restoredModules = mutableListOf<BackupModule>()
            
            // 6. 恢复各模块
            if (BackupModule.SETTINGS in modulesToRestore) {
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.SETTINGS,
                    progress = 0.3f,
                    message = "正在恢复应用设置..."
                ))
                
                try {
                    val settingsFile = File(tempDir, "settings.json")
                    if (settingsFile.exists()) {
                        val settingsData = json.decodeFromString<SettingsBackupData>(settingsFile.readText())
                        settingsDataSource.importData(settingsData)
                        successRecords++
                        restoredModules.add(BackupModule.SETTINGS)
                    }
                } catch (e: Exception) {
                    failedRecords++
                }
            }
            
            if (BackupModule.CREDENTIALS in modulesToRestore) {
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.CREDENTIALS,
                    progress = 0.6f,
                    message = "正在恢复登录信息..."
                ))
                
                try {
                    val credentialsFile = File(tempDir, "credentials.json")
                    if (credentialsFile.exists()) {
                        val credentialsData = json.decodeFromString<CredentialsBackupData>(credentialsFile.readText())
                        credentialsDataSource.importData(credentialsData)
                        successRecords++
                        restoredModules.add(BackupModule.CREDENTIALS)
                    }
                } catch (e: Exception) {
                    failedRecords++
                }
            }
            
            // 7. 清理临时文件
            tempDir.deleteRecursively()
            
            // 8. 返回结果
            if (failedRecords > 0 && successRecords == 0) {
                emit(RestoreResult.Failure(RestoreError.UnknownError("所有模块恢复失败")))
            } else if (failedRecords > 0) {
                emit(RestoreResult.Failure(RestoreError.PartialRestore(
                    message = "部分模块恢复失败",
                    failedModules = modulesToRestore.filter { it !in restoredModules }
                )))
            } else {
                emit(RestoreResult.Success(
                    restoredModules = restoredModules,
                    statistics = RestoreStatistics(
                        totalRecords = successRecords + failedRecords,
                        successRecords = successRecords,
                        skippedRecords = 0,
                        failedRecords = failedRecords
                    )
                ))
            }
            
        } catch (e: Exception) {
            emit(RestoreResult.Failure(RestoreError.UnknownError(e.message ?: "未知错误")))
        }
    }
    
    /**
     * 验证文件校验和
     */
    private fun verifyChecksums(tempDir: File, checksums: Map<String, String>): Boolean {
        return try {
            checksums.all { (fileName, expectedChecksum) ->
                val file = File(tempDir, fileName)
                if (!file.exists()) return@all false
                
                val actualChecksum = ChecksumCalculator.calculateChecksum(file.readText())
                actualChecksum == expectedChecksum
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 验证备份文件
     */
    suspend fun validateBackupFile(filePath: String): Result<BackupMetadata> {
        return try {
            println("RestoreManager: validateBackupFile - filePath=$filePath")
            // 处理content:// URI或普通文件路径
            val backupFile = if (filePath.startsWith("content://")) {
                println("RestoreManager: Detected content URI, copying to temp file")
                // 从content URI复制到临时文件
                val tempFile = File(backupStorage.createTempDirectory("validate_uri_${System.currentTimeMillis()}"), "backup.pbu")
                if (!backupStorage.copyFile(filePath, tempFile)) {
                    println("RestoreManager: Failed to copy file from content URI")
                    return Result.failure(Exception("无法读取选择的文件"))
                }
                println("RestoreManager: Successfully copied to temp file: ${tempFile.absolutePath}, size=${tempFile.length()}")
                tempFile
            } else {
                println("RestoreManager: Using file path directly")
                File(filePath)
            }
            
            println("RestoreManager: Backup file exists=${backupFile.exists()}, size=${backupFile.length()}")
            if (!backupFile.exists() || !CompressionHelper.isValidZip(backupFile)) {
                println("RestoreManager: File does not exist or is not a valid ZIP")
                return Result.failure(Exception("无效的备份文件"))
            }
            
            val tempDir = backupStorage.createTempDirectory("validate_temp_${System.currentTimeMillis()}")
            println("RestoreManager: Decompressing to temp dir: ${tempDir.absolutePath}")
            CompressionHelper.decompressZip(backupFile, tempDir)
            
            val metadataFile = File(tempDir, "metadata.json")
            println("RestoreManager: Metadata file exists=${metadataFile.exists()}")
            if (!metadataFile.exists()) {
                tempDir.deleteRecursively()
                return Result.failure(Exception("缺少元数据文件"))
            }
            
            val metadata = json.decodeFromString<BackupMetadata>(metadataFile.readText())
            println("RestoreManager: Successfully parsed metadata: version=${metadata.version}, timestamp=${metadata.timestamp}")
            tempDir.deleteRecursively()
            
            Result.success(metadata)
        } catch (e: Exception) {
            println("RestoreManager: Error validating backup file: ${e.message}")
            e.printStackTrace()
            Result.failure(e)
        }
    }
}
