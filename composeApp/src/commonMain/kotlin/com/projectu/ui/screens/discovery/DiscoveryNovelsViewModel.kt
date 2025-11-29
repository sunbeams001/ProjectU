package com.projectu.ui.screens.discovery

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.remote.model.DiscoveryMode
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 发现小说页面 ViewModel
 * MVI 架构模式，支持多模式数据缓存
 */
class DiscoveryNovelsViewModel(
    private val novelRepository: NovelRepository,
    private val syncNovelStatesUseCase: SyncNovelStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(DiscoveryNovelsState())
    val state: StateFlow<DiscoveryNovelsState> = _state.asStateFlow()
    
    init {
        // 监听全局状态变更事件
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.NovelBookmarkChanged -> {
                        updateNovelBookmarkStatus(event.novelId, event.status, event.bookmarkId)
                    }
                    else -> {}
                }
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
            loadNovels()
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
        loadNovelsIfNeeded()
    }
    
    /**
     * 只在当前模式没有数据时加载
     */
    private fun loadNovelsIfNeeded() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode]
        
        // 如果已有数据，不加载
        if (modeData != null && modeData.novels.isNotEmpty()) {
            return
        }
        
        // 否则开始加载
        _state.update { it.copy(isLoading = true, error = null) }
        loadNovels()
    }
    
    /**
     * 加载更多小说
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
        loadNovels()
    }
    
    /**
     * 加载小说
     */
    private fun loadNovels() {
        screenModelScope.launch {
            val currentMode = _state.value.currentMode
            
            novelRepository.getDiscoveryNovels(
                mode = currentMode,
                limit = 100
            )
                .onSuccess { newNovels ->
                    // 应用全局状态缓存
                    val syncedNovels = syncNovelStatesUseCase(newNovels)
                    
                    _state.update { currentState ->
                        val updatedCache = currentState.modeDataCache.toMutableMap()
                        updatedCache[currentMode] = NovelsModeData(novels = syncedNovels)
                        
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
     * 更新所有模式缓存中小说的收藏状态
     * 由全局状态变更事件触发
     */
    private fun updateNovelBookmarkStatus(
        novelId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        _state.update { currentState ->
            val updatedCache = currentState.modeDataCache.mapValues { (_, modeData) ->
                modeData.copy(
                    novels = modeData.novels.map { novel ->
                        if (novel.id == novelId) {
                            novel.copy(
                                bookmarkStatus = status,
                                bookmarkId = bookmarkId
                            )
                        } else {
                            novel
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
data class NovelsModeData(
    val novels: List<Novel> = emptyList()
)

/**
 * 发现小说页面状态
 */
data class DiscoveryNovelsState(
    val currentMode: DiscoveryMode = DiscoveryMode.ALL,
    val modeDataCache: Map<DiscoveryMode, NovelsModeData> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 获取当前模式的小说列表
     */
    val novels: List<Novel>
        get() = modeDataCache[currentMode]?.novels ?: emptyList()
    
    /**
     * 当前模式是否正在加载更多（发现接口不支持分页，始终为false）
     */
    val isLoadingMore: Boolean = false
}


