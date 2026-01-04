package com.projectu.ui.screens.followlatest

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.remote.dto.follow.WatchedNovelSeries
import com.projectu.shared.domain.repository.WatchListRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 小说追更列表 ViewModel
 * 获取并管理已追更的小说系列
 */
class WatchListNovelsViewModel(
    private val watchListRepository: WatchListRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(WatchListNovelsState())
    val state: StateFlow<WatchListNovelsState> = _state.asStateFlow()
    
    /**
     * 初始化加载
     */
    fun initLoadIfNeeded() {
        val currentState = _state.value
        if (currentState.series.isEmpty() && !currentState.isLoading) {
            loadSeries()
        }
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoading || currentState.isLoadingMore || currentState.isLastPage) {
            return
        }
        loadSeries(page = currentState.currentPage + 1, isLoadingMore = true)
    }
    
    /**
     * 刷新
     */
    fun refresh() {
        _state.update {
            it.copy(
                series = emptyList(),
                currentPage = 0,
                isLastPage = false,
                isLoading = true,
                error = null
            )
        }
        loadSeries()
    }
    
    private fun loadSeries(page: Int = 1, isLoadingMore: Boolean = false) {
        screenModelScope.launch {
            if (isLoadingMore) {
                _state.update { it.copy(isLoadingMore = true) }
            } else if (!_state.value.isLoading) {
                _state.update { it.copy(isLoading = true, error = null) }
            }
            
            watchListRepository.getWatchListNovels(page)
                .onSuccess { (newSeries, isLastPage) ->
                    _state.update { currentState ->
                        val mergedSeries = if (isLoadingMore) {
                            currentState.series + newSeries
                        } else {
                            newSeries
                        }
                        
                        currentState.copy(
                            series = mergedSeries,
                            currentPage = page,
                            isLastPage = isLastPage,
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
                            error = error.message ?: "Unknown error"
                        )
                    }
                }
        }
    }
}

/**
 * 小说追更列表状态
 */
data class WatchListNovelsState(
    val series: List<WatchedNovelSeries> = emptyList(),
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
