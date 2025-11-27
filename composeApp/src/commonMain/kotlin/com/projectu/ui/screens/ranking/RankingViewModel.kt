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
        // 初始加载
        loadRanking()
        
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
                artworks = emptyList(),
                novels = emptyList(),
                currentPage = 1,
                hasMorePages = true,
                isLoading = true,
                error = null
            )
        }
        loadRanking()
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
                artworks = emptyList(),
                novels = emptyList(),
                currentPage = 1,
                hasMorePages = true,
                isLoading = true,
                error = null
            )
        }
        loadRanking()
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
                artworks = emptyList(),
                novels = emptyList(),
                currentPage = 1,
                hasMorePages = true,
                isLoading = true,
                error = null
            )
        }
        loadRanking()
    }
    
    /**
     * 加载更多
     */
    fun loadMore() {
        if (_state.value.isLoading || _state.value.isLoadingMore || !_state.value.hasMorePages) return
        
        _state.update { it.copy(isLoadingMore = true) }
        loadRanking(append = true)
    }
    
    /**
     * 刷新数据
     */
    fun refresh() {
        _state.update {
            it.copy(
                artworks = emptyList(),
                novels = emptyList(),
                currentPage = 1,
                hasMorePages = true,
                isLoading = true,
                error = null
            )
        }
        loadRanking()
    }
    
    /**
     * 加载排行榜数据
     */
    private fun loadRanking(append: Boolean = false) {
        screenModelScope.launch {
            val currentState = _state.value
            val page = if (append) currentState.currentPage + 1 else 1
            
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
                            val updatedNovels = if (append) {
                                state.novels + syncedNovels
                            } else {
                                syncedNovels
                            }
                            
                            state.copy(
                                novels = updatedNovels,
                                currentPage = page,
                                hasMorePages = newNovels.isNotEmpty(), // 如果返回数据为空，说明没有更多了
                                currentDate = dateInfo.first,
                                prevDate = dateInfo.second,
                                nextDate = dateInfo.third,
                                isLoading = false,
                                isLoadingMore = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                error = error.message,
                                isLoading = false,
                                isLoadingMore = false
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
                            val updatedArtworks = if (append) {
                                state.artworks + syncedArtworks
                            } else {
                                syncedArtworks
                            }
                            
                            state.copy(
                                artworks = updatedArtworks,
                                currentPage = page,
                                hasMorePages = newArtworks.isNotEmpty(),
                                currentDate = dateInfo.first,
                                prevDate = dateInfo.second,
                                nextDate = dateInfo.third,
                                isLoading = false,
                                isLoadingMore = false,
                                error = null
                            )
                        }
                    }
                    .onFailure { error ->
                        _state.update {
                            it.copy(
                                error = error.message,
                                isLoading = false,
                                isLoadingMore = false
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
    
    /**
     * 更新小说收藏状态
     */
    private fun updateNovelBookmarkStatus(
        novelId: String,
        status: BookmarkStatus,
        bookmarkId: String?
    ) {
        _state.update { currentState ->
            currentState.copy(
                novels = currentState.novels.map { novel ->
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
    
    // 作品列表（当内容类型为综合、插画、漫画、动图时使用）
    val artworks: List<Artwork> = emptyList(),
    
    // 小说列表（当内容类型为小说时使用）
    val novels: List<Novel> = emptyList(),
    
    // 分页状态
    val currentPage: Int = 1,
    val hasMorePages: Boolean = true,
    
    // 加载状态
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null
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
