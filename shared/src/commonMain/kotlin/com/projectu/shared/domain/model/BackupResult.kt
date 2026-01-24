package com.projectu.shared.domain.model

/**
 * 备份结果
 */
sealed class BackupResult {
    /**
     * 备份成功
     */
    data class Success(
        val backupInfo: BackupInfo
    ) : BackupResult()
    
    /**
     * 备份失败
     */
    data class Failure(
        val error: BackupError
    ) : BackupResult()
    
    /**
     * 备份进行中
     */
    data class Progress(
        val currentModule: BackupModule,
        val progress: Float,
        val message: String
    ) : BackupResult()
}

/**
 * 备份信息
 */
data class BackupInfo(
    val id: String,
    val fileName: String,
    val filePath: String,
    val metadata: BackupMetadata,
    val fileSize: Long
)

/**
 * 备份错误
 */
sealed class BackupError(open val message: String) {
    data class StoragePermissionDenied(override val message: String = "Storage permission denied") : BackupError(message)
    data class InsufficientSpace(override val message: String = "Insufficient storage space") : BackupError(message)
    data class DatabaseError(override val message: String) : BackupError(message)
    data class IOError(override val message: String) : BackupError(message)
    data class UnknownError(override val message: String) : BackupError(message)
}
