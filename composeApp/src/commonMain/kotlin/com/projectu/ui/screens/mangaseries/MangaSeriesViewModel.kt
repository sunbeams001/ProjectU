package com.projectu.ui.screens.mangaseries

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.domain.repository.MangaSeriesRepository
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 漫画系列详情页 ViewModel
 */
class MangaSeriesViewModel(
    private val mangaSeriesRepository: MangaSeriesRepository
) : ScreenModel {
    
    private val _state = MutableStateFlow(MangaSeriesDetailState())
    val state: StateFlow<MangaSeriesDetailState> = _state.asStateFlow()
    
    // 当前系列 ID
    private var currentSeriesId: String = ""
    
    /**
     * 创建 ArtworkListSource，用于作品详情页的列表导航功能
     * 
     * 当用户点击系列中的作品时，创建一个列表源，
     * 使详情页可以响应式地获取列表更新并支持左右滑动。
     * 
     * @return 绑定到当前系列的 ArtworkListSource
     */
    fun createArtworkListSource(): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.artworks.map { it.id }
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.artworks.map { it.id }
            )
            
            override fun loadMoreArtworks() {
                loadMore()
            }
        }
    }
    
    /**
     * 加载系列详情和内容
     */
    fun loadSeries(seriesId: String) {
        if (seriesId == currentSeriesId && _state.value.series != null) {
            return // 已加载，不重复加载
        }
        
        currentSeriesId = seriesId
        
        // 重置状态
        _state.update { 
            MangaSeriesDetailState(
                isLoadingSeries = true,
                isLoadingContents = true
            ) 
        }
        
        // 加载系列详情和作品（第一页）
        screenModelScope.launch {
            loadSeriesAndWorks(page = 1, isInitial = true)
        }
    }
    
    /**
     * 加载系列详情和作品
     */
    private suspend fun loadSeriesAndWorks(page: Int, isInitial: Boolean) {
        val seriesIdLong = currentSeriesId.toLongOrNull() ?: return
        // 加载系列详情
        val seriesResult = mangaSeriesRepository.getSeriesDetail(seriesIdLong, page)
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
        
        // 加载作品列表
        val worksResult = mangaSeriesRepository.getSeriesWorks(seriesIdLong, page)
        worksResult.fold(
            onSuccess = { result ->
                _state.update { currentState ->
                    val allArtworks = if (isInitial) {
                        result.artworks
                    } else {
                        // 合并时去重，避免重复 key 导致崩溃
                        val existingIds = currentState.artworks.map { it.id }.toSet()
                        val uniqueNewArtworks = result.artworks.filter { it.id !in existingIds }
                        currentState.artworks + uniqueNewArtworks
                    }
                    
                    // 根据已加载的作品数量和总数判断是否还有更多
                    val hasMore = allArtworks.size < result.total
                    
                    currentState.copy(
                        artworks = allArtworks,
                        isLoadingContents = false,
                        hasMore = hasMore,
                        currentPage = page,
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
        if (_state.value.isLoadingContents || !_state.value.hasMore) {
            return
        }
        
        _state.update { it.copy(isLoadingContents = true) }
        
        screenModelScope.launch {
            val nextPage = _state.value.currentPage + 1
            loadSeriesAndWorks(page = nextPage, isInitial = false)
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
                mangaSeriesRepository.unwatchSeries(seriesIdLong)
            } else {
                mangaSeriesRepository.watchSeries(seriesIdLong)
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
                MangaSeriesDetailState(
                    isLoadingSeries = true,
                    isLoadingContents = true
                ) 
            }
            
            screenModelScope.launch {
                loadSeriesAndWorks(page = 1, isInitial = true)
            }
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
                val result = mangaSeriesRepository.getSeriesDetail(seriesIdLong, 1)
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
     * 重试加载内容
     */
    fun retryContents() {
        if (currentSeriesId.isNotEmpty()) {
            _state.update { it.copy(isLoadingContents = true, contentsError = null) }
            
            val seriesIdLong = currentSeriesId.toLongOrNull() ?: return
            screenModelScope.launch {
                val apiResult = mangaSeriesRepository.getSeriesWorks(seriesIdLong, 1)
                apiResult.fold(
                    onSuccess = { result ->
                        _state.update { 
                            it.copy(
                                artworks = result.artworks,
                                isLoadingContents = false,
                                hasMore = result.artworks.size < result.total,
                                currentPage = 1,
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
        }
    }
}
