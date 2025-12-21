package com.projectu.ui.screens.followlatest

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import org.koin.compose.koinInject
import com.projectu.shared.domain.model.Artwork
import com.projectu.shared.domain.model.Novel
import com.projectu.ui.components.ArtworkCard
import com.projectu.ui.components.NovelCard
import com.projectu.ui.components.SimpleNavigationBar
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.followlatest.more.FollowLatestMoreScreen
import com.projectu.ui.screens.novel.NovelDetailScreen
import com.projectu.ui.screens.novelseries.NovelSeriesScreen
import com.projectu.ui.screens.user.UserScreen
import com.projectu.ui.util.TagClickHandler
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import projectu.composeapp.generated.resources.*

/**
 * 动态页面统一内容区域
 * 参照发现和排行榜设计，支持横向滑动切换内容类型
 * 
 * @param scrollIndices 滚动位置缓存
 * @param initialPageIndex 初始页面索引
 * @param onPageChanged 页面切换回调，用于保存当前页面索引
 * @param onRegisterScrollToTopOrRefreshCallback 注册滚动到顶部或刷新的回调
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FollowLatestContent(
    scrollIndices: MutableMap<String, Int> = mutableMapOf(),
    initialPageIndex: Int = 0,
    onPageChanged: ((Int) -> Unit)? = null,
    onRegisterScrollToTopOrRefreshCallback: ((() -> Unit) -> Unit)? = null
) {
    val parentNavigator = LocalNavigator.current?.parent
    val contentTypes = remember { FollowLatestContentType.getAll() }
    
    // 预先创建所有 ViewModel，避免切换时重新创建
    val illustsViewModel: FollowLatestIllustsViewModel = koinInject()
    val novelsViewModel: FollowLatestNovelsViewModel = koinInject()
    
    // 收集所有状态
    val illustsState by illustsViewModel.state.collectAsState()
    val novelsState by novelsViewModel.state.collectAsState()
    
    // 为每个内容类型创建独立的列表状态缓存
    val listStates = remember {
        mutableStateMapOf<FollowLatestContentType, Any>()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 创建 Pager 状态，使用传入的初始页面索引
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { contentTypes.size }
    )
    
    val currentContentType = contentTypes[pagerState.currentPage]
    
    // 监听页面切换，触发惰性加载并通知外部
    LaunchedEffect(pagerState.currentPage) {
        // 通知外部保存当前页面索引
        onPageChanged?.invoke(pagerState.currentPage)
        
        when (contentTypes[pagerState.currentPage]) {
            FollowLatestContentType.ILLUSTS -> illustsViewModel.initLoadIfNeeded()
            FollowLatestContentType.NOVELS -> novelsViewModel.initLoadIfNeeded()
        }
    }
    
    // 创建刷新回调映射
    val refreshCallbacks = remember {
        mapOf(
            FollowLatestContentType.ILLUSTS to illustsViewModel::refresh,
            FollowLatestContentType.NOVELS to novelsViewModel::refresh
        )
    }
    
    // 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = {
        val listState = listStates[currentContentType]
        val isAtTop = when (listState) {
            is LazyStaggeredGridState -> 
                listState.firstVisibleItemIndex == 0 && 
                listState.firstVisibleItemScrollOffset == 0
            is LazyListState -> 
                listState.firstVisibleItemIndex == 0 && 
                listState.firstVisibleItemScrollOffset == 0
            else -> true
        }
        
        if (isAtTop) {
            // 刷新当前页面
            refreshCallbacks[currentContentType]?.invoke()
        } else {
            coroutineScope.launch {
                when (listState) {
                    is LazyStaggeredGridState -> listState.animateScrollToItem(0)
                    is LazyListState -> listState.animateScrollToItem(0)
                }
            }
        }
    }
    
    // 注册回调
    LaunchedEffect(scrollToTopOrRefresh) {
        onRegisterScrollToTopOrRefreshCallback?.invoke(scrollToTopOrRefresh)
    }
    
    Column(modifier = Modifier.fillMaxSize()) {
        // 第1层导航：内容类型选择器（插画·漫画 / 小说）+ 更多按钮
        SimpleNavigationBar(
            items = contentTypes,
            selectedIndex = pagerState.currentPage,
            onItemClick = { index ->
                if (index == pagerState.currentPage) {
                    scrollToTopOrRefresh()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            },
            getItemLabel = { type -> stringResource(type.displayNameRes) },
            modifier = Modifier.fillMaxWidth(),
            trailingContent = {
                TextButton(
                    onClick = {
                        parentNavigator?.push(FollowLatestMoreScreen())
                    }
                ) {
                    Text(stringResource(Res.string.follow_latest_more))
                }
            }
        )
        
        // HorizontalPager：支持左右滑动切换内容类型
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { contentTypes[it] }
        ) { page ->
            val contentType = contentTypes[page]
            
            when (contentType) {
                FollowLatestContentType.ILLUSTS -> {
                    // 为每个模式保存独立的滚动位置
                    val scrollPositions = remember { mutableStateMapOf<FollowLatestMode, Int>() }
                    val listState = rememberLazyStaggeredGridState(
                        initialFirstVisibleItemIndex = scrollPositions[illustsState.currentMode] ?: 0
                    )
                    remember(contentType) {
                        listStates.getOrPut(contentType) { listState }
                    }
                    
                    val currentMode = illustsState.currentMode
                    val scrollKey = "followlatest_illusts_${currentMode.name}"
                    
                    // 当模式变化时，保存当前滚动位置，然后恢复目标模式的滚动位置
                    var previousMode by remember { mutableStateOf(currentMode) }
                    LaunchedEffect(currentMode) {
                        if (previousMode != currentMode) {
                            // 保存上一个模式的滚动位置
                            scrollPositions[previousMode] = listState.firstVisibleItemIndex
                            
                            // 恢复目标模式的滚动位置
                            val targetIndex = scrollPositions[currentMode] ?: 0
                            listState.scrollToItem(targetIndex)
                            previousMode = currentMode
                        }
                    }
                    
                    // 监听滚动索引变化，滚动到指定位置（从详情页返回时）
                    val targetScrollIndex by remember(contentType, currentMode) {
                        derivedStateOf { scrollIndices[scrollKey] }
                    }
                    
                    LaunchedEffect(targetScrollIndex) {
                        val scrollIndex = targetScrollIndex
                        if (scrollIndex != null && scrollIndex > 0) {
                            listState.animateScrollToItem(scrollIndex)
                            scrollIndices.remove(scrollKey)
                        }
                    }
                    
                    FollowLatestIllustsPage(
                        state = illustsState,
                        onModeChange = illustsViewModel::switchMode,
                        onLoadMore = illustsViewModel::loadMore,
                        onRefresh = illustsViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onArtworkClick = { artwork, index ->
                            val key = "followlatest_illusts_${currentMode.name}"
                            val currentArtworkIds = illustsState.artworks.map { it.id }
                            
                            // 创建绑定到当前模式的列表源
                            val listSource = illustsViewModel.createArtworkListSource(currentMode)
                            
                            // 创建导航上下文
                            val contextKey = NavigationContextManager.createContext(
                                listSource = listSource,
                                onReturnWithIndex = { lastIndex ->
                                    scrollIndices[key] = lastIndex
                                }
                            )
                            
                            parentNavigator?.push(
                                ArtworkDetailScreen(
                                    artworkIds = currentArtworkIds,
                                    initialIndex = index,
                                    contextKey = contextKey
                                )
                            )
                        },
                        onUserClick = { userId ->
                            parentNavigator?.push(UserScreen(userId))
                        },
                        listState = listState as LazyStaggeredGridState
                    )
                }
                
                FollowLatestContentType.NOVELS -> {
                    // 为每个模式保存独立的滚动位置
                    val scrollPositions = remember { mutableStateMapOf<FollowLatestMode, Int>() }
                    val listState = rememberLazyListState(
                        initialFirstVisibleItemIndex = scrollPositions[novelsState.currentMode] ?: 0
                    )
                    remember(contentType) {
                        listStates.getOrPut(contentType) { listState }
                    }
                    
                    val currentMode = novelsState.currentMode
                    
                    // 当模式变化时，保存当前滚动位置，然后恢复目标模式的滚动位置
                    var previousMode by remember { mutableStateOf(currentMode) }
                    LaunchedEffect(currentMode) {
                        if (previousMode != currentMode) {
                            // 保存上一个模式的滚动位置
                            scrollPositions[previousMode] = listState.firstVisibleItemIndex
                            
                            // 恢复目标模式的滚动位置
                            val targetIndex = scrollPositions[currentMode] ?: 0
                            listState.scrollToItem(targetIndex)
                            previousMode = currentMode
                        }
                    }
                    
                    // 创建Tag点击处理器
                    val scope = rememberCoroutineScope()
                    val searchHistoryStore: com.projectu.shared.data.local.SearchHistoryStore = koinInject()
                    val tagClickHandler = remember(parentNavigator) {
                        parentNavigator?.let { nav ->
                            TagClickHandler(nav, searchHistoryStore, scope)
                        }
                    }
                    
                    FollowLatestNovelsPage(
                        state = novelsState,
                        onModeChange = novelsViewModel::switchMode,
                        onLoadMore = novelsViewModel::loadMore,
                        onRefresh = novelsViewModel::refresh,
                        onRefreshOrScrollToTop = scrollToTopOrRefresh,
                        onNovelClick = { novel ->
                            // 跳转到小说详情页
                            parentNavigator?.push(NovelDetailScreen(novelId = novel.id))
                        },
                        onSeriesClick = { seriesId ->
                            parentNavigator?.push(NovelSeriesScreen(seriesId))
                        },
                        onUserClick = { userId ->
                            parentNavigator?.push(UserScreen(userId))
                        },
                        onTagClick = tagClickHandler?.let { handler ->
                            { tag: com.projectu.shared.domain.model.Tag -> handler.handleTagClick(tag) }
                        },
                        listState = listState as LazyListState
                    )
                }
            }
        }
    }
}

/**
 * 关注用户最新插画·漫画页面内容
 */
