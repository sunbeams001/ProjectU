package com.projectu.ui.screens.novel

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import com.projectu.shared.data.remote.api.PixivApi
import com.projectu.shared.data.remote.mapper.toNovelList
import com.projectu.shared.domain.model.Novel
import com.projectu.shared.domain.usecase.SyncNovelStatesUseCase
import com.projectu.shared.util.AgeLimitDeterminer
import com.projectu.shared.util.TagTranslationUtil
import com.projectu.ui.components.ErrorDisplay
import com.projectu.ui.components.NovelCard
import com.projectu.ui.navigation.NovelListSource
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.util.TagClickHandler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import projectu.composeapp.generated.resources.Res
import projectu.composeapp.generated.resources.novel_recommend_title
import projectu.composeapp.generated.resources.novel_recommend_empty
import projectu.composeapp.generated.resources.common_loading
import projectu.composeapp.generated.resources.common_retry
import projectu.composeapp.generated.resources.nav_back
import projectu.composeapp.generated.resources.list_no_more_items

/**
 * 推荐小说页面
 * 
 * 功能：
 * - 展示基于某个小说的推荐小说
 * - 使用 getRecommendInit 获取初始数据
 * - 使用 getRecommendNovels 进行分页加载
 * - 支持下拉刷新
 * - 列表展示
 * 
 * @param novelId 基准小说ID
 */
