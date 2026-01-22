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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * 备份管理器
 * 协调P0模块的备份流程
 */
class BackupManager(
    private val settingsDataSource: SettingsBackupDataSource,
    private val credentialsDataSource: CredentialsBackupDataSource,
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
                message = "准备备份..."
            ))
            
            val tempDir = backupStorage.createTempDirectory("backup_temp_${System.currentTimeMillis()}")
            
            val checksums = mutableMapOf<String, String>()
            val moduleSizes = mutableMapOf<String, Long>()
            var totalRecords = 0
            
            // 2. 备份各模块
            if (BackupModule.SETTINGS in config.modules) {
                emit(BackupResult.Progress(
                    currentModule = BackupModule.SETTINGS,
                    progress = 0.25f,
                    message = "正在备份应用设置..."
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
                emit(BackupResult.Progress(
                    currentModule = BackupModule.CREDENTIALS,
                    progress = 0.5f,
                    message = "正在备份登录信息..."
                ))
                
                val credentialsData = credentialsDataSource.exportData()
                val credentialsJson = json.encodeToString(credentialsData)
                val credentialsFile = File(tempDir, "credentials.json")
                credentialsFile.writeText(credentialsJson)
                
                checksums["credentials.json"] = ChecksumCalculator.calculateChecksum(credentialsJson)
                moduleSizes["CREDENTIALS"] = credentialsJson.toByteArray().size.toLong()
                totalRecords += 1
            }
            
            // 3. 生成元数据
            emit(BackupResult.Progress(
                currentModule = BackupModule.SETTINGS,
                progress = 0.75f,
                message = "生成元数据..."
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
                statistics = BackupStatistics(totalRecords = totalRecords),
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
            val fileName = "backup_$backupId.pbu"
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
                BackupError.UnknownError(e.message ?: "未知错误")
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
