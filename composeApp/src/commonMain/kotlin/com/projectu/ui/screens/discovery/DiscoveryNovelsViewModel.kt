package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.NovelRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现小说页面 ViewModel
 * MVI 架构模式
 */
class DiscoveryNovelsViewModel(
    private val novelRepository: NovelRepository
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryNovelsState())
    val state: StateFlow<DiscoveryNovelsState> = _state.asStateFlow()
    
    init {
        // 初始加载
        loadNovels()
    }
    
    /**
     * 切换模式
     */
    fun switchMode(mode: DiscoveryMode) {
        if (_state.value.currentMode == mode) return
        
        _state.update {
            it.copy(
                currentMode = mode,
                novels = emptyList(),
                isLoading = true,
                error = null
            )
        }
        loadNovels()
    }
    
    /**
     * 加载更多小说
     * 注意：发现接口不支持分页，每次调用返回新的推荐结果
     */
    fun loadMore() {
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        
        _state.update { it.copy(isLoadingMore = true) }
        loadNovels(append = true)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _state.update {
            it.copy(
                novels = emptyList(),
                isLoading = true,
                error = null
            )
        }
        loadNovels()
    }
    
    /**
     * 加载小说
     */
    private fun loadNovels(append: Boolean = false) {
        screenModelScope.launch {
            novelRepository.getDiscoveryNovels(
                mode = _state.value.currentMode,
                limit = 100
            )
                .onSuccess { newNovels ->
                    _state.update { currentState ->
                        // 如果是追加模式，需要去重
                        val updatedNovels = if (append) {
                            val existingIds = currentState.novels.map { it.id }.toSet()
                            val uniqueNewNovels = newNovels.filter { it.id !in existingIds }
                            currentState.novels + uniqueNewNovels
                        } else {
                            newNovels
                        }
                        
                        currentState.copy(
                            novels = updatedNovels,
                            isLoading = false,
                            isLoadingMore = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            isLoadingMore = false,
                            error = error.message ?: "未知错误"
                        )
                    }
                }
        }
    }
}

/**
 * 发现小说页面状态
 */
data class DiscoveryNovelsState(
    val novels: List<Novel> = emptyList(),
    val currentMode: DiscoveryMode = DiscoveryMode.ALL,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)