@Composable
fun FollowLatestIllustsPage(
    state: FollowLatestIllustsState,
    onModeChange: (FollowLatestMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (userId: String) -> Unit,
    listState: LazyStaggeredGridState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 第2层导航：Mode 选择器（全部 / R-18）
        SimpleNavigationBar(
            items = FollowLatestMode.entries,
            selectedIndex = FollowLatestMode.entries.indexOf(state.currentMode),
            onItemClick = { index ->
                val newMode = FollowLatestMode.entries[index]
                if (newMode == state.currentMode) {
                    onRefreshOrScrollToTop()
                } else {
                    onModeChange(newMode)
                }
            },
            getItemLabel = { mode ->
                when (mode) {
                    FollowLatestMode.ALL -> stringResource(Res.string.follow_latest_mode_all)
                    FollowLatestMode.R18 -> stringResource(Res.string.follow_latest_mode_r18)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.artworks.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.artworks.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text(stringResource(Res.string.common_retry))
                        }
                    }
                }
                state.artworks.isNotEmpty() -> {
                    FollowLatestArtworkStaggeredGridLayout(
                        artworks = state.artworks,
                        onArtworkClick = onArtworkClick,
                        onUserClick = onUserClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        listState = listState,
                        isRefreshing = state.isLoading && state.artworks.isNotEmpty(),
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

/**
 * 关注用户最新小说页面内容
 */
@Composable
fun FollowLatestNovelsPage(
    state: FollowLatestNovelsState,
    onModeChange: (FollowLatestMode) -> Unit,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onRefreshOrScrollToTop: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    listState: LazyListState
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // 第2层导航：Mode 选择器（全部 / R-18）
        SimpleNavigationBar(
            items = FollowLatestMode.entries,
            selectedIndex = FollowLatestMode.entries.indexOf(state.currentMode),
            onItemClick = { index ->
                val newMode = FollowLatestMode.entries[index]
                if (newMode == state.currentMode) {
                    onRefreshOrScrollToTop()
                } else {
                    onModeChange(newMode)
                }
            },
            getItemLabel = { mode ->
                when (mode) {
                    FollowLatestMode.ALL -> stringResource(Res.string.follow_latest_mode_all)
                    FollowLatestMode.R18 -> stringResource(Res.string.follow_latest_mode_r18)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                state.isLoading && state.novels.isEmpty() -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.error != null && state.novels.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = state.error,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = onRefresh) {
                            Text(stringResource(Res.string.common_retry))
                        }
                    }
                }
                state.novels.isNotEmpty() -> {
                    FollowLatestNovelListLayout(
                        novels = state.novels,
                        onNovelClick = onNovelClick,
                        onSeriesClick = onSeriesClick,
                        onUserClick = onUserClick,
                        onTagClick = onTagClick,
                        onLoadMore = onLoadMore,
                        isLoadingMore = state.isLoadingMore,
                        listState = listState,
                        isRefreshing = state.isLoading && state.novels.isNotEmpty(),
                        onRefresh = onRefresh
                    )
                }
            }
        }
    }
}

/**
 * 作品瀑布流布局（带下拉刷新）
 */
@Composable
private fun FollowLatestArtworkStaggeredGridLayout(
    artworks: List<Artwork>,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: LazyStaggeredGridState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index }
            .collect { lastVisibleIndex ->
                if (lastVisibleIndex != null && lastVisibleIndex >= artworks.size - 10) {
                    onLoadMore()
                }
            }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(3),
            state = listState,
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalItemSpacing = 8.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            items(artworks, key = { it.id }) { artwork ->
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

/**
 * 小说列表布局（带下拉刷新）
 */
@Composable
private fun FollowLatestNovelListLayout(
    novels: List<Novel>,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    onLoadMore: () -> Unit,
    isLoadingMore: Boolean,
    listState: LazyListState,
    isRefreshing: Boolean = false,
    onRefresh: () -> Unit = {}
) {
    // 监听滚动，触发加载更多
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()
            lastVisibleItem?.index?.let { it >= totalItems - 5 } ?: false
        }
        .distinctUntilChanged()
        .collect { shouldLoadMore ->
            if (shouldLoadMore && !isLoadingMore) {
                onLoadMore()
            }
        }
    }
    
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(novels, key = { it.id }) { novel ->
                NovelCard(
                    novel = novel,
                    onClick = { onNovelClick(novel) },
                    onSeriesClick = onSeriesClick,
                    onUserClick = onUserClick,
                    onTagClick = onTagClick
                )
            }
            
            // 加载更多指示器
            if (isLoadingMore) {
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
        }
    }
}
