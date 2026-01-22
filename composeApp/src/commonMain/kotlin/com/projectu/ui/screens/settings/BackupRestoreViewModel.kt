package com.projectu.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.domain.model.BackupConfig
import com.projectu.shared.domain.model.BackupError
import com.projectu.shared.domain.model.BackupInfo
import com.projectu.shared.domain.model.BackupMetadata
import com.projectu.shared.domain.model.BackupModule
import com.projectu.shared.domain.model.BackupResult
import com.projectu.shared.domain.model.RestoreError
import com.projectu.shared.domain.model.RestoreResult
import com.projectu.shared.domain.usecase.CreateBackupUseCase
import com.projectu.shared.domain.usecase.RestoreBackupUseCase
import com.projectu.shared.domain.repository.BackupRestoreRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

/**
 * 备份与恢复 ViewModel
 * 
 * MVI 架构：
 * - State: 包含备份历史、进度状态、结果消息等
 * - Intent: 用户操作意图（创建备份、选择文件、恢复等）
 * - Effect: 单次事件（成功提示、错误提示、导航等）
 */
class BackupRestoreViewModel(
    private val createBackupUseCase: CreateBackupUseCase,
    private val restoreBackupUseCase: RestoreBackupUseCase,
    private val backupRestoreRepository: BackupRestoreRepository
) : ViewModel() {
    
    // UI State
    private val _state = MutableStateFlow(BackupRestoreState())
    val state: StateFlow<BackupRestoreState> = _state.asStateFlow()
    
    // Effects (Single-shot events)
    private val _effect = MutableSharedFlow<BackupRestoreEffect>()
    val effect: SharedFlow<BackupRestoreEffect> = _effect.asSharedFlow()
    
    init {
        // 初始化时加载备份历史和检查目录访问权限
        checkBackupDirectoryAccess()
        loadBackupHistory()
    }
    
    /**
     * 检查备份目录访问权限
     */
    private fun checkBackupDirectoryAccess() {
        viewModelScope.launch {
            try {
                val hasAccess = backupRestoreRepository.hasBackupDirectoryAccess()
                val directoryUri = backupRestoreRepository.getBackupDirectoryUri()
                val readablePath = directoryUri?.let { extractReadablePath(it) }
                _state.update { 
                    it.copy(
                        hasBackupDirectoryAccess = hasAccess,
                        backupDirectoryPath = readablePath
                    ) 
                }
            } catch (e: Exception) {
                // 忽略错误，默认为false
            }
        }
    }
    
    /**
     * 处理用户意图
     */
    fun handleIntent(intent: BackupRestoreIntent) {
        when (intent) {
            is BackupRestoreIntent.CreateBackup -> createBackup(intent.config)
            is BackupRestoreIntent.SelectBackupFile -> selectBackupFile(intent.filePath)
            is BackupRestoreIntent.RestoreBackup -> restoreBackup()
            is BackupRestoreIntent.DeleteBackup -> deleteBackup(intent.backupInfo)
            is BackupRestoreIntent.DismissDialog -> dismissDialog()
            is BackupRestoreIntent.RefreshBackupList -> loadBackupHistory()
            is BackupRestoreIntent.SetBackupDirectory -> setBackupDirectory(intent.treeUri)
        }
    }
    
    /**
     * 设置备份目录URI（SAF授权）
     */
    private fun setBackupDirectory(treeUri: String) {
        viewModelScope.launch {
            try {
                val success = backupRestoreRepository.setBackupDirectoryUri(treeUri)
                if (success) {
                    val readablePath = extractReadablePath(treeUri)
                    // 更新状态
                    _state.update { 
                        it.copy(
                            hasBackupDirectoryAccess = true,
                            backupDirectoryPath = readablePath
                        ) 
                    }
                    // 刷新备份列表
                    loadBackupHistory()
                    _effect.emit(BackupRestoreEffect.ShowBackupDirectorySet)
                } else {
                    _effect.emit(BackupRestoreEffect.ShowSetupFailed(null))
                }
            } catch (e: Exception) {
                _effect.emit(BackupRestoreEffect.ShowSetupFailed(e.message))
            }
        }
    }
    
    /**
     * 创建备份
     */
    private fun createBackup(config: BackupConfig) {
        viewModelScope.launch {
            _state.update { it.copy(isCreatingBackup = true, currentProgress = null) }
            
            createBackupUseCase(config)
                .catch { error ->
                    _state.update { it.copy(isCreatingBackup = false, currentProgress = null) }
                    _effect.emit(BackupRestoreEffect.ShowBackupFailed(
                        error.message ?: "Unknown error"
                    ))
                }
                .collect { result ->
                    when (result) {
                        is BackupResult.Progress -> {
                            _state.update { it.copy(currentProgress = result) }
                        }
                        is BackupResult.Success -> {
                            _state.update { 
                                it.copy(
                                    isCreatingBackup = false,
                                    currentProgress = null
                                )
                            }
                            // 刷新备份列表
                            loadBackupHistory()
                            // 显示成功提示
                            _effect.emit(BackupRestoreEffect.ShowBackupCreated(
                                result.backupInfo.filePath
                            ))
                        }
                        is BackupResult.Failure -> {
                            _state.update { 
                                it.copy(
                                    isCreatingBackup = false,
                                    currentProgress = null
                                )
                            }
                            _effect.emit(BackupRestoreEffect.ShowBackupFailed(
                                result.error.getDisplayMessage()
                            ))
                        }
                    }
                }
        }
    }
    
    /**
     * 选择备份文件
     */
    private fun selectBackupFile(filePath: String) {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isValidatingFile = true) }
                
                val validationResult = backupRestoreRepository.validateBackupFile(filePath)
                
                validationResult.onSuccess { metadata ->
                    _state.update { 
                        it.copy(
                            isValidatingFile = false,
                            selectedBackupFile = filePath,
                            selectedBackupMetadata = metadata
                        )
                    }
                }.onFailure { error ->
                    _state.update { it.copy(isValidatingFile = false) }
                    _effect.emit(BackupRestoreEffect.ShowInvalidFile(
                        error.message
                    ))
                }
            } catch (e: Exception) {
                _state.update { it.copy(isValidatingFile = false) }
                _effect.emit(BackupRestoreEffect.ShowFileError(
                    e.message
                ))
            }
        }
    }
    
    /**
     * 恢复备份
     */
    private fun restoreBackup() {
        val filePath = _state.value.selectedBackupFile
        if (filePath == null) {
            viewModelScope.launch {
                _effect.emit(BackupRestoreEffect.ShowNoFileSelected)
            }
            return
        }
        
        viewModelScope.launch {
            _state.update { it.copy(isRestoringBackup = true, currentRestoreProgress = null) }
            
            restoreBackupUseCase(filePath)
                .catch { error ->
                    _state.update { it.copy(isRestoringBackup = false, currentRestoreProgress = null) }
                    _effect.emit(BackupRestoreEffect.ShowRestoreFailed(
                        error.message ?: "Unknown error"
                    ))
                }
                .collect { result ->
                    when (result) {
                        is RestoreResult.Progress -> {
                            _state.update { it.copy(currentRestoreProgress = result) }
                        }
                        is RestoreResult.Success -> {
                            _state.update { 
                                it.copy(
                                    isRestoringBackup = false,
                                    currentRestoreProgress = null
                                )
                            }
                            _effect.emit(BackupRestoreEffect.ShowRestoreComplete(
                                result.restoredModules.size,
                                result.statistics.successRecords
                            ))
                            // 触发应用重启
                            _effect.emit(BackupRestoreEffect.RestartApp)
                        }
                        is RestoreResult.Failure -> {
                            _state.update { 
                                it.copy(
                                    isRestoringBackup = false,
                                    currentRestoreProgress = null
                                )
                            }
                            _effect.emit(BackupRestoreEffect.ShowRestoreFailed(
                                result.error.getDisplayMessage()
                            ))
                        }
                    }
                }
        }
    }
    
    /**
     * 删除备份
     */
    private fun deleteBackup(backupInfo: BackupInfo) {
        viewModelScope.launch {
            try {
                backupRestoreRepository.deleteBackup(backupInfo.id)
                loadBackupHistory()
                _effect.emit(BackupRestoreEffect.ShowBackupDeleted)
            } catch (e: Exception) {
                _effect.emit(BackupRestoreEffect.ShowDeleteFailed(
                    e.message
                ))
            }
        }
    }
    
    /**
     * 加载备份历史
     */
    private fun loadBackupHistory() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoadingBackups = true) }
                
                val result = backupRestoreRepository.listBackups()
                
                result.onSuccess { backups ->
                    _state.update { 
                        it.copy(
                            isLoadingBackups = false,
                            backupHistory = backups
                        )
                    }
                }.onFailure { error ->
                    println("BackupRestoreViewModel: Failed to load backup history: ${error.message}")
                    error.printStackTrace()
                    _state.update { it.copy(isLoadingBackups = false) }
                    _effect.emit(BackupRestoreEffect.ShowLoadFailed(
                        error.message
                    ))
                }
            } catch (e: Exception) {
                println("BackupRestoreViewModel: Exception while loading backup history: ${e.message}")
                e.printStackTrace()
                _state.update { it.copy(isLoadingBackups = false) }
                _effect.emit(BackupRestoreEffect.ShowLoadFailed(
                    e.message
                ))
            }
        }
    }
    
    /**
     * 从URI中提取可读的路径
     * content://com.android.externalstorage.documents/tree/primary%3ADownload%2FProjectU%2FBackups
     * -> /storage/emulated/0/Download/ProjectU/Backups
     */
    private fun extractReadablePath(treeUri: String): String {
        return try {
            // 从SAF URI中提取路径部分
            if (treeUri.contains("tree/primary")) {
                // 提取 primary: 或 primary%3A 后面的路径
                val pathPart = treeUri.substringAfter("tree/primary")
                    .removePrefix(":")
                    .removePrefix("%3A")
                    .replace("%2F", "/")
                    .replace("%3A", ":")
                "/storage/emulated/0/$pathPart"
            } else if (treeUri.contains("tree/")) {
                // 处理其他存储位置
                val afterTree = treeUri.substringAfter("tree/")
                val path = if (afterTree.contains(":")) {
                    afterTree.substringAfter(":")
                        .replace("%2F", "/")
                        .replace("%3A", ":")
                } else {
                    afterTree.replace("%2F", "/")
                }
                path
            } else {
                treeUri
            }
        } catch (e: Exception) {
            treeUri
        }
    }
    
    /**
     * 关闭对话框
     */
    private fun dismissDialog() {
        _state.update { it.copy(currentProgress = null, currentRestoreProgress = null) }
    }
}

