package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.repository.ArtworkRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现插画页面 ViewModel
 * MVI 架构模式
 */
class DiscoveryIllustsViewModel(
    private val artworkRepository: ArtworkRepository
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryIllustsState())
    val state: StateFlow<DiscoveryIllustsState> = _state.asStateFlow()
    
    init {
        // 初始加载
        loadArtworks()
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
                    _state.update { currentState ->
                        // 如果是追加模式，需要去重
                        val updatedArtworks = if (append) {
                            val existingIds = currentState.artworks.map { it.id }.toSet()
                            val uniqueNewArtworks = newArtworks.filter { it.id !in existingIds }
                            currentState.artworks + uniqueNewArtworks
                        } else {
                            newArtworks
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
