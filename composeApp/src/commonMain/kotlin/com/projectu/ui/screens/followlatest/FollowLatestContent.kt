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
import com.projectu.ui.components.TabbedNavigationBar
import com.projectu.ui.components.PageMapping
import com.projectu.ui.components.CustomTwoLayerMapper
import com.projectu.ui.components.rememberPagedNavigationState
import com.projectu.ui.navigation.NavigationContextManager
import com.projectu.ui.screens.artwork.ArtworkDetailScreen
import com.projectu.ui.screens.followlatest.WatchListMangaViewModel
import com.projectu.ui.screens.followlatest.WatchListNovelsViewModel
import com.projectu.ui.screens.mangaseries.MangaSeriesScreen
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
    val watchListMangaViewModel: WatchListMangaViewModel = koinInject()
    val watchListNovelsViewModel: WatchListNovelsViewModel = koinInject()
    
    // 收集所有状态
    val illustsState by illustsViewModel.state.collectAsState()
    val novelsState by novelsViewModel.state.collectAsState()
    val watchListMangaState by watchListMangaViewModel.state.collectAsState()
    val watchListNovelsState by watchListNovelsViewModel.state.collectAsState()
    
    // 为每个页面创建独立的列表状态缓存
    val listStates = remember {
        mutableStateMapOf<String, Any>()
    }
    
    val coroutineScope = rememberCoroutineScope()
    
    // 定义动态页面的映射信息
    data class FollowLatestPageMapping(
        override val primaryIndex: Int,
        override val secondaryIndex: Int,
        override val showSecondaryNav: Boolean,
        val contentType: FollowLatestContentType,
        val mode: FollowLatestMode
    ) : PageMapping
    
    // 使用自定义双层导航映射器
    // 结构：插画×2(0-1) → 小说×2(2-3) → 追更列表×2(4-5) → 好P友×1(6)
    val modes = remember { FollowLatestMode.entries }
    val mapper = remember {
        CustomTwoLayerMapper(
            secondaryCountPerPrimary = listOf(2, 2, 2, 1), // 插画2个、小说2个、追更列表2个、好P友1个
            createMapping = { primaryIndex, secondaryIndex, showSecondary ->
                val contentType = contentTypes[primaryIndex]
                val mode = when (contentType) {
                    FollowLatestContentType.ILLUSTS, FollowLatestContentType.NOVELS -> {
                        if (showSecondary) modes[secondaryIndex] else modes[0]
                    }
                    else -> modes[0] // 追更列表和好P友不使用mode
                }
                FollowLatestPageMapping(primaryIndex, secondaryIndex, showSecondary, contentType, mode)
            }
        )
    }
    
    // 创建 Pager 状态
    val pagerState = rememberPagerState(
        initialPage = initialPageIndex.coerceIn(0, mapper.totalPages - 1),
        pageCount = { mapper.totalPages }
    )
    
    // 创建页码导航状态
    val navState = rememberPagedNavigationState(pagerState, mapper)
    
    // 监听页面切换，触发惰性加载并通知外部
    LaunchedEffect(pagerState.currentPage) {
        // 通知外部保存当前页面索引
        onPageChanged?.invoke(pagerState.currentPage)
        
        val mapping = mapper.parsePageIndex(pagerState.currentPage)
        when (mapping.contentType) {
            FollowLatestContentType.ILLUSTS -> {
                illustsViewModel.initLoadIfNeeded()
                // 同步模式状态
                if (mapping.mode != illustsState.currentMode) {
                    illustsViewModel.switchMode(mapping.mode)
                }
            }
            FollowLatestContentType.NOVELS -> {
                novelsViewModel.initLoadIfNeeded()
                // 同步模式状态
                if (mapping.mode != novelsState.currentMode) {
                    novelsViewModel.switchMode(mapping.mode)
                }
            }
            FollowLatestContentType.WATCH_LIST -> {
                // 根据secondaryIndex判断是漫画还是小说
                if (mapping.secondaryIndex == 0) {
                    watchListMangaViewModel.initLoadIfNeeded()
                } else {
                    watchListNovelsViewModel.initLoadIfNeeded()
                }
            }
            FollowLatestContentType.GOOD_P_FRIENDS -> {
                // 好P友页面占位，暂无数据加载
            }
        }
    }
    
    // 创建刷新回调
    val refreshCurrentPage: () -> Unit = {
        val mapping = mapper.parsePageIndex(pagerState.currentPage)
        when (mapping.contentType) {
            FollowLatestContentType.ILLUSTS -> illustsViewModel.refresh()
            FollowLatestContentType.NOVELS -> novelsViewModel.refresh()
            FollowLatestContentType.WATCH_LIST -> {
                // 根据secondaryIndex判断是漫画还是小说
                if (mapping.secondaryIndex == 0) {
                    watchListMangaViewModel.refresh()
                } else {
                    watchListNovelsViewModel.refresh()
                }
            }
            FollowLatestContentType.GOOD_P_FRIENDS -> {} // 无需刷新
        }
    }
    
    // 创建滚动到顶部或刷新的回调
    val scrollToTopOrRefresh: () -> Unit = {
        val pageKey = "page_${pagerState.currentPage}"
        val listState = listStates[pageKey]
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
            refreshCurrentPage()
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
        // 双层导航：使用 TabbedNavigationBar（Tab + FilterChip）
        val currentMapping = navState.currentMapping
        
        // 根据当前一级导航选择对应的二级导航项
        val watchListTypes = remember { WatchListContentType.getAll() }
        val secondaryItems = when (currentMapping.contentType) {
            FollowLatestContentType.ILLUSTS, FollowLatestContentType.NOVELS -> modes
            FollowLatestContentType.WATCH_LIST -> watchListTypes
            else -> emptyList()
        }
        
        TabbedNavigationBar(
            primaryItems = contentTypes,
            primarySelectedIndex = currentMapping.primaryIndex,
            onPrimaryItemClick = { index ->
                navState.handlePrimaryClick(
                    primaryIndex = index,
                    currentSecondaryIndex = currentMapping.secondaryIndex,
                    scope = coroutineScope,
                    onSamePage = scrollToTopOrRefresh
                )
            },
            getPrimaryItemLabel = { type -> stringResource(type.displayNameRes) },
            secondaryItems = secondaryItems,
            secondarySelectedIndex = currentMapping.secondaryIndex,
            onSecondaryItemClick = { secondaryIndex ->
                navState.handleSecondaryClick(
                    secondaryIndex = secondaryIndex,
                    currentPrimaryIndex = currentMapping.primaryIndex,
                    scope = coroutineScope,
                    onSamePage = scrollToTopOrRefresh
                )
            },
            getSecondaryItemLabel = { item ->
                when (item) {
                    is FollowLatestMode -> {
                        when (item) {
                            FollowLatestMode.ALL -> stringResource(Res.string.follow_latest_mode_all)
                            FollowLatestMode.R18 -> stringResource(Res.string.follow_latest_mode_r18)
                        }
                    }
                    is WatchListContentType -> {
                        stringResource(item.displayNameRes)
                    }
                    else -> ""
                }
            },
            showSecondaryNav = currentMapping.showSecondaryNav,
            modifier = Modifier.fillMaxWidth()
        )
        
        // HorizontalPager：支持左右滑动切换所有页面
        // 滑动顺序：插画-公开(0) → 插画-R18(1) → 小说-公开(2) → 小说-R18(3)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { it }
        ) { page ->
            val mapping = mapper.parsePageIndex(page)
            val pageKey = "page_$page"
            
            when (mapping.contentType) {
                FollowLatestContentType.ILLUSTS -> {
                    val listState = rememberLazyStaggeredGridState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
                    }
                    
                    val currentMode = mapping.mode
                    val scrollKey = "followlatest_illusts_${currentMode.name}"
                    
                    // 监听滚动索引变化，滚动到指定位置（从详情页返回时）
                    val targetScrollIndex by remember(pageKey) {
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
                        onLoadMore = illustsViewModel::loadMore,
                        onRefresh = illustsViewModel::refresh,
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
                        listState = listState
                    )
                }
                
                FollowLatestContentType.NOVELS -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
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
                        onLoadMore = novelsViewModel::loadMore,
                        onRefresh = novelsViewModel::refresh,
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
                        listState = listState
                    )
                }
                
                FollowLatestContentType.WATCH_LIST -> {
                    val listState = rememberLazyListState()
                    LaunchedEffect(pageKey) {
                        listStates[pageKey] = listState
                    }
                    
                    // 创建Tag点击处理器
                    val scope = rememberCoroutineScope()
                    val searchHistoryStore: com.projectu.shared.data.local.SearchHistoryStore = koinInject()
                    val tagClickHandler = remember(parentNavigator) {
                        parentNavigator?.let { nav ->
                            TagClickHandler(nav, searchHistoryStore, scope)
                        }
                    }
                    
                    // 根据secondaryIndex判断是漫画还是小说
                    when (mapping.secondaryIndex) {
                        0 -> {
                            // 漫画追更列表
                            WatchListMangaPage(
                                state = watchListMangaState,
                                onLoadMore = watchListMangaViewModel::loadMore,
                                onRefresh = watchListMangaViewModel::refresh,
                                onSeriesClick = { series ->
                                    parentNavigator?.push(MangaSeriesScreen(series.id))
                                }
                            )
                        }
                        1 -> {
                            // 小说追更列表
                            WatchListNovelsPage(
                                state = watchListNovelsState,
                                onLoadMore = watchListNovelsViewModel::loadMore,
                                onRefresh = watchListNovelsViewModel::refresh,
                                onSeriesClick = { series ->
                                    parentNavigator?.push(NovelSeriesScreen(series.id))
                                },
                                onTagClick = if (tagClickHandler != null) tagClickHandler::handleTagClick else null
                            )
                        }
                    }
                }
                
                FollowLatestContentType.GOOD_P_FRIENDS -> {
                    // 好P友页面占位
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = stringResource(Res.string.follow_latest_good_p_friends),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = stringResource(Res.string.follow_latest_coming_soon),
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

/**
 * 关注用户最新插画·漫画页面内容
 */
@Composable
fun FollowLatestIllustsPage(
    state: FollowLatestIllustsState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onArtworkClick: (Artwork, Int) -> Unit,
    onUserClick: (userId: String) -> Unit,
    listState: LazyStaggeredGridState
) {
    // 移除了第二层导航栏，现在直接显示内容
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

/**
 * 关注用户最新小说页面内容
 */
@Composable
fun FollowLatestNovelsPage(
    state: FollowLatestNovelsState,
    onLoadMore: () -> Unit,
    onRefresh: () -> Unit,
    onNovelClick: (Novel) -> Unit,
    onSeriesClick: (String) -> Unit,
    onUserClick: (userId: String) -> Unit,
    onTagClick: ((com.projectu.shared.domain.model.Tag) -> Unit)? = null,
    listState: LazyListState
) {
    // 移除了第二层导航栏，现在直接显示内容
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
    
    val settingsCache: com.projectu.shared.data.local.SettingsCache = koinInject()
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
