package com.projectu.shared.data.backup.manager

import com.projectu.shared.data.backup.DownloadTasksBackupDataSource
import com.projectu.shared.data.backup.SearchHistoryBackupDataSource
import com.projectu.shared.data.backup.datasource.*
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
 * 协调P0、P1和P2模块的恢复流程
 */
class RestoreManager(
    private val settingsDataSource: SettingsBackupDataSource,
    private val credentialsDataSource: CredentialsBackupDataSource,
    private val blockRulesDataSource: BlockRulesBackupDataSource,
    private val browseHistoryDataSource: BrowseHistoryBackupDataSource,
    private val downloadRulesDataSource: DownloadRulesBackupDataSource,
    private val downloadTasksDataSource: DownloadTasksBackupDataSource,
    private val searchHistoryDataSource: SearchHistoryBackupDataSource,
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
                val tempFile = File(backupStorage.createTempDirectory("restore_uri_${System.currentTimeMillis()}"), "backup.pbu.zip")
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
            
            val totalModules = modulesToRestore.size
            var currentModuleIndex = 0
            
            // 6. 恢复各模块
            if (BackupModule.SETTINGS in modulesToRestore) {
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.SETTINGS,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
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
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.CREDENTIALS,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
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
            
            // P1模块：屏蔽列表
            if (BackupModule.BLOCK_RULES in modulesToRestore) {
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.BLOCK_RULES,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
                    message = "正在恢复屏蔽列表..."
                ))
                
                try {
                    val blockRulesFile = File(tempDir, "block_rules.json")
                    if (blockRulesFile.exists()) {
                        val blockRulesData = json.decodeFromString<BlockRulesBackupData>(blockRulesFile.readText())
                        blockRulesDataSource.importData(blockRulesData)
                        successRecords += blockRulesData.rules.size
                        restoredModules.add(BackupModule.BLOCK_RULES)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedRecords++
                }
            }
            
            // P1模块：浏览历史
            if (BackupModule.BROWSE_HISTORY in modulesToRestore) {
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.BROWSE_HISTORY,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
                    message = "正在恢复浏览历史..."
                ))
                
                try {
                    val browseHistoryFile = File(tempDir, "browse_history.json")
                    if (browseHistoryFile.exists()) {
                        val browseHistoryData = json.decodeFromString<BrowseHistoryBackupData>(browseHistoryFile.readText())
                        browseHistoryDataSource.importData(browseHistoryData)
                        successRecords += browseHistoryData.history.size
                        restoredModules.add(BackupModule.BROWSE_HISTORY)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedRecords++
                }
            }
            
            // P1模块：下载路径规则
            if (BackupModule.DOWNLOAD_RULES in modulesToRestore) {
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.DOWNLOAD_RULES,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
                    message = "正在恢复下载路径规则..."
                ))
                
                try {
                    val downloadRulesFile = File(tempDir, "download_rules.json")
                    if (downloadRulesFile.exists()) {
                        val downloadRulesData = json.decodeFromString<DownloadRulesBackupData>(downloadRulesFile.readText())
                        downloadRulesDataSource.importData(downloadRulesData)
                        successRecords += downloadRulesData.rules.size
                        restoredModules.add(BackupModule.DOWNLOAD_RULES)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedRecords++
                }
            }
            
            // P2模块：下载任务
            if (BackupModule.DOWNLOAD_TASKS in modulesToRestore) {
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.DOWNLOAD_TASKS,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
                    message = "正在恢复下载任务..."
                ))
                
                try {
                    val downloadTasksFile = File(tempDir, "download_tasks.json")
                    if (downloadTasksFile.exists()) {
                        val downloadTasksJson = downloadTasksFile.readText()
                        downloadTasksDataSource.importData(downloadTasksJson)
                        val downloadTasksData = json.decodeFromString<com.projectu.shared.data.backup.DownloadTasksBackupData>(downloadTasksJson)
                        successRecords += downloadTasksData.tasks.size
                        restoredModules.add(BackupModule.DOWNLOAD_TASKS)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    failedRecords++
                }
            }
            
            // P2模块：搜索历史
            if (BackupModule.SEARCH_HISTORY in modulesToRestore) {
                currentModuleIndex++
                emit(RestoreResult.Progress(
                    currentModule = BackupModule.SEARCH_HISTORY,
                    progress = 0.2f + (currentModuleIndex.toFloat() / totalModules) * 0.7f,
                    message = "正在恢复搜索历史..."
                ))
                
                try {
                    val searchHistoryFile = File(tempDir, "search_history.json")
                    if (searchHistoryFile.exists()) {
                        val searchHistoryJson = searchHistoryFile.readText()
                        searchHistoryDataSource.importData(searchHistoryJson)
                        val searchHistoryData = json.decodeFromString<com.projectu.shared.data.backup.SearchHistoryBackupData>(searchHistoryJson)
                        successRecords += searchHistoryData.items.size
                        restoredModules.add(BackupModule.SEARCH_HISTORY)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
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
                val tempFile = File(backupStorage.createTempDirectory("validate_uri_${System.currentTimeMillis()}"), "backup.pbu.zip")
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
