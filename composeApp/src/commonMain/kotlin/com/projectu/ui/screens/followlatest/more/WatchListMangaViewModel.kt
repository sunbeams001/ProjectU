package com.projectu.ui.screens.followlatest.more

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.remote.dto.follow.WatchedIllustSeries
import com.projectu.shared.domain.repository.WatchListRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 漫画追更列表 ViewModel
 * 获取并管理已追更的漫画系列
 */
class WatchListMangaViewModel(
    private val watchListRepository: WatchListRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(WatchListMangaState())
    val state: StateFlow<WatchListMangaState> = _state.asStateFlow()
    
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
            
            watchListRepository.getWatchListManga(page)
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
 * 漫画追更列表状态
 */
data class WatchListMangaState(
    val series: List<WatchedIllustSeries> = emptyList(),
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
