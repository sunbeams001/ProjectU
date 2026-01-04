package com.projectu.ui.screens.artwork

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.projectu.shared.data.cache.StateCacheEvent
import com.projectu.shared.data.cache.StateCacheManager
import com.projectu.shared.data.local.SettingsCache
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toArtworkList
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.BookmarkStatus
import com.projectu.shared.domain.usecase.SyncArtworkStatesUseCase
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.navigation.ArtworkListSource
import com.projectu.ui.util.TagClickHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.artwork_recommend_title
import projectu.composeapp.generated.resources.artwork_recommend_empty
import projectu.composeapp.generated.resources.common_loading
import projectu.composeapp.generated.resources.common_retry
import projectu.composeapp.generated.resources.nav_back

/**
 * 推荐作品页面
 * 
 * 功能：
 * - 展示基于某个作品的推荐作品
 * - 使用 getRecommendInit 获取初始数据
 * - 使用 getRecommendIllusts 进行分页加载
 * - 支持下拉刷新
 * - 瀑布流展示（使用配置的列数）
 * 
 * @param artworkId 基准作品ID
 */
data class ArtworkRecommendScreen(
    private val artworkId: String
) : Screen {
    
    /**
     * 自定义 Screen key，确保不同作品的推荐页面有独立的 ViewModel 实例
     * 每个基准作品ID对应一个独立的推荐列表状态
     */
    override val key: cafe.adriel.voyager.core.screen.ScreenKey
        get() = "ArtworkRecommendScreen_$artworkId"
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // 获取依赖
        val pixivApi: PixivApi = koinInject()
        val tagTranslationUtil: TagTranslationUtil = koinInject()
        val ageLimitDeterminer: AgeLimitDeterminer = koinInject()
        val syncArtworkStatesUseCase: SyncArtworkStatesUseCase = koinInject()
        val stateCacheManager: StateCacheManager = koinInject()
        
        val viewModel = rememberScreenModel {
            ArtworkRecommendViewModel(
                artworkId = artworkId,
                pixivApi = pixivApi,
                tagTranslationUtil = tagTranslationUtil,
                ageLimitDeterminer = ageLimitDeterminer,
                syncArtworkStatesUseCase = syncArtworkStatesUseCase,
                stateCacheManager = stateCacheManager
            )
        }
        val state by viewModel.state.collectAsState()
        val scrollToIndex by viewModel.scrollToIndex.collectAsState()
        
        // 创建Tag点击处理器
        val scope = rememberCoroutineScope()
        val searchHistoryStore: com.projectu.shared.data.local.SearchHistoryStore = koinInject()
        val tagClickHandler = remember(navigator) {
            TagClickHandler(navigator, searchHistoryStore, scope)
        }
        
        ArtworkRecommendContent(
            state = state,
            scrollToIndex = scrollToIndex,
            onScrollCompleted = { viewModel.resetScrollToIndex() },
            onBackClick = { navigator.pop() },
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
            onArtworkClick = { artwork, index ->
                // 创建列表源以支持详情页左右翻页
                val listSource = viewModel.createArtworkListSource()
                val currentArtworkIds = state.artworks.map { it.id }
                
                // 创建导航上下文，保存返回时的滚动位置
                val contextKey = com.projectu.ui.navigation.NavigationContextManager.createContext(
                    listSource = listSource,
                    onReturnWithIndex = { lastIndex ->
                        viewModel.setScrollToIndex(lastIndex)
                    }
                )
                
                navigator.push(
                    ArtworkDetailScreen(
                        artworkIds = currentArtworkIds,
                        initialIndex = index,
                        contextKey = contextKey
                    )
                )
            },
            onUserClick = { userId ->
                navigator.push(com.projectu.ui.screens.user.UserScreen(userId))
            },
            onTagClick = { tag -> tagClickHandler.handleTagClick(tag) }
        )
    }
}

/**
 * 推荐作品状态
 */
