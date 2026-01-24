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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 备份管理器
 * 协调P0、P1和P2模块的备份流程
 */
class BackupManager(
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
        prettyPrint = true
        encodeDefaults = true
    }
    
    /**
     * 创建备份
     */
    fun createBackup(config: BackupConfig): Flow<BackupResult> = flow {
        try {
            
            // 1. 创建临时目录
            emit(BackupResult.Progress(
                currentModule = BackupModule.SETTINGS,
                progress = 0f,
                message = "Preparing backup..."
            ))
            
            val tempDir = backupStorage.createTempDirectory("backup_temp_${System.currentTimeMillis()}")
            
            val checksums = mutableMapOf<String, String>()
            val moduleSizes = mutableMapOf<String, Long>()
            var totalRecords = 0
            var blockRulesCount = 0
            var browseHistoryCount = 0
            var downloadRulesCount = 0
            var downloadTasksCount = 0
            var searchHistoryCount = 0
            
            val totalModules = config.modules.size
            var currentModuleIndex = 0
            
            // 2. 备份各模块
            if (BackupModule.SETTINGS in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.SETTINGS,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up app settings..."
                ))
                
                val settingsData = settingsDataSource.exportData()
                val settingsJson = json.encodeToString(settingsData)
                val settingsFile = File(tempDir, "settings.json")
                settingsFile.writeText(settingsJson)
                
                checksums["settings.json"] = ChecksumCalculator.calculateChecksum(settingsJson)
                moduleSizes["SETTINGS"] = settingsJson.toByteArray().size.toLong()
                totalRecords += 1
            }
            
            if (BackupModule.CREDENTIALS in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.CREDENTIALS,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up login info..."
                ))
                
                val credentialsData = credentialsDataSource.exportData()
                val credentialsJson = json.encodeToString(credentialsData)
                val credentialsFile = File(tempDir, "credentials.json")
                credentialsFile.writeText(credentialsJson)
                
                checksums["credentials.json"] = ChecksumCalculator.calculateChecksum(credentialsJson)
                moduleSizes["CREDENTIALS"] = credentialsJson.toByteArray().size.toLong()
                totalRecords += 1
            }
            
            // P1模块：屏蔽列表
            if (BackupModule.BLOCK_RULES in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.BLOCK_RULES,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up block list..."
                ))
                
                val blockRulesData = blockRulesDataSource.exportData()
                val blockRulesJson = json.encodeToString(blockRulesData)
                val blockRulesFile = File(tempDir, "block_rules.json")
                blockRulesFile.writeText(blockRulesJson)
                
                checksums["block_rules.json"] = ChecksumCalculator.calculateChecksum(blockRulesJson)
                moduleSizes["BLOCK_RULES"] = blockRulesJson.toByteArray().size.toLong()
                blockRulesCount = blockRulesData.rules.size
                totalRecords += blockRulesCount
            }
            
            // P1模块：浏览历史
            if (BackupModule.BROWSE_HISTORY in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.BROWSE_HISTORY,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up browse history..."
                ))
                
                val browseHistoryData = browseHistoryDataSource.exportData()
                val browseHistoryJson = json.encodeToString(browseHistoryData)
                val browseHistoryFile = File(tempDir, "browse_history.json")
                browseHistoryFile.writeText(browseHistoryJson)
                
                checksums["browse_history.json"] = ChecksumCalculator.calculateChecksum(browseHistoryJson)
                moduleSizes["BROWSE_HISTORY"] = browseHistoryJson.toByteArray().size.toLong()
                browseHistoryCount = browseHistoryData.history.size
                totalRecords += browseHistoryCount
            }
            
            // P1模块：下载路径规则
            if (BackupModule.DOWNLOAD_RULES in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.DOWNLOAD_RULES,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up download path rules..."
                ))
                
                val downloadRulesData = downloadRulesDataSource.exportData()
                val downloadRulesJson = json.encodeToString(downloadRulesData)
                val downloadRulesFile = File(tempDir, "download_rules.json")
                downloadRulesFile.writeText(downloadRulesJson)
                
                checksums["download_rules.json"] = ChecksumCalculator.calculateChecksum(downloadRulesJson)
                moduleSizes["DOWNLOAD_RULES"] = downloadRulesJson.toByteArray().size.toLong()
                downloadRulesCount = downloadRulesData.rules.size
                totalRecords += downloadRulesCount
            }
            
            // P2模块：下载任务
            if (BackupModule.DOWNLOAD_TASKS in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.DOWNLOAD_TASKS,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up download tasks..."
                ))
                
                val downloadTasksJson = downloadTasksDataSource.exportData()
                val downloadTasksFile = File(tempDir, "download_tasks.json")
                downloadTasksFile.writeText(downloadTasksJson)
                
                checksums["download_tasks.json"] = ChecksumCalculator.calculateChecksum(downloadTasksJson)
                moduleSizes["DOWNLOAD_TASKS"] = downloadTasksJson.toByteArray().size.toLong()
                downloadTasksCount = com.projectu.shared.data.backup.DownloadTasksBackupData.serializer().let {
                    json.decodeFromString(it, downloadTasksJson).tasks.size
                }
                totalRecords += downloadTasksCount
            }
            
            // P2模块：搜索历史
            if (BackupModule.SEARCH_HISTORY in config.modules) {
                currentModuleIndex++
                emit(BackupResult.Progress(
                    currentModule = BackupModule.SEARCH_HISTORY,
                    progress = currentModuleIndex.toFloat() / totalModules,
                    message = "Backing up search history..."
                ))
                
                val searchHistoryJson = searchHistoryDataSource.exportData()
                val searchHistoryFile = File(tempDir, "search_history.json")
                searchHistoryFile.writeText(searchHistoryJson)
                
                checksums["search_history.json"] = ChecksumCalculator.calculateChecksum(searchHistoryJson)
                moduleSizes["SEARCH_HISTORY"] = searchHistoryJson.toByteArray().size.toLong()
                searchHistoryCount = com.projectu.shared.data.backup.SearchHistoryBackupData.serializer().let {
                    json.decodeFromString(it, searchHistoryJson).items.size
                }
                totalRecords += searchHistoryCount
            }
            
            // 3. 生成元数据
            emit(BackupResult.Progress(
                currentModule = BackupModule.SETTINGS,
                progress = 0.9f,
                message = "Generating metadata..."
            ))
            
            val metadata = BackupMetadata(
                version = BACKUP_FORMAT_VERSION,
                appVersion = getAppVersion(),
                databaseVersion = getDatabaseVersion(),
                timestamp = System.currentTimeMillis(),
                platform = getPlatform(),
                deviceInfo = getDeviceInfo(),
                modules = config.modules.map { it.name },
                moduleSizes = moduleSizes,
                encryption = EncryptionInfo(enabled = false),
                statistics = BackupStatistics(
                    totalRecords = totalRecords,
                    blockRulesCount = blockRulesCount,
                    browseHistoryCount = browseHistoryCount,
                    downloadRulesCount = downloadRulesCount,
                    downloadTasksCount = downloadTasksCount,
                    searchHistoryCount = searchHistoryCount
                ),
                checksum = ChecksumCalculator.calculateOverallChecksum(checksums),
                comment = config.comment
            )
            
            val metadataJson = json.encodeToString(metadata)
            File(tempDir, "metadata.json").writeText(metadataJson)
            
            val checksumsJson = json.encodeToString(checksums)
            File(tempDir, "checksums.json").writeText(checksumsJson)
            
            // 4. 压缩为ZIP文件
            emit(BackupResult.Progress(
                currentModule = BackupModule.SETTINGS,
                progress = 0.9f,
                message = "正在压缩备份文件..."
            ))
            
            val backupId = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "backup_$backupId.pbu.zip"
            val zipFile = File(tempDir.parentFile, fileName)
            
            CompressionHelper.compressToZip(tempDir, zipFile)
            
            // 5. 移动到目标位置
            val finalPath = backupStorage.saveBackupFile(zipFile, fileName)
            
            // 6. 清理临时文件
            tempDir.deleteRecursively()
            zipFile.delete()
            
            // 7. 返回成功结果
            val backupInfo = BackupInfo(
                id = backupId,
                fileName = fileName,
                filePath = finalPath,
                metadata = metadata,
                fileSize = File(finalPath).length()
            )
            
            emit(BackupResult.Success(backupInfo))
            
        } catch (e: Exception) {
            emit(BackupResult.Failure(
                BackupError.UnknownError(e.message ?: "Unknown error")
            ))
        }
    }
    
    private fun getAppVersion(): String = "1.0.0" // TODO: 从build config获取
    
    private fun getDatabaseVersion(): Int = 6 // TODO: 从AppDatabase获取
    
    private fun getPlatform(): String {
        return System.getProperty("os.name") ?: "Unknown"
    }
    
    private fun getDeviceInfo(): DeviceInfo? {
        return try {
            DeviceInfo(
                os = System.getProperty("os.name") ?: "Unknown",
                model = System.getProperty("os.arch") ?: "Unknown"
            )
        } catch (e: Exception) {
            null
        }
    }
    
    companion object {
        private const val BACKUP_FORMAT_VERSION = "1.0.0"
    }
}
