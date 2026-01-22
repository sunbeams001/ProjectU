package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.BackupModule
import com.projectu.shared.domain.model.RestoreResult
import com.projectu.shared.domain.repository.BackupRestoreRepository
import kotlinx.coroutines.flow.Flow

/**
 * 恢复备份用例
 */
class RestoreBackupUseCase(
    private val repository: BackupRestoreRepository
) {
    
    /**
     * 执行恢复
     */
    operator fun invoke(
        filePath: String,
        modules: Set<BackupModule>? = null
    ): Flow<RestoreResult> {
        return repository.restoreBackup(filePath, modules)
    }
}