data class ArtworkRecommendState(
    val artworks: List<Artwork> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val isEmpty: Boolean = false // 明确标识推荐为空的情况
)

/**
 * 推荐作品 ViewModel
 */
class ArtworkRecommendViewModel(
    private val artworkId: String,
    private val pixivApi: PixivApi,
    private val tagTranslationUtil: TagTranslationUtil,
    private val ageLimitDeterminer: AgeLimitDeterminer,
    private val syncArtworkStatesUseCase: SyncArtworkStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(ArtworkRecommendState())
    val state: StateFlow<ArtworkRecommendState> = _state.asStateFlow()
        // 用于保存从详情页返回时要滚动到的位置
    private val _scrollToIndex = MutableStateFlow(-1)
    val scrollToIndex: StateFlow<Int> = _scrollToIndex.asStateFlow()
        // nextIds 数据源：从getRecommendInit获取
    private val nextIds = mutableListOf<Long>()
    // 当前加载位置
    private var currentIdIndex = 0
    // 每页加载数量
    private val pageSize = 18
    
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
        
        // 初始化加载
        loadInitialRecommendations()
    }
    
    /**
     * 创建 ArtworkListSource 以支持详情页列表导航
     * 
     * 当用户从推荐页面点击作品进入详情页时，创建此列表源，
     * 使详情页可以响应式地获取最新的推荐列表，并支持左右翻页。
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
     * 设置滚动位置（从详情页返回时调用）
     */
    fun setScrollToIndex(index: Int) {
        _scrollToIndex.value = index
    }
    
    /**
     * 重置滚动位置（滚动完成后调用）
     */
    fun resetScrollToIndex() {
        _scrollToIndex.value = -1
    }
    
    /**
     * 加载初始推荐数据
     */
    private fun loadInitialRecommendations() {
        screenModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            
            try {
                // 调用 getRecommendInit
                val response = pixivApi.illustApi.getRecommendInit(
                    pid = artworkId.toLong(),
                    limit = pageSize
                )
                
                if (response.error) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = response.message,
                            isEmpty = true
                        )
                    }
                    return@launch
                }
                
                val body = response.body
                if (body == null) {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "响应数据为空",
                            isEmpty = true
                        )
                    }
                    return@launch
                }
                
                // 转换作品列表
                val artworks = body.toArtworkList(
                    tagTranslationUtil = tagTranslationUtil,
                    ageLimitDeterminer = ageLimitDeterminer
                )
                
                // 同步作品状态
                val syncedArtworks = syncArtworkStatesUseCase(artworks)
                
                // 保存 nextIds 用于后续分页
                nextIds.clear()
                nextIds.addAll(body.nextIds)
                currentIdIndex = 0
                
                // 判断是否为空
                val isEmpty = artworks.isEmpty() && body.nextIds.isEmpty()
                
                _state.update {
                    it.copy(
                        artworks = syncedArtworks,
                        isLoading = false,
                        error = null,
                        hasMore = body.nextIds.isNotEmpty(),
                        isEmpty = isEmpty
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "加载失败",
                        isEmpty = true
                    )
                }
            }
        }
    }
    
    /**
     * 加载更多推荐作品
     */
    fun loadMore() {
        val currentState = _state.value
        if (currentState.isLoadingMore || !currentState.hasMore) {
            return
        }
        
        // 检查是否还有可用的ID
        if (currentIdIndex >= nextIds.size) {
            _state.update { it.copy(hasMore = false) }
            return
        }
        
        screenModelScope.launch {
            _state.update { it.copy(isLoadingMore = true) }
            
            try {
                // 从 nextIds 中取出一页的ID
                val endIndex = minOf(currentIdIndex + pageSize, nextIds.size)
                val idsToLoad = nextIds.subList(currentIdIndex, endIndex)
                
                // 调用 getRecommendIllusts
                val response = pixivApi.illustApi.getRecommendIllusts(illustIds = idsToLoad)
                
                if (response.error) {
                    _state.update { it.copy(isLoadingMore = false, error = response.message) }
                    return@launch
                }
                
                val body = response.body
                if (body == null) {
                    _state.update { it.copy(isLoadingMore = false) }
                    return@launch
                }
                
                // 转换作品列表
                val newArtworks = body.toArtworkList(
                    tagTranslationUtil = tagTranslationUtil,
                    ageLimitDeterminer = ageLimitDeterminer
                )
                
                // 同步作品状态
                val syncedNewArtworks = syncArtworkStatesUseCase(newArtworks)
                
                // 更新索引
                currentIdIndex = endIndex
                
                // 判断是否还有更多
                val hasMore = currentIdIndex < nextIds.size
                
                _state.update {
                    it.copy(
                        artworks = it.artworks + syncedNewArtworks,
                        isLoadingMore = false,
                        hasMore = hasMore
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "加载失败"
                    )
                }
            }
        }
    }
    
    /**
     * 刷新推荐列表
     */
    fun refresh() {
        // 重置状态
        nextIds.clear()
        currentIdIndex = 0
        _state.update {
            ArtworkRecommendState()
        }
        // 重新加载
        loadInitialRecommendations()
    }
    
    /**
     * 更新作品收藏状态
     */
    private fun updateArtworkBookmarkStatus(artworkId: String, status: BookmarkStatus, bookmarkId: String?) {
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
 * 推荐作品页面内容
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArtworkRecommendContent(
    state: ArtworkRecommendState,
    scrollToIndex: Int = -1,
    onScrollCompleted: () -> Unit = {},
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.artwork_recommend_title)) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(Res.string.nav_back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                // 初次加载中
                state.isLoading && state.artworks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator()
                        Text(stringResource(Res.string.common_loading))
                    }
                }
                
                // 加载错误且无数据
                state.error != null && state.artworks.isEmpty() -> {
                    ErrorDisplay(
                        message = state.error,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center),
                        isFullScreen = true
                    )
                }
                
                // 推荐为空
                state.isEmpty && state.artworks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = stringResource(Res.string.artwork_recommend_empty),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Button(onClick = onRefresh) {
                            Text(stringResource(Res.string.common_retry))
                        }
                    }
                }
                
                // 展示作品列表
                state.artworks.isNotEmpty() -> {
                    ArtworkRecommendGrid(
                        artworks = state.artworks,
                        scrollToIndex = scrollToIndex,
                        onScrollCompleted = onScrollCompleted,
                        onArtworkClick = onArtworkClick,
                        onUserClick = onUserClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        hasMore = state.hasMore,
                        isRefreshing = state.isLoading,
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

/**
 * 推荐作品瀑布流网格
 */
@Composable
private fun ArtworkRecommendGrid(
    artworks: List<Artwork>,
    scrollToIndex: Int = -1,
    onScrollCompleted: () -> Unit = {},
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (String) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    hasMore: Boolean,
    isRefreshing: Boolean,
    onRefresh: () -> Unit
) {
    val listState = rememberLazyStaggeredGridState()
    
    // 监听 scrollToIndex 变化，滚动到指定位置
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex >= 0 && scrollToIndex < artworks.size) {
            // 平滑滚动到目标位置
            listState.animateScrollToItem(scrollToIndex)
            
            // 通知滚动完成
            onScrollCompleted()
        }
    }
    
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && 
                    lastVisibleIndex >= artworks.size - 10 && 
                    hasMore && 
                    !isLoadingMore
                ) {
                    onLoadMore()
                }
            }
    }
    
    val settingsCache: SettingsCache = koinInject()
    val columns by settingsCache.staggeredGridColumns.collectAsState()
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(columns),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = artworks,
                key = { it.id }
            ) { artwork ->
                val index = artworks.indexOf(artwork)
                ArtworkCard(
                    artwork = artwork,
                    onClick = { onArtworkClick(artwork, index) },
                    onUserClick = onUserClick
                )
            }
            
            // 加载更多指示器
            if (isLoadingMore) {
                item(span = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan.FullLine) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}
