package com.projectu.ui.screens.novelseries

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.domain.repository.NovelSeriesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 小说系列详情页 ViewModel
 */
class NovelSeriesViewModel(
    private val novelSeriesRepository: NovelSeriesRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(NovelSeriesDetailState())
    val state: StateFlow<NovelSeriesDetailState> = _state.asStateFlow()
    
    // 当前系列 ID
    private var currentSeriesId: String = ""
    
    // 每页加载数量
    private val pageSize = 30
    
    /**
     * 加载系列详情和内容
     */
    fun loadSeries(seriesId: String) {
        if (seriesId == currentSeriesId && _state.value.series != null) {
            return // 已加载，不重复加载
        }
        
        currentSeriesId = seriesId
        val seriesIdLong = seriesId.toLongOrNull() ?: return
        
        // 重置状态
        _state.update { 
            NovelSeriesDetailState(
                isLoadingSeries = true,
                isLoadingContents = true
            ) 
        }
        
        // 并行加载系列详情和内容
        screenModelScope.launch {
            // 1. 加载系列详情
            val seriesResult = novelSeriesRepository.getSeriesDetail(seriesIdLong)
            seriesResult.fold(
                onSuccess = { series ->
                    _state.update { 
                        it.copy(
                            series = series,
                            isLoadingSeries = false,
                            seriesError = null
                        ) 
                    }
                },
                onFailure = { e ->
                    _state.update { 
                        it.copy(
                            isLoadingSeries = false,
                            seriesError = e.message ?: "Failed to load series detail"
                        ) 
                    }
                }
            )
        }
        
        screenModelScope.launch {
            // 2. 加载系列内容（第一页）
            loadContents(isInitial = true)
        }
    }
    
    /**
     * 加载系列内容（分页）
     */
    private suspend fun loadContents(isInitial: Boolean = false) {
        if (!isInitial && (!_state.value.hasMore || _state.value.isLoadingContents)) {
            return
        }
        
        _state.update { it.copy(isLoadingContents = true, contentsError = null) }
        
        val lastOrder = if (isInitial) null else _state.value.lastOrder
        val seriesIdLong = currentSeriesId.toLongOrNull() ?: return
        
        val result = novelSeriesRepository.getSeriesContents(
            seriesId = seriesIdLong,
            limit = pageSize,
            lastOrder = lastOrder,
            orderBy = "asc"
        )
        
        result.fold(
            onSuccess = { newNovels ->
                _state.update { currentState ->
                    val allNovels = if (isInitial) {
                        newNovels
                    } else {
                        currentState.novels + newNovels
                    }
                    
                    // 计算 lastOrder（下一页的起始位置）
                    val newLastOrder = newNovels.lastOrNull()?.seriesOrder ?: currentState.lastOrder
                    
                    currentState.copy(
                        novels = allNovels,
                        isLoadingContents = false,
                        hasMore = newNovels.size >= pageSize,
                        lastOrder = newLastOrder,
                        contentsError = null
                    )
                }
            },
            onFailure = { e ->
                _state.update { 
                    it.copy(
                        isLoadingContents = false,
                        contentsError = e.message ?: "Failed to load series contents"
                    ) 
                }
            }
        )
    }
    
    /**
     * 加载更多内容
     */
    fun loadMore() {
        screenModelScope.launch {
            loadContents(isInitial = false)
        }
    }
    
    /**
     * 切换追更状态
     */
    fun toggleWatch() {
        val series = _state.value.series ?: return
        
        _state.update { it.copy(isWatchLoading = true, watchError = null) }
        
        val seriesIdLong = series.id.toLongOrNull() ?: return
        screenModelScope.launch {
            val result = if (series.isWatched) {
                novelSeriesRepository.unwatchSeries(seriesIdLong)
            } else {
                novelSeriesRepository.watchSeries(seriesIdLong)
            }
            
            result.fold(
                onSuccess = {
                    _state.update { currentState ->
                        currentState.copy(
                            series = currentState.series?.copy(isWatched = !series.isWatched),
                            isWatchLoading = false,
                            watchError = null
                        )
                    }
                },
                onFailure = { e ->
                    _state.update { 
                        it.copy(
                            isWatchLoading = false,
                            watchError = e.message ?: "Operation failed"
                        ) 
                    }
                }
            )
        }
    }
    
    /**
     * 刷新页面
     */
    fun refresh() {
        if (currentSeriesId.isNotEmpty()) {
            // 重置状态并重新加载
            _state.update { 
                NovelSeriesDetailState(
                    isLoadingSeries = true,
                    isLoadingContents = true
                ) 
            }
            loadSeries(currentSeriesId)
        }
    }
    
    /**
     * 重试加载系列详情
     */
    fun retrySeries() {
        if (currentSeriesId.isNotEmpty()) {
            _state.update { it.copy(isLoadingSeries = true, seriesError = null) }
            
            val seriesIdLong = currentSeriesId.toLongOrNull() ?: return
            screenModelScope.launch {
                val result = novelSeriesRepository.getSeriesDetail(seriesIdLong)
                result.fold(
                    onSuccess = { series ->
                        _state.update { 
                            it.copy(
                                series = series,
                                isLoadingSeries = false,
                                seriesError = null
                            ) 
                        }
                    },
                    onFailure = { e ->
                        _state.update { 
                            it.copy(
                                isLoadingSeries = false,
                                seriesError = e.message ?: "Failed to load series detail"
                            ) 
                        }
                    }
                )
            }
        }
    }
    
    /**
     * 重试加载系列内容
     */
    fun retryContents() {
        screenModelScope.launch {
            loadContents(isInitial = _state.value.novels.isEmpty())
        }
    }
}
