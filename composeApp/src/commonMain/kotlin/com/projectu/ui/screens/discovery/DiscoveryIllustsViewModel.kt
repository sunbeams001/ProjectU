package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现插画页面 ViewModel
 * MVI 架构模式
 */
class DiscoveryIllustsViewModel(
    private val artworkRepository: ArtworkRepository,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryIllustsState())
    val state: StateFlow<DiscoveryIllustsState> = _state.asStateFlow()
    
    init {
        // 监听全局状态变更事件
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.ArtworkBookmarkChanged -> {
                        updateArtworkBookmarkStatus(event.artworkId, event.status, event.bookmarkId)
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * 初始化加载（惰性加载）
     */
    fun initLoadIfNeeded() {
        if (_state.value.artworks.isEmpty() && !_state.value.isLoading && _state.value.error == null) {
            loadArtworks()
        }
    }
    
    /**
     * 切换模式
     */
    fun switchMode(mode: DiscoveryMode) {
        if (_state.value.currentMode == mode) return
        
        _state.update {
            it.copy(
                currentMode = mode,
                artworks = emptyList(),
                isLoading = true,
                error = null
            )
        }
        loadArtworks()
    }
    
    /**
     * 加载更多作品
     */
    fun loadMore() {
        if (_state.value.isLoading || _state.value.isLoadingMore) return
        
        // 由于发现接口不支持分页，多次调用会返回相同数据
        // 这里简单地不再加载更多，避免重复数据
        // 如果需要更多数据，可以考虑其他策略（如切换到其他推荐接口）
        if (_state.value.artworks.isNotEmpty()) {
            // 已经有数据了，不再加载更多
            return
        }
        
        _state.update { it.copy(isLoadingMore = true) }
        loadArtworks(append = true)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _state.update {
            it.copy(
                artworks = emptyList(),
                isLoading = true,
                error = null
            )
        }
        loadArtworks()
    }
    
    /**
     * 加载作品
     */
    private fun loadArtworks(append: Boolean = false) {
        screenModelScope.launch {
            artworkRepository.getDiscoveryIllusts(
                mode = _state.value.currentMode,
                limit = 100
            )
                .onSuccess { newArtworks ->
                    // 应用全局状态缓存
                    val syncedArtworks = syncArtworkStatesUseCase(newArtworks)
                    
                    _state.update { currentState ->
                        // 如果是追加模式，需要去重
                        val updatedArtworks = if (append) {
                            val existingIds = currentState.artworks.map { it.id }.toSet()
                            val uniqueNewArtworks = syncedArtworks.filter { it.id !in existingIds }
                            currentState.artworks + uniqueNewArtworks
                        } else {
                            syncedArtworks
                        }
                        
                        currentState.copy(
                            artworks = updatedArtworks,
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
    
    /**
     * 更新列表中作品的收藏状态
     * 由全局状态变更事件触发
     */
    private fun updateArtworkBookmarkStatus(
        artworkId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        _state.update { currentState ->
            currentState.copy(
                artworks = currentState.artworks.map { artwork ->
                    if (artwork.id == artworkId) {
                        artwork.copy(
                            bookmarkStatus = status,
                            bookmarkId = bookmarkId
                        )
                    } else {
                        artwork
                    }
                }
            )
        }
    }
}

/**
 * 发现插画页面状态
 */
data class DiscoveryIllustsState(
    val artworks: List<Artwork> = emptyList(),
    val currentMode: DiscoveryMode = DiscoveryMode.ALL,
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
)
