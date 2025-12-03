package com.projectu.ui.screens.followlatest

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 关注用户最新小说 ViewModel
 * MVI 架构模式，支持多模式数据缓存和分页加载
 */
class FollowLatestNovelsViewModel(
    private val novelRepository: NovelRepository,
    private val syncNovelStatesUseCase: SyncNovelStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(FollowLatestNovelsState())
    val state: StateFlow<FollowLatestNovelsState> = _state.asStateFlow()
    
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
     */
    fun switchMode(mode: FollowLatestMode) {
        if (_state.value.currentMode == mode) return
        
        _state.update {
            it.copy(
                currentMode = mode,
                error = null
            )
        }
        
        loadNovelsIfNeeded()
    }
    
    /**
     * 只在当前模式没有数据时加载
     */
    private fun loadNovelsIfNeeded() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode]
        
        if (modeData != null && modeData.novels.isNotEmpty()) {
            return
        }
        
        _state.update { it.copy(isLoading = true, error = null) }
        loadNovels()
    }
    
    /**
     * 加载更多小说
     */
    fun loadMore() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode]
        
        // 避免重复加载
        if (currentState.isLoading || modeData?.isLoadingMore == true || modeData?.isLastPage == true) {
            return
        }
        
        val nextPage = (modeData?.currentPage ?: 0) + 1
        loadNovels(page = nextPage, isLoadingMore = true)
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
        loadNovels()
    }
    
    /**
     * 加载小说
     */
    private fun loadNovels(page: Int = 1, isLoadingMore: Boolean = false) {
        screenModelScope.launch {
            val currentMode = _state.value.currentMode
            
            if (isLoadingMore) {
                _state.update { currentState ->
                    val updatedCache = currentState.modeDataCache.toMutableMap()
                    val modeData = updatedCache[currentMode] ?: FollowLatestNovelsModeData()
                    updatedCache[currentMode] = modeData.copy(isLoadingMore = true)
                    currentState.copy(modeDataCache = updatedCache)
                }
            }
            
            novelRepository.getFollowLatestNovels(
                mode = currentMode.value,
                page = page
            )
                .onSuccess { result ->
                    val newNovels = result.first
                    val isLastPage = result.second
                    
                    // 应用全局状态缓存
                    val syncedNovels = syncNovelStatesUseCase(newNovels)
                    
                    _state.update { currentState ->
                        val updatedCache = currentState.modeDataCache.toMutableMap()
                        val existingData = updatedCache[currentMode] ?: FollowLatestNovelsModeData()
                        
                        val mergedNovels = if (isLoadingMore) {
                            existingData.novels + syncedNovels
                        } else {
                            syncedNovels
                        }
                        
                        updatedCache[currentMode] = FollowLatestNovelsModeData(
                            novels = mergedNovels,
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
                            val modeData = updatedCache[currentMode] ?: FollowLatestNovelsModeData()
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
     * 更新所有模式缓存中小说的收藏状态
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
 * 模式数据
 */
data class FollowLatestNovelsModeData(
    val novels: List<Novel> = emptyList(),
    val currentPage: Int = 0,
    val isLastPage: Boolean = false,
    val isLoadingMore: Boolean = false
)

/**
 * 关注用户最新小说页面状态
 */
data class FollowLatestNovelsState(
    val currentMode: FollowLatestMode = FollowLatestMode.ALL,
    val modeDataCache: Map<FollowLatestMode, FollowLatestNovelsModeData> = emptyMap(),
    val isLoading: Boolean = false,
    val error: String? = null
) {
    /**
     * 获取当前模式的小说列表
     */
    val novels: List<Novel>
        get() = modeDataCache[currentMode]?.novels ?: emptyList()
    
    /**
     * 是否正在加载更多
     */
    val isLoadingMore: Boolean
        get() = modeDataCache[currentMode]?.isLoadingMore ?: false
}
