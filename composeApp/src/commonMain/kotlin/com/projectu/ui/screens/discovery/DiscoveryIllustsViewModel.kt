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
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现插画页面 ViewModel
 * MVI 架构模式，支持多模式数据缓存
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
     * 创建绑定到指定模式的 ArtworkListSource
     * 
     * @param mode 发现模式
     * @return 绑定到指定模式的 ArtworkListSource
     */
    fun createArtworkListSource(mode: DiscoveryMode): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.modeDataCache[mode]?.artworks?.map { it.id } ?: emptyList()
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.modeDataCache[mode]?.artworks?.map { it.id } ?: emptyList()
            )
            
            override fun loadMoreArtworks() {
                loadMore()
            }
        }
    }
    
    /**
     * 初始化加载（惰性加载）
     * 只在当前模式没有数据时才加载
     */
    fun initLoadIfNeeded() {
        val currentMode = _state.value.currentMode
        val modeData = _state.value.modeDataCache[currentMode]
        
        if (modeData == null && !_state.value.isLoading) {
            loadArtworks()
        }
    }
    
    /**
     * 切换模式
     * 不清空数据，保持各模式独立的缓存
     */
    fun switchMode(mode: DiscoveryMode) {
        if (_state.value.currentMode == mode) return
        
        _state.update {
            it.copy(
                currentMode = mode,
                error = null
            )
        }
        
        // 只在该模式没有数据时才加载
        loadArtworksIfNeeded()
    }
    
    /**
     * 只在当前模式没有数据时加载
     */
    private fun loadArtworksIfNeeded() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode]
        
        // 如果已有数据，不加载
        if (modeData != null && modeData.artworks.isNotEmpty()) {
            return
        }
        
        // 否则开始加载
        _state.update { it.copy(isLoading = true, error = null) }
        loadArtworks()
    }
    
    /**
     * 加载更多作品
     * 由于发现接口不支持分页，这里不实现加载更多
     */
    fun loadMore() {
        // 发现接口不支持分页，不加载更多
    }
    
    /**
     * 刷新当前模式的数据
     */
    fun refresh() {
        val currentMode = _state.value.currentMode
        
        _state.update {
            val updatedCache = it.modeDataCache.toMutableMap()
            updatedCache.remove(currentMode) // 移除当前模式的缓存
            it.copy(
                modeDataCache = updatedCache,
                isLoading = true,
                error = null
            )
        }
        loadArtworks()
    }
    
    /**
     * 加载作品
     */
    private fun loadArtworks() {
        screenModelScope.launch {
            val currentMode = _state.value.currentMode
            
            artworkRepository.getDiscoveryIllusts(
                mode = currentMode,
                limit = 100
            )
                .onSuccess { newArtworks ->
                    // 应用全局状态缓存
                    val syncedArtworks = syncArtworkStatesUseCase(newArtworks)
                    
                    _state.update { currentState ->
                        val updatedCache = currentState.modeDataCache.toMutableMap()
                        updatedCache[currentMode] = IllustsModeData(artworks = syncedArtworks)
                        
                        currentState.copy(
                            modeDataCache = updatedCache,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = error.message ?: "未知错误"
                        )
                    }
                }
        }
    }
    
    /**
     * 更新所有模式缓存中作品的收藏状态
     * 由全局状态变更事件触发
     */
    private fun updateArtworkBookmarkStatus(
        artworkId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        _state.update { currentState ->
            val updatedCache = currentState.modeDataCache.mapValues { (_, modeData) ->
                modeData.copy(
                    artworks = modeData.artworks.map { artwork ->
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
            currentState.copy(modeDataCache = updatedCache)
        }
    }
}

/**
 * 模式数据（每个模式独立的数据缓存）
 */
data class IllustsModeData(
    val artworks: List<Artwork> = emptyList()
)

/**
 * 发现插画页面状态
 */
data class DiscoveryIllustsState(
    val currentMode: DiscoveryMode = DiscoveryMode.ALL,
    val modeDataCache: Map<DiscoveryMode, IllustsModeData> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 获取当前模式的作品列表
     */
    val artworks: List<Artwork>
        get() = modeDataCache[currentMode]?.artworks ?: emptyList()
    
    /**
     * 当前模式是否正在加载更多（发现接口不支持分页，始终为false）
     */
    val isLoadingMore: Boolean = false
}
