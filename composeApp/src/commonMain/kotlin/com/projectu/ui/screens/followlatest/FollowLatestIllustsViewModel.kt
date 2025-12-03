package com.projectu.ui.screens.followlatest

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 关注用户最新插画 ViewModel
 * MVI 架构模式，支持多模式数据缓存和分页加载
 */
class FollowLatestIllustsViewModel(
    private val artworkRepository: ArtworkRepository,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(FollowLatestIllustsState())
    val state: StateFlow<FollowLatestIllustsState> = _state.asStateFlow()
    
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
     */
    fun createArtworkListSource(mode: FollowLatestMode): ArtworkListSource {
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
     */
    fun switchMode(mode: FollowLatestMode) {
        if (_state.value.currentMode == mode) return
        
        _state.update {
            it.copy(
                currentMode = mode,
                error = null
            )
        }
        
        loadArtworksIfNeeded()
    }
    
    /**
     * 只在当前模式没有数据时加载
     */
    private fun loadArtworksIfNeeded() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode]
        
        if (modeData != null && modeData.artworks.isNotEmpty()) {
            return
        }
        
        _state.update { it.copy(isLoading = true, error = null) }
        loadArtworks()
    }
    
    /**
     * 加载更多作品
     */
    fun loadMore() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode]
        
        // 避免重复加载
        if (currentState.isLoading || modeData?.isLoadingMore == true || modeData?.isLastPage == true) {
            return
        }
        
        val nextPage = (modeData?.currentPage ?: 0) + 1
        loadArtworks(page = nextPage, isLoadingMore = true)
    }
    
    /**
     * 刷新当前模式的数据
     */
    fun refresh() {
        val currentMode = _state.value.currentMode
        
        _state.update {
            val updatedCache = it.modeDataCache.toMutableMap()
            updatedCache.remove(currentMode)
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
    private fun loadArtworks(page: Int = 1, isLoadingMore: Boolean = false) {
        screenModelScope.launch {
            val currentMode = _state.value.currentMode
            
            if (isLoadingMore) {
                _state.update { currentState ->
                    val updatedCache = currentState.modeDataCache.toMutableMap()
                    val modeData = updatedCache[currentMode] ?: FollowLatestIllustsModeData()
                    updatedCache[currentMode] = modeData.copy(isLoadingMore = true)
                    currentState.copy(modeDataCache = updatedCache)
                }
            }
            
            artworkRepository.getFollowLatestIllusts(
                mode = currentMode.value,
                page = page
            )
                .onSuccess { result ->
                    val newArtworks = result.first
                    val isLastPage = result.second
                    
                    // 应用全局状态缓存
                    val syncedArtworks = syncArtworkStatesUseCase(newArtworks)
                    
                    _state.update { currentState ->
                        val updatedCache = currentState.modeDataCache.toMutableMap()
                        val existingData = updatedCache[currentMode] ?: FollowLatestIllustsModeData()
                        
                        val mergedArtworks = if (isLoadingMore) {
                            existingData.artworks + syncedArtworks
                        } else {
                            syncedArtworks
                        }
                        
                        updatedCache[currentMode] = FollowLatestIllustsModeData(
                            artworks = mergedArtworks,
                            currentPage = page,
                            isLastPage = isLastPage,
                            isLoadingMore = false
                        )
                        
                        currentState.copy(
                            modeDataCache = updatedCache,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                .onFailure { error ->
                    _state.update { currentState ->
                        if (isLoadingMore) {
                            val updatedCache = currentState.modeDataCache.toMutableMap()
                            val modeData = updatedCache[currentMode] ?: FollowLatestIllustsModeData()
                            updatedCache[currentMode] = modeData.copy(isLoadingMore = false)
                            currentState.copy(modeDataCache = updatedCache)
                        } else {
                            currentState.copy(
                                isLoading = false,
                                error = error.message ?: "Unknown error"
                            )
                        }
                    }
                }
        }
    }
    
    /**
     * 更新所有模式缓存中作品的收藏状态
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
 * 模式数据
 */
data class FollowLatestIllustsModeData(
    val artworks: List<Artwork> = emptyList(),
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false
)

/**
 * 关注用户最新插画页面状态
 */
data class FollowLatestIllustsState(
    val currentMode: FollowLatestMode = FollowLatestMode.ALL,
    val modeDataCache: Map<FollowLatestMode, FollowLatestIllustsModeData> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 获取当前模式的作品列表
     */
    val artworks: List<Artwork>
        get() = modeDataCache[currentMode]?.artworks ?: emptyList()
    
    /**
     * 是否正在加载更多
     */
    val isLoadingMore: Boolean
        get() = modeDataCache[currentMode]?.isLoadingMore ?: false
}