/**
 * UI State
 */
data class BackupRestoreState(
    // 备份历史
    val backupHistory: List<BackupInfo> = emptyList(),
    val isLoadingBackups: Boolean = false,
    
    // 创建备份
    val isCreatingBackup: Boolean = false,
    val currentProgress: BackupResult.Progress? = null,
    
    // 恢复备份
    val selectedBackupFile: String? = null,
    val selectedBackupMetadata: BackupMetadata? = null,
    val isValidatingFile: Boolean = false,
    val isRestoringBackup: Boolean = false,
    val currentRestoreProgress: RestoreResult.Progress? = null,
    
    // 备份目录访问权限
    val hasBackupDirectoryAccess: Boolean = false,
    val backupDirectoryPath: String? = null
)

/**
 * User Intent
 */
sealed class BackupRestoreIntent {
    data class CreateBackup(val config: BackupConfig) : BackupRestoreIntent()
    data class SelectBackupFile(val filePath: String) : BackupRestoreIntent()
    data object RestoreBackup : BackupRestoreIntent()
    data class DeleteBackup(val backupInfo: BackupInfo) : BackupRestoreIntent()
    data object DismissDialog : BackupRestoreIntent()
    data object RefreshBackupList : BackupRestoreIntent()
    data class SetBackupDirectory(val treeUri: String) : BackupRestoreIntent()
}

