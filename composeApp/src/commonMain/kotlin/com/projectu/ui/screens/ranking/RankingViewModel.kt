package com.projectu.ui.screens.ranking

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.remote.model.RankingContent
import com.projectu.shared.data.remote.model.RankingContentModeConfig
import com.projectu.shared.data.remote.model.RankingMode
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.repository.ArtworkRepository
import com.projectu.shared.domain.repository.NovelRepository
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import com.projectu.ui.navigation.ArtworkListSource
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * 排行榜 ViewModel
 * MVI 架构模式
 */
class RankingViewModel(
    private val artworkRepository: ArtworkRepository,
    private val novelRepository: NovelRepository,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val syncNovelStatesUseCase: SyncNovelStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    // UI 状态
    private val _state = MutableStateFlow(RankingState())
    val state: StateFlow<RankingState> = _state.asStateFlow()
    
    init {
        // 监听全局状态变更事件
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents.collect { event ->
                when (event) {
                    is StateCacheEvent.ArtworkBookmarkChanged -> {
                        updateArtworkBookmarkStatus(event.artworkId, event.status, event.bookmarkId)
                    }
                    is StateCacheEvent.NovelBookmarkChanged -> {
                        updateNovelBookmarkStatus(event.novelId, event.status, event.bookmarkId)
                    }
                    else -> {}
                }
            }
        }
    }
    
    /**
     * 创建绑定到指定 mode 的 ArtworkListSource
     * 
     * 用于作品详情页的列表导航功能。当用户点击某个 mode 下的作品时，
     * 创建一个绑定该 mode 的列表源，使详情页可以响应式地获取列表更新。
     * 
     * @param modeKey 排行榜模式的 key (RankingMode.value)
     * @return 绑定到指定 mode 的 ArtworkListSource
     */
    fun createArtworkListSource(modeKey: String): ArtworkListSource {
        return object : ArtworkListSource {
            override val artworkIdsFlow: StateFlow<List<String>> = state.map { currentState ->
                currentState.modeDataCache[modeKey]?.artworks?.map { it.id } ?: emptyList()
            }.stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.modeDataCache[modeKey]?.artworks?.map { it.id } ?: emptyList()
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
        val hasData = _state.value.modeDataCache[currentMode.value] != null
        
        if (!hasData && !_state.value.isLoading) {
            loadRanking()
        }
    }
    
    /**
     * 切换内容类型（第一层选择器）
     */
    fun switchContentType(contentType: RankingContent) {
        if (_state.value.currentContentType == contentType) return
        
        val currentMode = _state.value.currentMode
        // 如果新内容类型支持当前mode，则保留；否则使用默认mode
        val newMode = if (RankingContentModeConfig.isCompatible(contentType, currentMode)) {
            currentMode
        } else {
            RankingContentModeConfig.getDefaultMode(contentType) ?: RankingMode.DAILY
        }
        
        _state.update {
            it.copy(
                currentContentType = contentType,
                currentMode = newMode,
                modeDataCache = emptyMap(), // 清空所有缓存
                isLoading = false,
                error = null
            )
        }
        // 加载新 mode 的数据
        loadRankingIfNeeded()
    }
    
    /**
     * 切换排行榜模式（第二层选择器）
     */
    fun switchMode(mode: RankingMode) {
        if (_state.value.currentMode == mode) return
        
        // 检查兼容性
        if (!RankingContentModeConfig.isCompatible(_state.value.currentContentType, mode)) {
            return
        }
        
        _state.update {
            it.copy(
                currentMode = mode,
                error = null
            )
        }
        // 只在该 mode 没有数据时才加载
        loadRankingIfNeeded()
    }
    
    /**
     * 切换日期
     * @param date 日期字符串（格式：yyyyMMdd），null表示最新
     */
    fun switchDate(date: String?) {
        if (_state.value.selectedDate == date) return
        
        _state.update {
            it.copy(
                selectedDate = date,
                modeDataCache = emptyMap(), // 清空所有缓存
                isLoading = false,
                error = null
            )
        }
        loadRankingIfNeeded()
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode.value]
        
        if (currentState.isLoading || modeData?.isLoadingMore == true || modeData?.hasMorePages == false) {
            return
        }
        
        _state.update {
            val updatedCache = it.modeDataCache.toMutableMap()
            val currentModeData = updatedCache[it.currentMode.value] ?: ModeData()
            updatedCache[it.currentMode.value] = currentModeData.copy(isLoadingMore = true)
            it.copy(modeDataCache = updatedCache)
        }
        
        loadRanking(append = true)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _state.update {
            val updatedCache = it.modeDataCache.toMutableMap()
            updatedCache.remove(it.currentMode.value) // 移除当前 mode 的缓存
            it.copy(
                modeDataCache = updatedCache,
                isLoading = false,
                error = null
            )
        }
        loadRankingIfNeeded()
    }
    
    /**
     * 只在当前 mode 没有数据时加载
     */
    private fun loadRankingIfNeeded() {
        val currentState = _state.value
        val modeData = currentState.modeDataCache[currentState.currentMode.value]
        
        // 如果已有数据，不加载
        if (modeData != null && (modeData.artworks.isNotEmpty() || modeData.novels.isNotEmpty())) {
            return
        }
        
        // 否则开始加载
        _state.update { it.copy(isLoading = true, error = null) }
        loadRanking()
    }
    /**
     * 加载排行榜数据
     */
    private fun loadRanking(append: Boolean = false) {
        screenModelScope.launch {
            val currentState = _state.value
            val modeKey = currentState.currentMode.value
            val currentModeData = currentState.modeDataCache[modeKey] ?: ModeData()
            val page = if (append) currentModeData.currentPage + 1 else 1
            
            // 根据内容类型判断是加载作品还是小说
            if (currentState.currentContentType == RankingContent.NOVEL) {
                // 加载小说排行榜
                novelRepository.getRankingWithDateInfo(
                    mode = currentState.currentMode,
                    content = currentState.currentContentType,
                    page = page,
                    date = currentState.selectedDate
                )
                    .onSuccess { (newNovels, dateInfo) ->
                        // 应用全局状态缓存
                        val syncedNovels = syncNovelStatesUseCase(newNovels)
                        
                        _state.update { state ->
                            val updatedCache = state.modeDataCache.toMutableMap()
                            val existingData = updatedCache[modeKey] ?: ModeData()
                            
                            val updatedNovels = if (append) {
                                existingData.novels + syncedNovels
                            } else {
                                syncedNovels
                            }
                            
                            updatedCache[modeKey] = ModeData(
                                novels = updatedNovels,
                                currentPage = page,
                                hasMorePages = newNovels.isNotEmpty(),
                                isLoadingMore = false
                            )
                            
                            state.copy(
                                modeDataCache = updatedCache,
                                currentDate = dateInfo.first,
                                prevDate = dateInfo.second,
                                nextDate = dateInfo.third,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update { state ->
                            val updatedCache = state.modeDataCache.toMutableMap()
                            val existingData = updatedCache[modeKey] ?: ModeData()
                            updatedCache[modeKey] = existingData.copy(isLoadingMore = false)
                            
                            state.copy(
                                modeDataCache = updatedCache,
                                error = error.message,
                                isLoading = false
                            )
                        }
                    }
            } else {
                // 加载作品排行榜（综合、插画、漫画、动图）
                artworkRepository.getRankingWithDateInfo(
                    mode = currentState.currentMode,
                    content = currentState.currentContentType,
                    page = page,
                    date = currentState.selectedDate
                )
                    .onSuccess { (newArtworks, dateInfo) ->
                        // 应用全局状态缓存
                        val syncedArtworks = syncArtworkStatesUseCase(newArtworks)
                        
                        _state.update { state ->
                            val updatedCache = state.modeDataCache.toMutableMap()
                            val existingData = updatedCache[modeKey] ?: ModeData()
                            
                            val updatedArtworks = if (append) {
                                existingData.artworks + syncedArtworks
                            } else {
                                syncedArtworks
                            }
                            
                            updatedCache[modeKey] = ModeData(
                                artworks = updatedArtworks,
                                currentPage = page,
                                hasMorePages = newArtworks.isNotEmpty(),
                                isLoadingMore = false
                            )
                            
                            state.copy(
                                modeDataCache = updatedCache,
                                currentDate = dateInfo.first,
                                prevDate = dateInfo.second,
                                nextDate = dateInfo.third,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update { state ->
                            val updatedCache = state.modeDataCache.toMutableMap()
                            val existingData = updatedCache[modeKey] ?: ModeData()
                            updatedCache[modeKey] = existingData.copy(isLoadingMore = false)
                            
                            state.copy(
                                modeDataCache = updatedCache,
                                error = error.message,
                                isLoading = false
                            )
                        }
                    }
            }
        }
    }
    
    /**
     * 更新作品收藏状态
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
    
    /**
     * 更新小说收藏状态
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
 * 排行榜状态
 */
data class RankingState(
    // 第一层：内容类型选择器
    val currentContentType: RankingContent = RankingContent.ALL,
    
    // 第二层：排行榜模式选择器
    val currentMode: RankingMode = RankingMode.DAILY,
    
    // 日期选择（格式：yyyyMMdd，null表示使用最新）
    val selectedDate: String? = null,
    
    // 日期导航信息（从API响应中获取）
    val currentDate: String? = null,  // 当前显示的日期
    val prevDate: String? = null,     // 前一天的日期
    val nextDate: String? = null,     // 后一天的日期
    
    // 每个 mode 的数据缓存（key = RankingMode.value）
    val modeDataCache: Map<String, ModeData> = emptyMap(),
    
    // 加载状态
    val isLoading: Boolean = false,
    val error: String? = null
)

/**
 * 单个 mode 的数据
 */
data class ModeData(
    // 作品列表（当内容类型为综合、插画、漫画、动图时使用）
    val artworks: List<Artwork> = emptyList(),
    
    // 小说列表（当内容类型为小说时使用）
    val novels: List<Novel> = emptyList(),
    
    // 分页状态
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    val isLoadingMore: Boolean = false
)

/**
 * 排行榜响应数据包装（包含作品/小说列表和日期导航信息）
 */
data class RankingResponseData(
    val artworks: List<Artwork> = emptyList(),
    val novels: List<Novel> = emptyList(),
    val currentDate: String? = null,
    val prevDate: String? = null,
    val nextDate: String? = null
)
