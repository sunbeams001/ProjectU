package com.projectu.shared.domain.usecase

import com.projectu.shared.domain.model.BackupConfig
import com.projectu.shared.domain.model.BackupResult
import com.projectu.shared.domain.repository.BackupRestoreRepository
import kotlinx.coroutines.flow.Flow

/**
 * 创建备份用例
 */
class CreateBackupUseCase(
    private val repository: BackupRestoreRepository
) {
    
    /**
     * 执行备份
     */
    operator fun invoke(config: BackupConfig): Flow<BackupResult> {
        return repository.createBackup(config)
    }
}
