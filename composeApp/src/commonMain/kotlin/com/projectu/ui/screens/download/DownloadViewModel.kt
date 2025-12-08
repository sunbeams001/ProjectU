package com.projectu.ui.screens.download

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.projectu.shared.domain.model.DownloadStatus
import com.projectu.shared.domain.model.DownloadTask
import com.projectu.shared.domain.repository.DownloadRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 下载列表ViewModel
 */
class DownloadViewModel(
    private val downloadRepository: DownloadRepository
) : ViewModel() {
    
    private val _selectedStatus = MutableStateFlow<DownloadStatus?>(null)
    val selectedStatus: StateFlow<DownloadStatus?> = _selectedStatus.asStateFlow()
    
    /**
     * 下载任务列表（根据选中状态筛选）
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val downloadTasks: StateFlow<List<DownloadTask>> = _selectedStatus
        .flatMapLatest { status ->
            if (status == null) {
                downloadRepository.observeAllTasks()
            } else {
                downloadRepository.observeTasksByStatus(status)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    /**
     * 按状态筛选
     */
    fun filterByStatus(status: DownloadStatus?) {
        _selectedStatus.value = status
    }
    
    /**
     * 开始下载
     */
    fun startDownload(taskId: String) {
        viewModelScope.launch {
            downloadRepository.startDownload(taskId)
        }
    }
    
    /**
     * 暂停下载
     */
    fun pauseDownload(taskId: String) {
        viewModelScope.launch {
            downloadRepository.pauseDownload(taskId)
        }
    }
    
    /**
     * 删除下载任务
     */
    fun deleteDownload(taskId: String) {
        viewModelScope.launch {
            downloadRepository.deleteTask(taskId)
        }
    }
}