/**
 * Side Effect (Single-shot events)
 */
sealed class BackupRestoreEffect {
    data class ShowBackupCreated(val filePath: String) : BackupRestoreEffect()
    data class ShowRestoreComplete(val modulesCount: Int, val recordsCount: Int) : BackupRestoreEffect()
    data class ShowSetupFailed(val message: String?) : BackupRestoreEffect()
    data class ShowBackupFailed(val message: String) : BackupRestoreEffect()
    data class ShowRestoreFailed(val message: String) : BackupRestoreEffect()
    data class ShowInvalidFile(val message: String?) : BackupRestoreEffect()
    data class ShowFileError(val message: String?) : BackupRestoreEffect()
    data object ShowNoFileSelected : BackupRestoreEffect()
    data class ShowDeleteFailed(val message: String?) : BackupRestoreEffect()
    data class ShowLoadFailed(val message: String?) : BackupRestoreEffect()
    data class ShowMessage(val message: String) : BackupRestoreEffect()
    data object ShowBackupDirectorySet : BackupRestoreEffect()
    data object ShowBackupDeleted : BackupRestoreEffect()
    data object RestartApp : BackupRestoreEffect()
}

/**
 * 将错误转换为用户友好的消息
 */
private fun BackupError.getDisplayMessage(): String {
    return when (this) {
        is BackupError.StoragePermissionDenied -> "Storage permission denied: $message"
        is BackupError.InsufficientSpace -> "Insufficient storage space: $message"
        is BackupError.DatabaseError -> "Database error: $message"
        is BackupError.IOError -> "IO error: $message"
        is BackupError.UnknownError -> "Unknown error: $message"
    }
}

private fun RestoreError.getDisplayMessage(): String {
    return when (this) {
        is RestoreError.InvalidBackupFile -> "Invalid backup file: $message"
        is RestoreError.ChecksumMismatch -> "Backup file corrupted: $message"
        is RestoreError.IncompatibleVersion -> "Incompatible backup version: $backupVersion (current: $currentVersion)"
        is RestoreError.DatabaseError -> "Database error: $message"
        is RestoreError.PartialRestore -> "Partial restore completed: $message (failed modules: ${failedModules.joinToString()})"
        is RestoreError.UnknownError -> "Unknown error: $message"
    }
}