data class NovelRecommendScreen(
    private val novelId: String
) : Screen {
    
    /**
     * 自定义 Screen key，确保不同小说的推荐页面有独立的 ViewModel 实例
     * 每个基准小说ID对应一个独立的推荐列表状态
     */
    override val key: cafe.adriel.voyager.core.screen.ScreenKey
        get() = "NovelRecommendScreen_$novelId"
    
    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        
        // 获取依赖
        val pixivApi: PixivApi = koinInject()
        val tagTranslationUtil: TagTranslationUtil = koinInject()
        val ageLimitDeterminer: AgeLimitDeterminer = koinInject()
        val syncNovelStatesUseCase: SyncNovelStatesUseCase = koinInject()
        val stateCacheManager: StateCacheManager = koinInject()
        
        val viewModel = rememberScreenModel {
            NovelRecommendViewModel(
                novelId = novelId,
                pixivApi = pixivApi,
                tagTranslationUtil = tagTranslationUtil,
                ageLimitDeterminer = ageLimitDeterminer,
                syncNovelStatesUseCase = syncNovelStatesUseCase,
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
        
        NovelRecommendContent(
            state = state,
            scrollToIndex = scrollToIndex,
            onScrollCompleted = { viewModel.resetScrollToIndex() },
            onBackClick = { navigator.pop() },
            onRefresh = { viewModel.refresh() },
            onLoadMore = { viewModel.loadMore() },
            onNovelClick = { novel ->
                // 保存滚动位置
                viewModel.setScrollToIndex(state.novels.indexOf(novel))
                // 跳转到小说详情页
                navigator.push(NovelDetailScreen(novelId = novel.id))
            },
            onSeriesClick = { seriesId ->
                navigator.push(NovelSeriesScreen(seriesId))
            },
            onUserClick = { userId ->
                navigator.push(UserScreen(userId))
            },
            onTagClick = { tag -> tagClickHandler.handleTagClick(tag) }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovelRecommendContent(
    state: NovelRecommendState,
    scrollToIndex: Int,
    onScrollCompleted: () -> Unit,
    onBackClick: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: (String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null
) {
    val listState = rememberLazyListState()
    
    // 处理滚动到指定位置
    LaunchedEffect(scrollToIndex) {
        if (scrollToIndex >= 0 && scrollToIndex < state.novels.size) {
            listState.animateScrollToItem(scrollToIndex)
            onScrollCompleted()
        }
    }
    
    // 监听滚动位置，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem >= totalItems - 3
        }
            .distinctUntilChanged()
            .filter { it && !state.isLoadingMore && state.hasMore }
            .collect {
                onLoadMore()
            }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(Res.string.novel_recommend_title)) },
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
                state.isLoading -> {
                    // 加载中
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.isEmpty -> {
                    // 空状态
                    Text(
                        text = stringResource(Res.string.novel_recommend_empty),
                        modifier = Modifier.align(Alignment.Center),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                state.error != null && state.novels.isEmpty() -> {
                    // 错误状态
                    ErrorDisplay(
                        message = state.error,
                        onRetry = onRefresh,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    // 小说列表
                    PullToRefreshBox(
                        isRefreshing = state.isRefreshing,
                        onRefresh = onRefresh,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            items(
                                items = state.novels,
                                key = { it.id }
                            ) { novel ->
                                NovelCard(
                                    novel = novel,
                                    onClick = { onNovelClick(novel) },
                                    onUserClick = { onUserClick(novel.userId) },
                                    onSeriesClick = novel.seriesId?.let { { onSeriesClick(it) } },
                                    onTagClick = onTagClick,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                            
                            // 加载更多指示器
                            if (state.isLoadingMore) {
                                item {
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
                            
                            // 没有更多数据提示
                            if (!state.hasMore && state.novels.isNotEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = stringResource(Res.string.list_no_more_items),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 小说推荐页面状态
 */
data class NovelRecommendState(
    val novels: List<Novel> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val hasMore: Boolean = true,
    val isEmpty: Boolean = false
)

/**
 * 小说推荐 ViewModel
 */
class NovelRecommendViewModel(
    private val novelId: String,
    private val pixivApi: PixivApi,
    private val tagTranslationUtil: TagTranslationUtil,
    private val ageLimitDeterminer: AgeLimitDeterminer,
    private val syncNovelStatesUseCase: SyncNovelStatesUseCase,
    private val stateCacheManager: StateCacheManager
) : ScreenModel {
    
    private val _state = MutableStateFlow(NovelRecommendState())
    val state: StateFlow<NovelRecommendState> = _state.asStateFlow()
    
    private val _scrollToIndex = MutableStateFlow(-1)
    val scrollToIndex: StateFlow<Int> = _scrollToIndex.asStateFlow()
    
    // 存储 nextIds 用于分页
    private val nextIds = mutableListOf<String>()
    private var currentIdIndex = 0
    private val pageSize = 9 // 每次加载9个
    
    init {
        loadInitialRecommendations()
        
        // 监听小说状态变化
        screenModelScope.launch {
            stateCacheManager.stateChangeEvents
                .filter { event ->
                    when (event) {
                        is StateCacheEvent.NovelBookmarkChanged -> true
                        else -> false
                    }
                }
                .collect {
                    // 重新同步状态
                    val syncedNovels = syncNovelStatesUseCase(_state.value.novels)
                    _state.update { current -> current.copy(novels = syncedNovels) }
                }
        }
    }
    
    /**
     * 刷新
     */
    fun refresh() {
        if (_state.value.isRefreshing || _state.value.isLoading) {
            return
        }
        
        screenModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            
            try {
                // 重新加载初始数据
                val response = pixivApi.novelApi.getRecommendInit(
                    novelId = novelId.toLong(),
                    limit = pageSize
                )
                
                if (response.error) {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = response.message
                        )
                    }
                    return@launch
                }
                
                val body = response.body
                if (body == null) {
                    _state.update {
                        it.copy(
                            isRefreshing = false,
                            error = "Empty response"
                        )
                    }
                    return@launch
                }
                
                // 转换小说列表
                val novels = body.toNovelList(
                    tagTranslationUtil = tagTranslationUtil,
                    ageLimitDeterminer = ageLimitDeterminer
                )
                
                // 同步小说状态
                val syncedNovels = syncNovelStatesUseCase(novels)
                
                // 保存 nextIds
                nextIds.clear()
                nextIds.addAll(body.nextIds)
                currentIdIndex = 0
                
                val isEmpty = novels.isEmpty() && body.nextIds.isEmpty()
                
                _state.update {
                    it.copy(
                        novels = syncedNovels,
                        isRefreshing = false,
                        error = null,
                        hasMore = body.nextIds.isNotEmpty(),
                        isEmpty = isEmpty
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isRefreshing = false,
                        error = e.message ?: "Refresh failed"
                    )
                }
            }
        }
    }
    
    /**
     * NovelListSource 实现
     */
    val novelListSource = object : NovelListSource {
        override val novelIdsFlow: StateFlow<List<String>> = state.map { it.novels.map { novel -> novel.id } }
            .stateIn(
                scope = screenModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = state.value.novels.map { it.id }
            )
        
        override fun loadMoreNovels() {
            loadMore()
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
                val response = pixivApi.novelApi.getRecommendInit(
                    novelId = novelId.toLong(),
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
                            error = "Empty response",
                            isEmpty = true
                        )
                    }
                    return@launch
                }
                
                // 转换小说列表
                val novels = body.toNovelList(
                    tagTranslationUtil = tagTranslationUtil,
                    ageLimitDeterminer = ageLimitDeterminer
                )
                
                // 同步小说状态
                val syncedNovels = syncNovelStatesUseCase(novels)
                
                // 保存 nextIds 用于后续分页
                nextIds.clear()
                nextIds.addAll(body.nextIds)
                currentIdIndex = 0
                
                // 判断是否为空
                val isEmpty = novels.isEmpty() && body.nextIds.isEmpty()
                
                _state.update {
                    it.copy(
                        novels = syncedNovels,
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
                        error = e.message ?: "Load failed",
                        isEmpty = true
                    )
                }
            }
        }
    }
    
    /**
     * 加载更多推荐小说
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
                
                // 调用 getRecommendNovels
                val response = pixivApi.novelApi.getRecommendNovels(novelIds = idsToLoad)
                
                if (response.error) {
                    _state.update { it.copy(isLoadingMore = false, error = response.message) }
                    return@launch
                }
                
                val body = response.body
                if (body == null) {
                    _state.update { it.copy(isLoadingMore = false) }
                    return@launch
                }
                
                // 转换小说列表
                val newNovels = body.toNovelList(
                    tagTranslationUtil = tagTranslationUtil,
                    ageLimitDeterminer = ageLimitDeterminer
                )
                
                // 同步小说状态
                val syncedNewNovels = syncNovelStatesUseCase(newNovels)
                
                // 更新索引
                currentIdIndex = endIndex
                
                // 判断是否还有更多
                val hasMore = currentIdIndex < nextIds.size
                
                _state.update {
                    it.copy(
                        novels = it.novels + syncedNewNovels,
                        isLoadingMore = false,
                        error = null,
                        hasMore = hasMore
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoadingMore = false,
                        error = e.message ?: "Load more failed"
                    )
                }
            }
        }
    }
}
