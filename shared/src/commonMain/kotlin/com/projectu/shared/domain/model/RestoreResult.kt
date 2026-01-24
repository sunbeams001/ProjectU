package com.projectu.shared.domain.model

/**
 * 恢复结果
 */
sealed class RestoreResult {
    /**
     * 恢复成功
     */
    data class Success(
        val restoredModules: List<BackupModule>,
        val statistics: RestoreStatistics
    ) : RestoreResult()
    
    /**
     * 恢复失败
     */
    data class Failure(
        val error: RestoreError
    ) : RestoreResult()
    
    /**
     * 恢复进行中
     */
    data class Progress(
        val currentModule: BackupModule,
        val progress: Float,
        val message: String
    ) : RestoreResult()
}

/**
 * 恢复统计信息
 */
data class RestoreStatistics(
    val totalRecords: Int,
    val successRecords: Int,
    val skippedRecords: Int,
    val failedRecords: Int
)

/**
 * 恢复错误
 */
sealed class RestoreError(open val message: String) {
    data class InvalidBackupFile(override val message: String = "Invalid backup file") : RestoreError(message)
    data class ChecksumMismatch(override val message: String = "Data verification failed") : RestoreError(message)
    data class IncompatibleVersion(
        override val message: String,
        val backupVersion: String,
        val currentVersion: String
    ) : RestoreError(message)
    data class DatabaseError(override val message: String) : RestoreError(message)
    data class PartialRestore(
        override val message: String,
        val failedModules: List<BackupModule>
    ) : RestoreError(message)
    data class UnknownError(override val message: String) : RestoreError(message)
}
